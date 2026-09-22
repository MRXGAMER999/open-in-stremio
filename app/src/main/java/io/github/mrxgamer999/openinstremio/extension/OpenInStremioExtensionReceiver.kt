package io.github.mrxgamer999.openinstremio.extension

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.IntentCompat
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.api.constants.IncomingConstants
import com.battlelancer.seriesguide.api.constants.OutgoingConstants
import io.github.mrxgamer999.openinstremio.data.AppGraph
import io.github.mrxgamer999.openinstremio.data.ExtensionStateStore
import io.github.mrxgamer999.openinstremio.data.ImdbResolver
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import io.github.mrxgamer999.openinstremio.forwarder.Target
import io.github.mrxgamer999.openinstremio.forwarder.targets
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Discovery point for SeriesGuide, and the place every action is published.
 *
 * This used to hand its work to a [com.battlelancer.seriesguide.api.SeriesGuideExtension] service,
 * which is a JobIntentService: on API 26+ that means JobScheduler, which may delay the job by
 * minutes when the app sits in a restricted App Standby bucket (this one has no launcher icon on
 * TV, so it usually does). SeriesGuide meanwhile keeps only the last five requested titles and
 * drops any action that arrives after a title fell out of that cache — which is why the button
 * used to appear only sometimes, and why restarting SeriesGuide "fixed" it.
 *
 * So the action is published from the broadcast itself, which is delivered immediately, and
 * [goAsync] buys enough time to read the local IMDb-id cache off the main thread. A title whose id
 * needs a TMDb lookup gets the "Search in Stremio" button first and the direct link afterwards,
 * from [ImdbLookups] — started only once the broadcast has been finished, because a receiver that
 * holds its [PendingResult] holds up every broadcast queued behind it.
 */
class OpenInStremioExtensionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pendingResult = goAsync()
        orderedScope.launch {
            // Requests are answered in arrival order, which matters: a subscribe carries the token
            // the update right behind it has to be published with. The single-threaded dispatcher
            // orders the starts; the mutex keeps a coroutine suspended on DataStore from letting
            // the next one overtake it.
            orderedMutex.withLock {
                val answered =
                    try {
                        when (intent.action) {
                            IncomingConstants.ACTION_SUBSCRIBE -> handleSubscribe(app, intent)
                            IncomingConstants.ACTION_UPDATE -> handleUpdate(app, intent)
                            else -> Answered()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to answer SeriesGuide", e)
                        Answered()
                    } finally {
                        // SeriesGuide has its action; everything below is bookkeeping and must not
                        // be charged to the broadcast. Inside the try, so a cancellation on the way
                        // out cannot skip it.
                        pendingResult.finish()
                    }

                // Still in the ordered lane: a detached setActive(true) from an update could
                // otherwise land after an unsubscribe's setActive(false) and resurrect the card.
                answered.active?.let { setActive(app, it) }
                answered.upgrade?.let { upgrade ->
                    ImdbLookups.launchUpgrade { publishUpgrade(app, upgrade) }
                }
            }
        }
    }

    private fun handleSubscribe(context: Context, intent: Intent): Answered {
        val subscriber =
            IntentCompat.getParcelableExtra(
                intent,
                IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT,
                ComponentName::class.java,
            )
        if (subscriber == null) {
            Log.w(TAG, "subscribe without a subscriber component")
            return Answered()
        }
        val token = intent.getStringExtra(IncomingConstants.EXTRA_TOKEN)
        val active = ExtensionSubscriptions(context).setSubscription(subscriber, token)
        Log.i(TAG, "${if (token.isNullOrEmpty()) "unsubscribed" else "subscribed"} $subscriber")
        return Answered(active = active)
    }

    private suspend fun handleUpdate(context: Context, intent: Intent): Answered {
        val identifier = intent.getIntExtra(IncomingConstants.EXTRA_ENTITY_IDENTIFIER, 0)
        if (identifier <= 0) return Answered()
        val subscriptions = ExtensionSubscriptions(context)
        val chosen = chosenTargets(context, CHOICE_FAST_TIMEOUT_MS)

        val episode = intent.getBundleExtra(IncomingConstants.EXTRA_EPISODE)?.let(Episode::fromBundle)
        val movie = intent.getBundleExtra(IncomingConstants.EXTRA_MOVIE)?.let(Movie::fromBundle)
        val upgrade =
            when {
                episode != null -> publishEpisode(context, subscriptions, chosen, identifier, episode)
                movie != null -> publishMovie(context, subscriptions, chosen, identifier, movie)
                else -> null
            }

        // Receiving a request proves the extension is enabled; cheap self-heal for the status card.
        return Answered(active = true, upgrade = upgrade)
    }

    /** Returns the lookup that would upgrade what was just published, if one is worth running. */
    private suspend fun publishEpisode(
        context: Context,
        subscriptions: ExtensionSubscriptions,
        chosen: List<Target>,
        identifier: Int,
        episode: Episode,
    ): Upgrade? {
        val title = episode.showTitle?.takeUnless { it.isBlank() } ?: episode.title.orEmpty()
        val season = episode.season
        val number = episode.number
        val year = firstReleaseYear(episode.showFirstReleaseDate)
        // Without a season/episode number a direct link is impossible, so neither the cache nor a
        // lookup buys anything.
        if (season == null || number == null) {
            publishSearch(
                context,
                subscriptions,
                chosen,
                identifier,
                title,
                OutgoingConstants.ACTION_TYPE_EPISODE,
                year = year,
            )
            return null
        }

        val imdbId =
            episode.showImdbId?.takeUnless { it.isBlank() }
                ?: cached(context) { it.cachedShow(episode.showTmdbId) }
        if (imdbId == null) {
            // The numbers ride along even though a search cannot use them: Fireguy matches on
            // the name, so with them it still answers with the episode rather than the show.
            publishSearch(
                context,
                subscriptions,
                chosen,
                identifier,
                title,
                OutgoingConstants.ACTION_TYPE_EPISODE,
                season,
                number,
                year,
            )
            // A lookup needs something to look up; without an id the fallback is the final answer.
            return episode.showTmdbId?.takeIf { it > 0 }?.let {
                Upgrade.Episode(identifier, title, it, season, number, year)
            }
        }

        subscriptions.publish(
            LaunchActions.openEpisode(context, chosen, identifier, imdbId, title, season, number, year),
            OutgoingConstants.ACTION_TYPE_EPISODE,
        )
        return null
    }

    /** Returns the lookup that would upgrade what was just published, if one is worth running. */
    private suspend fun publishMovie(
        context: Context,
        subscriptions: ExtensionSubscriptions,
        chosen: List<Target>,
        identifier: Int,
        movie: Movie,
    ): Upgrade? {
        val title = movie.title.orEmpty()
        val year = movie.releaseDate?.let(::releaseYear)
        val imdbId =
            movie.imdbId?.takeUnless { it.isBlank() }
                ?: cached(context) { it.cachedMovie(movie.tmdbId) }
        if (imdbId == null) {
            publishSearch(
                context,
                subscriptions,
                chosen,
                identifier,
                title,
                OutgoingConstants.ACTION_TYPE_MOVIE,
                year = year,
            )
            return movie.tmdbId?.takeIf { it > 0 }?.let { Upgrade.Movie(identifier, title, it, year) }
        }

        subscriptions.publish(
            LaunchActions.openMovie(context, chosen, identifier, imdbId, title, year),
            OutgoingConstants.ACTION_TYPE_MOVIE,
        )
        return null
    }

    private fun publishSearch(
        context: Context,
        subscriptions: ExtensionSubscriptions,
        chosen: List<Target>,
        identifier: Int,
        title: String,
        actionType: Int,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
    ) = subscriptions.publish(
        LaunchActions.search(context, chosen, identifier, title, season, episode, year),
        actionType,
    )

    /**
     * Replaces the search fallback with a direct link, once TMDb has answered. Runs detached, so
     * SeriesGuide may well have moved on — publishing to a title it no longer shows is harmless.
     */
    private suspend fun publishUpgrade(context: Context, request: Upgrade) {
        // Detached, so there is time to wait for the choice properly.
        val chosen = chosenTargets(context, CHOICE_TIMEOUT_MS)
        val action =
            when (request) {
                is Upgrade.Episode -> {
                    val imdbId = ImdbLookups.resolveShow(context, request.tmdbId) ?: return
                    LaunchActions.openEpisode(
                        context,
                        chosen,
                        request.identifier,
                        imdbId,
                        request.title,
                        request.season,
                        request.number,
                        request.year,
                    )
                }
                is Upgrade.Movie -> {
                    val imdbId = ImdbLookups.resolveMovie(context, request.tmdbId) ?: return
                    LaunchActions.openMovie(context, chosen, request.identifier, imdbId, request.title, request.year)
                }
            }
        ExtensionSubscriptions(context).publish(action, request.actionType)
        Log.i(TAG, "${request.identifier}: upgraded to open (id resolved)")
    }

    /**
     * Local-cache lookup, capped: a cold DataStore has to open its file, and a broadcast receiver
     * that overruns its window is an ANR. Giving up just means publishing the search fallback and
     * letting the detached lookup upgrade it — which is why the cap is here and not around the
     * whole fast path, where it could leave the user with no button at all.
     */
    private suspend fun cached(context: Context, lookup: suspend (ImdbResolver) -> String?): String? =
        try {
            withTimeoutOrNull(CACHE_TIMEOUT_MS) { lookup(AppGraph.imdbResolver(context)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "IMDb-id cache lookup failed", e)
            null
        }

    /**
     * The players the user chose, for the button's label only: the forwarder reads the choice
     * again when the button is tapped. So giving up here costs at most a label that reads as [PlayerChoice.BOTH]
     * until SeriesGuide asks again, never a wrong destination, and it keeps the button fast.
     */
    private suspend fun chosenTargets(context: Context, timeoutMs: Long): List<Target> {
        val choice =
            try {
                withTimeoutOrNull(timeoutMs) { AppGraph.playerChoiceStore(context).choice.first() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Player choice read failed", e)
                null
            }
        return (choice ?: PlayerChoice.BOTH).targets
    }

    /** Best-effort: a failed flag write must never affect the published action. */
    private suspend fun setActive(context: Context, active: Boolean) {
        try {
            // Capped as well: this runs holding the ordered lane, so a stuck write would stall
            // every request behind it.
            withTimeoutOrNull(ACTIVE_WRITE_TIMEOUT_MS) {
                ExtensionStateStore(AppGraph.dataStore(context)).setActive(active)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist extension-active flag", e)
        }
    }

    /**
     * What is left once SeriesGuide has its answer: the status-card flag to record, and — when only
     * a TMDb lookup could still improve the button — the request to upgrade it.
     */
    private class Answered(val active: Boolean? = null, val upgrade: Upgrade? = null)

    /**
     * Everything a detached lookup needs, read out of the Intent while the broadcast is still
     * alive: once `finish()` has run, the bundle is no business of this receiver's. The year is
     * read here for the same reason — the upgraded button must carry what the original one did.
     */
    private sealed interface Upgrade {
        val identifier: Int
        val title: String
        val tmdbId: Int
        val year: Int?
        val actionType: Int

        data class Episode(
            override val identifier: Int,
            override val title: String,
            override val tmdbId: Int,
            val season: Int,
            val number: Int,
            override val year: Int? = null,
        ) : Upgrade {
            override val actionType = OutgoingConstants.ACTION_TYPE_EPISODE
        }

        data class Movie(
            override val identifier: Int,
            override val title: String,
            override val tmdbId: Int,
            override val year: Int? = null,
        ) : Upgrade {
            override val actionType = OutgoingConstants.ACTION_TYPE_MOVIE
        }
    }

    /** UTC on purpose: a date-only release read at a local midnight could otherwise shift a year at the year's edge. */
    private fun releaseYear(date: Date): Int =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }.get(Calendar.YEAR)

    /** The show's first release is an ISO instant ("2022-07-14T04:00:00Z") or empty; the year leads. */
    private fun firstReleaseYear(firstRelease: String?): Int? =
        LEADING_YEAR.find(firstRelease.orEmpty())?.value?.toIntOrNull()

    private companion object {
        private const val TAG = "OpenInStremioExt"

        private val LEADING_YEAR = Regex("^\\d{4}")

        private const val CACHE_TIMEOUT_MS = 2_000L
        private const val CHOICE_TIMEOUT_MS = 2_000L

        /**
         * Tight because it runs before the action exists, and SeriesGuide only waits for so long.
         * The DataStore is opened once per process, so this only matters on a cold start.
         */
        private const val CHOICE_FAST_TIMEOUT_MS = 300L
        private const val ACTIVE_WRITE_TIMEOUT_MS = 2_000L

        /** Idle workers are released: this process is usually only alive for the broadcast. */
        private val worker =
            ThreadPoolExecutor(1, 1, 30L, TimeUnit.SECONDS, LinkedBlockingQueue()).apply {
                allowCoreThreadTimeOut(true)
            }

        /**
         * One thread, so coroutines are *started* in the order the broadcasts arrived. The handler
         * covers the one thing outside the try/catch below that can still throw: `finish()` on a
         * result the framework has already finished for us. Nothing here is worth a crash.
         */
        private val orderedScope =
            CoroutineScope(
                SupervisorJob() +
                    worker.asCoroutineDispatcher() +
                    CoroutineExceptionHandler { _, e -> Log.e(TAG, "Extension broadcast failed", e) }
            )

        /** Fair, so they also *finish* in that order across suspension points. */
        private val orderedMutex = Mutex()
    }
}

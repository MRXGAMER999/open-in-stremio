package io.github.mrxgamer999.openinstremio.extension

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.api.constants.IncomingConstants
import io.github.mrxgamer999.openinstremio.data.AppGraph
import io.github.mrxgamer999.openinstremio.data.PlayerChoice
import io.github.mrxgamer999.openinstremio.data.PlayerChoiceStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Drives the extension the way SeriesGuide does: pairs [DebugSubscriberReceiver] with it as a real
 * subscriber, sends subscribe/update broadcasts, and waits for what comes back.
 */
internal class ExtensionHarness(
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
) {

    private val extension = ComponentName(context, OpenInStremioExtensionReceiver::class.java.name)
    private val subscriber = ComponentName(context, DebugSubscriberReceiver::class.java.name)

    private var savedChoice: String? = null

    /**
     * Also pins the player choice to Stremio, so the asserted labels read "Open in Stremio"
     * whatever players the device has. [unsubscribe] puts the device's own choice back.
     */
    fun subscribe() {
        runBlocking {
            val dataStore = AppGraph.dataStore(context)
            savedChoice = dataStore.data.first()[PlayerChoiceStore.KEY_CHOICE]
            dataStore.edit { it[PlayerChoiceStore.KEY_CHOICE] = PlayerChoice.STREMIO.name }
        }
        DebugSubscriberReceiver.published.clear()
        sendSubscribe(TOKEN)
        awaitSubscription()
    }

    fun unsubscribe() {
        sendSubscribe(null)
        DebugSubscriberReceiver.published.clear()
        runBlocking {
            AppGraph.dataStore(context).edit { prefs ->
                savedChoice?.let { prefs[PlayerChoiceStore.KEY_CHOICE] = it }
                    ?: prefs.remove(PlayerChoiceStore.KEY_CHOICE)
            }
        }
    }

    fun sendSubscribe(token: String?) {
        context.sendBroadcast(
            Intent(IncomingConstants.ACTION_SUBSCRIBE)
                .setComponent(extension)
                .putExtra(IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT, subscriber)
                .putExtra(IncomingConstants.EXTRA_TOKEN, token)
        )
    }

    fun requestEpisode(identifier: Int, episode: Episode) {
        context.sendBroadcast(
            updateIntent(identifier).putExtra(IncomingConstants.EXTRA_EPISODE, episode.toBundle())
        )
    }

    fun requestMovie(identifier: Int, movie: Movie) {
        context.sendBroadcast(
            updateIntent(identifier).putExtra(IncomingConstants.EXTRA_MOVIE, movie.toBundle())
        )
    }

    fun episode(
        showImdbId: String?,
        showTmdbId: Int = 1399,
        showTitle: String = "Game of Thrones",
        episodeTmdbId: Int = 63056,
    ): Episode =
        Episode.Builder()
            .tmdbId(episodeTmdbId)
            .tvdbId(0)
            .title("Winter Is Coming")
            .number(1)
            .numberAbsolute(1)
            .season(1)
            .showTitle(showTitle)
            .showTmdbId(showTmdbId)
            .showTvdbId(0)
            .showImdbId(showImdbId)
            .build()

    fun movie(imdbId: String?, tmdbId: Int = 603, title: String = "The Matrix"): Movie =
        Movie.Builder().tmdbId(tmdbId).imdbId(imdbId).title(title).build()

    fun awaitPublished(identifier: Int, timeoutMs: Long): DebugSubscriberReceiver.PublishedAction =
        pollPublished(identifier, timeoutMs)
            ?: throw AssertionError("No action published for $identifier within ${timeoutMs}ms")

    /**
     * Collects one action per identifier, in whatever order they arrive — polling for them one at
     * a time would throw away the ones that arrived first. [timeoutMs] is the wait allowed per
     * action, so the clock restarts only when a wanted one lands: a straggler for some other title
     * must not buy the batch more time, or the deadline stops meaning anything.
     */
    fun awaitPublished(
        identifiers: Collection<Int>,
        timeoutMs: Long,
    ): Map<Int, DebugSubscriberReceiver.PublishedAction> {
        val outstanding = identifiers.toMutableSet()
        val collected = mutableMapOf<Int, DebugSubscriberReceiver.PublishedAction>()
        var deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (outstanding.isNotEmpty()) {
            val remaining = deadline - System.nanoTime()
            val published =
                if (remaining <= 0) null
                else DebugSubscriberReceiver.published.poll(remaining, TimeUnit.NANOSECONDS)
            if (published == null) {
                throw AssertionError("Nothing published for $outstanding within ${timeoutMs}ms")
            }
            val identifier = published.action.entityIdentifier
            if (outstanding.remove(identifier)) {
                collected[identifier] = published
                deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
            }
        }
        return collected
    }

    /**
     * Waits for an action for [identifier] specifically. A title whose IMDb id had to be looked up
     * publishes a second, upgraded action later, and that straggler can land during another test —
     * so match on the request rather than on arrival.
     */
    fun pollPublished(identifier: Int, timeoutMs: Long): DebugSubscriberReceiver.PublishedAction? {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) return null
            val published =
                DebugSubscriberReceiver.published.poll(remaining, TimeUnit.NANOSECONDS) ?: return null
            if (published.action.entityIdentifier == identifier) return published
        }
    }

    private fun updateIntent(identifier: Int): Intent =
        Intent(IncomingConstants.ACTION_UPDATE)
            .setComponent(extension)
            .putExtra(IncomingConstants.EXTRA_ENTITY_IDENTIFIER, identifier)
            .putExtra(IncomingConstants.EXTRA_VERSION, 2)

    private fun awaitSubscription() {
        val subscriptions = ExtensionSubscriptions(context)
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SUBSCRIBE_TIMEOUT_MS)
        while (System.nanoTime() < deadline) {
            if (subscriptions.subscribers().containsValue(TOKEN)) return
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        throw AssertionError("Extension never recorded the subscription")
    }

    companion object {
        const val TOKEN = "test-token"

        /**
         * The whole point of publishing from the receiver: an action arrives while SeriesGuide is
         * still waiting. Anything a deferred job could also pass would not be a regression test.
         */
        const val FAST_PUBLISH_TIMEOUT_MS = 1_000L

        /** Only for an action that is allowed to wait on a lookup. */
        const val UPGRADE_TIMEOUT_MS = 10_000L

        const val SILENCE_TIMEOUT_MS = 2_000L

        /** Setup, not a property under test: generous, so a cold DataStore cannot flake a run. */
        private const val SUBSCRIBE_TIMEOUT_MS = 5_000L
        private const val POLL_INTERVAL_MILLIS = 50L
    }
}

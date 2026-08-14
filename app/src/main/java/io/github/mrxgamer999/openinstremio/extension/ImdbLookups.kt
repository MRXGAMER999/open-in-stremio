package io.github.mrxgamer999.openinstremio.extension

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import io.github.mrxgamer999.openinstremio.data.AppGraph
import io.github.mrxgamer999.openinstremio.data.ImdbResolver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The TMDb lookups that run *after* [OpenInStremioExtensionReceiver] has already answered
 * SeriesGuide, to upgrade a published "Search in Stremio" button into a direct link.
 *
 * Detached on purpose: a broadcast that is still being processed blocks the queue behind it, so
 * the network must never be reached before `PendingResult.finish()`. What is left running
 * afterwards is best-effort — if the process dies first, the fallback button is already on screen.
 *
 * Concurrent requests for the same title share one lookup. SeriesGuide asks about several episodes
 * of a show at once, and without this every visible episode would fire its own identical call.
 */
internal object ImdbLookups {

    /**
     * [SupervisorJob] is load-bearing: under a plain [kotlinx.coroutines.Job] the first failing
     * lookup would cancel the scope and silently kill every lookup for the rest of the process'
     * life. The handler is the matching backstop — an upgrade that throws must not take the app
     * down with it.
     */
    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO +
                CoroutineExceptionHandler { _, e -> Log.w(TAG, "Detached extension work failed", e) }
        )

    /** Keyed by kind *and* id: TMDb numbers movies and shows separately, so 1399 is two titles. */
    private val inFlight = ConcurrentHashMap<String, Deferred<String?>>()

    /** Test seam: instrumented tests drive the timing of a lookup they never let reach TMDb. */
    @VisibleForTesting @Volatile var resolverOverride: ImdbResolver? = null

    /**
     * Test seam: callers attached to a lookup so far, whether they started one or joined one. A
     * test that let a gated lookup finish before the last caller had attached would watch that
     * caller start a second lookup, and read it as a dedupe failure.
     */
    @VisibleForTesting val attachedCallers = AtomicInteger()

    /** Runs [block] outside the broadcast, on the lookup scope. */
    fun launchUpgrade(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    suspend fun resolveShow(context: Context, showTmdbId: Int?): String? =
        resolve("show:$showTmdbId", context) { it.resolveShow(showTmdbId) }

    suspend fun resolveMovie(context: Context, tmdbId: Int?): String? =
        resolve("movie:$tmdbId", context) { it.resolveMovie(tmdbId) }

    private suspend fun resolve(
        key: String,
        context: Context,
        lookup: suspend (ImdbResolver) -> String?,
    ): String? {
        // The deadline lives inside the shared work, not around each await: one lookup, one
        // deadline, and a late joiner inherits whatever is left of it.
        val fresh =
            scope.async(start = CoroutineStart.LAZY) {
                withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { lookup(resolver(context)) }
            }
        // Lazy start plus putIfAbsent, never computeIfAbsent: the completion handler below removes
        // the entry, and a mapping function that completes on another thread would recurse into
        // the map mid-computeIfAbsent, which ConcurrentHashMap forbids.
        val existing = inFlight.putIfAbsent(key, fresh)
        attachedCallers.incrementAndGet()
        if (existing != null) {
            fresh.cancel()
            return existing.awaitOrNull()
        }
        // Two-arg remove: only ever evict this lookup, never a newer one for the same title.
        fresh.invokeOnCompletion { inFlight.remove(key, fresh) }
        fresh.start()
        return fresh.awaitOrNull()
    }

    private fun resolver(context: Context): ImdbResolver =
        resolverOverride ?: AppGraph.imdbResolver(context)

    /**
     * A shared lookup that failed just means no upgrade for anyone waiting on it. The caller's own
     * cancellation is a different thing and has to keep travelling, hence [ensureActive] rather
     * than `runCatching`.
     */
    private suspend fun Deferred<String?>.awaitOrNull(): String? =
        try {
            await()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Log.w(TAG, "IMDb-id lookup failed", e)
            null
        }

    private const val TAG = "OpenInStremioExt"

    /**
     * Deliberately longer than the TMDb client's 4s call timeout, so OkHttp fails first and
     * [io.github.mrxgamer999.openinstremio.data.DefaultImdbResolver] still gets to classify the
     * failure (a 404 is worth remembering). This is only a backstop for a hung cache read.
     */
    private const val LOOKUP_TIMEOUT_MS = 6_000L
}

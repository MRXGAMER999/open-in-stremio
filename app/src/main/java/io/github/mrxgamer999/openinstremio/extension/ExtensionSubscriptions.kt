package io.github.mrxgamer999.openinstremio.extension

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.constants.IncomingConstants
import com.battlelancer.seriesguide.api.constants.OutgoingConstants

/**
 * The subscriber pairings SeriesGuide hands out, plus the broadcast that carries an action back
 * to them.
 *
 * This replaces bookkeeping the seriesguide-api base class does, deliberately: the library only
 * runs that code inside a JobIntentService, and JobScheduler is free to delay a job for minutes —
 * long enough for SeriesGuide to have dropped the request (it keeps only the last five requested
 * titles). Owning the pairing here lets the broadcast receiver publish while SeriesGuide is still
 * waiting for an answer.
 *
 * Storage keeps the library's file, key and `<component>|<token>` format — not for interop, which
 * ended with the service, but because that file already exists on every installed device: changing
 * it would silently unpair everyone until they toggled the extension in SeriesGuide again.
 * [PREFS_FILE] is also named in `backup_rules.xml` and `data_extraction_rules.xml` — a stale
 * restored token makes SeriesGuide silently ignore every action, so keep the three in sync.
 */
internal class ExtensionSubscriptions(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun subscribers(): Map<ComponentName, String> =
        prefs.getStringSet(PREF_SUBSCRIPTIONS, null).orEmpty().mapNotNull(::parseSubscription).toMap()

    /**
     * Mirrors the base class' subscribe handling: a blank token means unsubscribe. Returns whether
     * any subscriber is left, i.e. whether the extension is enabled in some app.
     */
    // commit, not apply: the pairing is the one piece of state that cannot be recovered without
    // SeriesGuide re-subscribing, and this never runs on the publish path.
    @SuppressLint("ApplySharedPref")
    fun setSubscription(subscriber: ComponentName, token: String?): Boolean {
        val key = subscriber.canonical()
        val updated = subscribers().toMutableMap()
        if (token.isNullOrEmpty()) updated.remove(key) else updated[key] = token
        prefs.edit(commit = true) {
            putStringSet(
                PREF_SUBSCRIPTIONS,
                updated.map { (component, value) -> "${component.flattenToShortString()}|$value" }.toSet(),
            )
        }
        return updated.isNotEmpty()
    }

    /** Sends [action] to every current subscriber. No-op when nobody is subscribed. */
    fun publish(action: Action, actionType: Int) {
        val actionBundle = action.toBundle()
        subscribers().forEach { (subscriber, token) ->
            context.sendBroadcast(
                Intent(OutgoingConstants.ACTION_PUBLISH_ACTION)
                    .setComponent(subscriber)
                    .putExtra(IncomingConstants.EXTRA_TOKEN, token)
                    .putExtra(OutgoingConstants.EXTRA_ACTION, actionBundle)
                    .putExtra(OutgoingConstants.EXTRA_ACTION_TYPE, actionType)
            )
        }
    }

    /**
     * The flattened short form is what gets stored, and unflattening expands a leading "." back
     * into the package — so a subscriber built with a short class name would not match the entry
     * it wrote, and could never unsubscribe itself. Key on the form that round-trips.
     */
    private fun ComponentName.canonical(): ComponentName =
        ComponentName.unflattenFromString(flattenToShortString()) ?: this

    private fun parseSubscription(serialized: String): Pair<ComponentName, String>? {
        val separator = serialized.indexOf('|')
        if (separator <= 0) return null
        val subscriber = ComponentName.unflattenFromString(serialized.substring(0, separator))
        val token = serialized.substring(separator + 1)
        return if (subscriber == null || token.isEmpty()) null else subscriber to token
    }

    companion object {
        /**
         * The name the extension was registered under while `SeriesGuideExtension` still derived
         * the prefs file from it. A literal now, not a reference: the file it names is already on
         * disk for existing users, so it has to outlive the class it came from.
         */
        private const val EXTENSION_NAME = "OpenInStremioExtension"

        /** The library's own layout: `seriesguideextension_<name>`, kept for compatibility. */
        const val PREFS_FILE = "seriesguideextension_$EXTENSION_NAME"

        const val PREF_SUBSCRIPTIONS = "subscriptions"
    }
}

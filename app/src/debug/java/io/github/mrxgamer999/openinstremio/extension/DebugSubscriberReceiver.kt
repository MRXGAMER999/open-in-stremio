package io.github.mrxgamer999.openinstremio.extension

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.constants.IncomingConstants
import com.battlelancer.seriesguide.api.constants.OutgoingConstants
import java.util.concurrent.LinkedBlockingQueue

/**
 * Debug-only stand-in for SeriesGuide's ExtensionActionReceiver, so an instrumented test can be a
 * real subscriber and observe what the extension publishes.
 *
 * It lives in the debug variant of the app rather than in the test APK on purpose: published
 * actions are addressed to an explicit component, and a receiver declared by the test package
 * would be delivered into the test package's own process, where the instrumented test could not
 * see what it recorded.
 */
class DebugSubscriberReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val actionBundle = intent.getBundleExtra(OutgoingConstants.EXTRA_ACTION) ?: return
        val action = Action.fromBundle(actionBundle) ?: return
        published.add(
            PublishedAction(
                action = action,
                actionType = intent.getIntExtra(OutgoingConstants.EXTRA_ACTION_TYPE, -1),
                token = intent.getStringExtra(IncomingConstants.EXTRA_TOKEN),
            )
        )
    }

    data class PublishedAction(val action: Action, val actionType: Int, val token: String?)

    companion object {
        val published = LinkedBlockingQueue<PublishedAction>()
    }
}

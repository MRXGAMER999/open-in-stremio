package io.github.mrxgamer999.openinstremio.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import io.github.mrxgamer999.openinstremio.data.Packages

/**
 * Small helpers for launching other apps. All targets are explicit (component or package)
 * and every launch is wrapped, so a missing app can never crash us.
 */
object ExternalIntents {

    private const val SG_EXTENSIONS_ACTIVITY =
        "com.battlelancer.seriesguide.extensions.ExtensionsConfigurationActivity"

    /**
     * Opens SeriesGuide's extension configuration screen (exported, but has no intent
     * filter, so it must be addressed by explicit component). Tries both SeriesGuide
     * flavors, then falls back to just launching SeriesGuide.
     *
     * @return false when SeriesGuide isn't installed at all.
     */
    fun openSeriesGuideExtensions(context: Context): Boolean {
        for (pkg in listOf(Packages.SERIESGUIDE, Packages.SERIESGUIDE_AMAZON)) {
            try {
                context.startActivity(
                    Intent()
                        .setComponent(ComponentName(pkg, SG_EXTENSIONS_ACTIVITY))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return true
            } catch (e: ActivityNotFoundException) {
                // try next flavor
            } catch (e: SecurityException) {
                // activity exists but refused us; fall through to the launch intent
            }
        }
        for (pkg in listOf(Packages.SERIESGUIDE, Packages.SERIESGUIDE_AMAZON)) {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                context.startActivity(it)
                return true
            }
        }
        return false
    }

    /** Opens the Play Store page for [packageName], falling back to the web listing. */
    fun openPlayStore(context: Context, packageName: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            openUrl(context, "https://play.google.com/store/apps/details?id=$packageName")
        }
    }

    /** Opens [url] in whatever handles it; silently ignores devices with no handler. */
    fun openUrl(context: Context, url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            // No browser available (some TV devices) - nothing sensible to do.
        }
    }
}

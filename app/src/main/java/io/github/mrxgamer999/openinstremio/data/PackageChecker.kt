package io.github.mrxgamer999.openinstremio.data

import android.content.Context
import android.content.pm.PackageManager

/** Package ids this app interacts with. All must be declared in the manifest `<queries>`. */
object Packages {
    const val SERIESGUIDE = "com.battlelancer.seriesguide"
    const val SERIESGUIDE_AMAZON = "com.uwetrottmann.seriesguide.amzn"
    const val STREMIO = "com.stremio.one"
}

fun interface PackageChecker {
    fun isInstalled(packageName: String): Boolean
}

class AndroidPackageChecker(private val context: Context) : PackageChecker {
    override fun isInstalled(packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
}

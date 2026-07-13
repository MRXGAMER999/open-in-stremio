package io.github.mrxgamer999.openinstremio.extension

import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver

/**
 * Discovery point for SeriesGuide: it finds extensions by querying for receivers with the
 * `com.battlelancer.seriesguide.api.SeriesGuideExtension` action and forwards its broadcasts
 * to the extension service through this receiver.
 */
class OpenInStremioExtensionReceiver : SeriesGuideExtensionReceiver() {

    override fun getJobId(): Int = 1000

    override fun getExtensionClass(): Class<out SeriesGuideExtension> =
        OpenInStremioExtension::class.java
}

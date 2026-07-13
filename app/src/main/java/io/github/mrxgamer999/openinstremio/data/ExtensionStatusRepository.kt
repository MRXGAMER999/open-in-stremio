package io.github.mrxgamer999.openinstremio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ExtensionStatus(
    val seriesGuideInstalled: Boolean,
    val stremioInstalled: Boolean,
    val extensionActive: Boolean,
)

/**
 * Combines package-install checks with the extension-active flag persisted by the
 * extension service; drives the Home screen's status card.
 */
class ExtensionStatusRepository(
    private val packageChecker: PackageChecker,
    extensionActive: Flow<Boolean>,
) {
    val status: Flow<ExtensionStatus> =
        extensionActive.map { active ->
            ExtensionStatus(
                seriesGuideInstalled =
                    packageChecker.isInstalled(Packages.SERIESGUIDE) ||
                        packageChecker.isInstalled(Packages.SERIESGUIDE_AMAZON),
                stremioInstalled = packageChecker.isInstalled(Packages.STREMIO),
                extensionActive = active,
            )
        }
}

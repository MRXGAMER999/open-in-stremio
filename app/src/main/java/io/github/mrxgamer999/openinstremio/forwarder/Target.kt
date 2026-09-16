package io.github.mrxgamer999.openinstremio.forwarder

import io.github.mrxgamer999.openinstremio.data.PackageChecker
import io.github.mrxgamer999.openinstremio.data.Packages

/**
 * The apps a published action can hand a title to.
 *
 * Deliberately a bare enum: with two targets, a sealed class carrying link builders and labels
 * would be a layer of indirection over two `when` branches. Labels live in the UI layer so this
 * stays free of resource ids and usable from the ViewModel.
 *
 * Declaration order is load-bearing twice: it is the order the chooser offers them in, and it
 * picks the install nudge shown when neither is there.
 */
enum class Target(val packageId: String) {
    STREMIO(Packages.STREMIO),
    FIREGUY(Packages.FIREGUY),
}

/** The subset of [Target] present on this device, in declaration order. */
fun PackageChecker.installedTargets(): List<Target> = Target.entries.filter { isInstalled(it.packageId) }

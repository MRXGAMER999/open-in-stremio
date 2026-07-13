package io.github.mrxgamer999.openinstremio.util

/**
 * Compares release tags like "v1.2.0" against the installed version name. Tolerant of a
 * leading `v`, pre-release/build suffixes (`-beta1`, `+42`), and malformed segments -
 * a version that cannot be parsed simply never reports as newer.
 */
object VersionComparator {

    fun isNewer(current: String, latest: String): Boolean {
        val currentParts = normalize(current)
        val latestParts = normalize(latest)
        val size = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until size) {
            val diff = latestParts.getOrElse(i) { 0 } - currentParts.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }

    private fun normalize(version: String): List<Int> =
        version
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
}

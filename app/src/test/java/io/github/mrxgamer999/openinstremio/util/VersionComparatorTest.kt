package io.github.mrxgamer999.openinstremio.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun equalVersions_notNewer() {
        assertFalse(VersionComparator.isNewer(current = "1.0.0", latest = "1.0.0"))
        assertFalse(VersionComparator.isNewer(current = "1.0.0", latest = "v1.0.0"))
    }

    @Test
    fun patchMinorMajor_newer() {
        assertTrue(VersionComparator.isNewer(current = "1.0.0", latest = "1.0.1"))
        assertTrue(VersionComparator.isNewer(current = "1.0.0", latest = "1.1.0"))
        assertTrue(VersionComparator.isNewer(current = "1.9.9", latest = "2.0.0"))
    }

    @Test
    fun olderVersions_notNewer() {
        assertFalse(VersionComparator.isNewer(current = "1.1.0", latest = "1.0.9"))
        assertFalse(VersionComparator.isNewer(current = "2.0.0", latest = "v1.9.9"))
    }

    @Test
    fun vPrefix_andDifferentSegmentCounts() {
        assertTrue(VersionComparator.isNewer(current = "1.0", latest = "v1.0.1"))
        assertFalse(VersionComparator.isNewer(current = "1.0.0", latest = "1.0"))
        assertTrue(VersionComparator.isNewer(current = "1.0.0", latest = "1.0.0.1"))
    }

    @Test
    fun preReleaseAndBuildMetadata_ignored() {
        assertTrue(VersionComparator.isNewer(current = "1.0.0", latest = "v2.0.0-beta1"))
        assertFalse(VersionComparator.isNewer(current = "1.0.0", latest = "1.0.0+42"))
    }

    @Test
    fun garbageInput_neverCrashes_neverNewer() {
        assertFalse(VersionComparator.isNewer(current = "1.0.0", latest = "not-a-version"))
        assertFalse(VersionComparator.isNewer(current = "abc", latest = "abc"))
        assertTrue(VersionComparator.isNewer(current = "abc", latest = "0.0.1"))
    }
}

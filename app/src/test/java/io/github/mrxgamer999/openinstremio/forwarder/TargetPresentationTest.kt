package io.github.mrxgamer999.openinstremio.forwarder

import io.github.mrxgamer999.openinstremio.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** The one action SeriesGuide shows has to name wherever the tap will actually land. */
class TargetPresentationTest {

    @Test
    fun nothingInstalled_keepsTheStremioLabel() {
        assertEquals(R.string.action_open_in_stremio, actionLabelRes(emptyList(), open = true))
        assertEquals(R.string.action_search_in_stremio, actionLabelRes(emptyList(), open = false))
    }

    @Test
    fun onlyStremio_keepsTheStremioLabel() {
        val installed = listOf(Target.STREMIO)

        assertEquals(R.string.action_open_in_stremio, actionLabelRes(installed, open = true))
        assertEquals(R.string.action_search_in_stremio, actionLabelRes(installed, open = false))
    }

    @Test
    fun onlyFireguy_namesFireguy() {
        val installed = listOf(Target.FIREGUY)

        assertEquals(R.string.action_open_in_fireguy, actionLabelRes(installed, open = true))
        assertEquals(R.string.action_search_in_fireguy, actionLabelRes(installed, open = false))
    }

    @Test
    fun bothInstalled_namesNeither() {
        val installed = listOf(Target.STREMIO, Target.FIREGUY)

        assertEquals(R.string.action_open_in, actionLabelRes(installed, open = true))
        assertEquals(R.string.action_search_in, actionLabelRes(installed, open = false))
    }
}

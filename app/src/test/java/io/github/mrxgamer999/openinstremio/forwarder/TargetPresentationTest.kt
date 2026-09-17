package io.github.mrxgamer999.openinstremio.forwarder

import io.github.mrxgamer999.openinstremio.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** The one action SeriesGuide shows has to name wherever the tap will actually land. */
class TargetPresentationTest {

    @Test
    fun nothingInstalled_keepsTheStremioLabel() {
        assertEquals(R.string.action_open_in_stremio, actionLabelRes(Target.entries, emptyList(), open = true))
        assertEquals(R.string.action_search_in_stremio, actionLabelRes(Target.entries, emptyList(), open = false))
    }

    @Test
    fun onlyStremio_keepsTheStremioLabel() {
        val installed = listOf(Target.STREMIO)

        assertEquals(R.string.action_open_in_stremio, actionLabelRes(Target.entries, installed, open = true))
        assertEquals(R.string.action_search_in_stremio, actionLabelRes(Target.entries, installed, open = false))
    }

    @Test
    fun onlyFireguy_namesFireguy() {
        val installed = listOf(Target.FIREGUY)

        assertEquals(R.string.action_open_in_fireguy, actionLabelRes(Target.entries, installed, open = true))
        assertEquals(R.string.action_search_in_fireguy, actionLabelRes(Target.entries, installed, open = false))
    }

    @Test
    fun bothInstalled_namesNeither() {
        val installed = listOf(Target.STREMIO, Target.FIREGUY)

        assertEquals(R.string.action_open_in, actionLabelRes(Target.entries, installed, open = true))
        assertEquals(R.string.action_search_in, actionLabelRes(Target.entries, installed, open = false))
    }

    @Test
    fun onlyFireguyChosen_butMissing_namesFireguy() {
        val chosen = listOf(Target.FIREGUY)

        assertEquals(R.string.action_open_in_fireguy, actionLabelRes(chosen, listOf(Target.STREMIO), open = true))
        assertEquals(R.string.action_search_in_fireguy, actionLabelRes(chosen, emptyList(), open = false))
    }

    @Test
    fun onlyStremioChosen_withBothInstalled_namesStremio() {
        val installed = listOf(Target.STREMIO, Target.FIREGUY)

        assertEquals(R.string.action_open_in_stremio, actionLabelRes(listOf(Target.STREMIO), installed, open = true))
    }
}

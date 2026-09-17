package io.github.mrxgamer999.openinstremio.data

/**
 * Which player(s) the user wants the SeriesGuide button to reach. [BOTH] is also what nobody
 * having chosen means: it is how the button behaved before there was a choice to make.
 */
enum class PlayerChoice {
    STREMIO,
    FIREGUY,
    BOTH,
}

package me.fireandice.ghosttracker.hud.elements

import me.fireandice.ghosttracker.config.HudColor

class ColoredText(
    var text: String,
    private val colorRef: () -> HudColor
) {
    val color get() = colorRef().packed
}

/**
 * Creates a [ColoredText] with the receiving [String] and the color property from your config
 */
infix fun String.withColor(color: () -> HudColor) = ColoredText(this, color)

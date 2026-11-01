package me.fireandice.ghosttracker.hud.elements

import me.fireandice.ghosttracker.config.GhostConfig
import me.fireandice.ghosttracker.hud.Images
import me.fireandice.ghosttracker.utils.FONT_HEIGHT
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

/**
 * A hud line that has two different colored text components. Used for drops and time hud lines. Note: prefix should
 * include a space after, and suffix should include a space before
 * @param prefix The prefix text. This is not a [ColoredText] because it will always be the same color as [main]
 * @param main The main text that displays the relevant tracker stat
 * @param suffix Text after the main text that may be hidden by the user
 * @param image The icon that may display before the hud line
 * @param visible The backing property of the config option that decides if the line is shown
 * @param suffixVisible An expression to calculate if the suffix should be shown
 */
class SuffixHudLine(
    var prefix: String? = null,
    var main: ColoredText,
    var suffix: ColoredText,
    private val image: Identifier,
    val visible: () -> Boolean,
    val suffixVisible: () -> Boolean
) : HudLine {

    override var width: Float = 0f
    override var height: Float = 0f

    override fun draw(x: Int, y: Int): Boolean {
        return draw(x, y, null)
    }

    /**
     * Rendered through the given [GuiGraphicsExtractor] when null the width is only computed.
     */
    fun draw(x: Int, y: Int, graphics: GuiGraphicsExtractor?): Boolean {
        if (!visible()) {
            width = 0f
            height = 0f
            return false
        }
        height = FONT_HEIGHT.toFloat()

        var currentX = x
        var currentWidth = 0f
        val font = Minecraft.getInstance().font

        if (GhostConfig.showIcons) {
            BasicHudLine.drawIcon(graphics, image, x, y)
            currentX += 10
            currentWidth += 10
        }

        if (GhostConfig.showPrefixes) {
            BasicHudLine.drawText(graphics, prefix, currentX, y, main.color)
            val prefixWidth = font.width(prefix ?: "")
            currentX += prefixWidth
            currentWidth += prefixWidth
        }

        BasicHudLine.drawText(graphics, main.text, currentX, y, main.color)
        val textWidth = font.width(main.text)
        currentWidth += textWidth

        if (suffixVisible()) {
            currentX += textWidth
            BasicHudLine.drawText(graphics, suffix.text, currentX, y, suffix.color)
            currentWidth += font.width(suffix.text)
        }

        width = currentWidth
        return true
    }
}

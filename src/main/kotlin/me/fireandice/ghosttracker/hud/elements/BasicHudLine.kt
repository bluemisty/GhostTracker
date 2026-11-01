package me.fireandice.ghosttracker.hud.elements

import me.fireandice.ghosttracker.config.GhostConfig
import me.fireandice.ghosttracker.hud.Images
import me.fireandice.ghosttracker.utils.FONT_HEIGHT
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

/**
 * A general hud line that only uses one color. This is used for kills, magic find, and all xp displays.
 * Note: the prefix should have a space at the end.
 * @param prefix The prefix text. This is not a [ColoredText] because it will always be the same color as [main]
 * @param main The main text that displays the relevant tracker stat
 * @param image The icon that may display before the hud line
 * @param visible The backing property of the config option that decides if the line is shown
 */
class BasicHudLine(
    var prefix: String? = null,
    var main: ColoredText,
    private val image: Identifier,
    private val visible: () -> Boolean
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
            drawIcon(graphics, image, x, y)
            currentX += 10
            currentWidth += 10
        }

        if (GhostConfig.showPrefixes) {
            drawText(graphics, prefix, currentX, y, main.color)
            val prefixWidth = font.width(prefix ?: "")
            currentX += prefixWidth
            currentWidth += prefixWidth
        }

        drawText(graphics, main.text, currentX, y, main.color)
        currentWidth += font.width(main.text)
        width = currentWidth
        return true
    }

    companion object {
        /**
         * Hud icons are 8x8 textures, drawn at a 8px size with a 2px gap before the text.
         */
        fun drawIcon(graphics: GuiGraphicsExtractor?, image: Identifier, x: Int, y: Int) {
            graphics?.blit(
                RenderPipelines.GUI_TEXTURED,
                image,
                x, y,
                0f, 0f,
                8, 8,
                8, 8
            )
        }

        /**
         * Modern versions receive plain text strings; using `dropShadow = true` replicates the old OneConfig
         * "Shadow" option (both shadow types map onto this since vanilla has no "full shadow" mode).
         */
        fun drawText(graphics: GuiGraphicsExtractor?, text: String?, x: Int, y: Int, color: Int) {
            if (graphics == null || text == null) return
            val font = Minecraft.getInstance().font
            graphics.text(font, text, x, y, color, GhostConfig.shadow)
        }
    }
}

package me.fireandice.ghosttracker.hud

import me.fireandice.ghosttracker.config.GhostConfig
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Simple hud editor: shows both huds with their example lines, click and drag to move a hud,
 * scroll to change its scale. Replaces the old OneConfig HUD station.
 */
class HudEditorScreen(
    private val parent: Screen,
    initialHudIndex: Int
) : Screen(Component.literal("Edit HUDs")) {

    private val huds = listOf(
        "tracker" to GhostConfig.trackerHud as HudInstance,
        "timer" to GhostConfig.timerHud as HudInstance
    )
    private var dragging: Pair<Int, GrabOffset>? = null
    private var selected = initialHudIndex

    private data class GrabOffset(val x: Int, val y: Int)

    override fun init() {
        addRenderableWidget(
            Button.builder(Component.literal("Reset position")) {
                val pos = positionOf(selected)
                pos.x = 100
                pos.y = 100 + selected * 20
                pos.scale = 1f
            }.bounds(width / 2 - 110, height - 50, 100, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Done")) {
                HudManager.save()
                minecraft?.setScreen(parent)
            }.bounds(width / 2 + 10, height - 50, 100, 20).build()
        )
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, 0xB0000000.toInt())
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        graphics.centeredText(font, title.getString(), width / 2, 10, 0xFFFFFF)
        graphics.centeredText(font, "Click and drag a hud to move it, scroll while hovering to resize", width / 2, 24, 0xAAAAAA)

        for ((index, hudEntry) in huds.withIndex()) {
            val hud = hudEntry.second
            // Refresh the example values and draw; width/height get computed on draw
            hud.drawExample(graphics)
            val pos = positionOf(index)
            graphics.fill(pos.x - 2, pos.y - 2, pos.x + hud.width.toInt() + 2, pos.y + hud.height.toInt() + 2, selectedColor(index))
            graphics.centeredText(
                font,
                hudEntry.first.replaceFirstChar { it.uppercase() },
                pos.x + hud.width.toInt() / 2,
                pos.y - 12,
                0xFFFFFF
            )
            hoverIndex(mouseX, mouseY)?.let { selected = it }
        }
    }

    private fun selectedColor(index: Int) = if (index == selected) 0x503282C8 else 0x3F3282C8

    private fun positionOf(index: Int): HudPosition = when (index) {
        0 -> HudManager.tracker
        else -> HudManager.timer
    }

    private fun hoverIndex(mouseX: Int, mouseY: Int): Int? {
        for (index in huds.indices) {
            if (isOverHud(index, mouseX, mouseY)) return index
        }
        return null
    }

    private fun isOverHud(index: Int, mouseX: Int, mouseY: Int): Boolean {
        val hud = huds[index].second
        val pos = positionOf(index)
        return mouseX in pos.x..(pos.x + hud.width.toInt() + 4) && mouseY in pos.y..(pos.y + hud.height.toInt() + 4)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        hoverIndex(event.x.toInt(), event.y.toInt())?.let { index ->
            selected = index
            val pos = positionOf(index)
            dragging = index to GrabOffset(event.x.toInt() - pos.x, event.y.toInt() - pos.y)
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val (index, offset) = dragging ?: return super.mouseDragged(event, dragX, dragY)
        // Position by the mouse, not by the drag delta, so the hud stays glued to the cursor
        val pos = positionOf(index)
        pos.x = (event.x.toInt() - offset.x).coerceIn(4, width - 20)
        pos.y = (event.y.toInt() - offset.y).coerceIn(4, height - 20)
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        dragging = null
        HudManager.save()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        hoverIndex(mouseX.toInt(), mouseY.toInt())?.let { index ->
            val pos = positionOf(index)
            // scroll up = closer and bigger, scroll down = smaller
            pos.scale = (pos.scale + (scrollY * 0.1).toFloat()).coerceIn(0.5f, 3f)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun onClose() {
        HudManager.save()
        minecraft?.setScreen(parent)
        super.onClose()
    }
}

package me.fireandice.ghosttracker.config

import me.fireandice.ghosttracker.GhostTracker
import me.fireandice.ghosttracker.hud.HudEditorScreen
import me.fireandice.ghosttracker.hud.HudManager
import me.fireandice.ghosttracker.platform.Platform
import me.fireandice.ghosttracker.tracker.GhostTimer
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * A simple paginated config screen that replaces the OneConfig one. The options live in [GhostConfig]
 * with the same names, and HUD positioning happens in [HudEditorScreen].
 */
class GhostConfigScreen : Screen(Component.literal("GhostTracker")) {

    private enum class Page(val label: String) {
        GENERAL("General"),
        STAT_TRACKER("Stat Tracker"),
        SESSION_TIMER("Session Timer"),
        COLORS("Colors")
    }

    private companion object {
        const val BUTTON_WIDTH = 110
        const val BUTTON_HEIGHT = 20
        const val ROW_SPACING = 24
        const val ROW_MARGIN = 12
    }

    private var page = Page.GENERAL
    private var scrollRows = 0

    /**
     * One row of the config page.
     * @param label The option name, drawn on the left side
     * @param valueText Only for steppers: a live-value text drawn between the minus and plus buttons
     * @param createButtons Builds the widget(s) that change the option. Number steppers build two buttons.
     */
    private data class RowDefinition(
        val label: String,
        val valueText: (() -> String)? = null,
        val createButtons: (x: Int, y: Int) -> List<Button>
    )

    private val visibleRows = ArrayList<Pair<RowDefinition, List<Button>>>()

    override fun init() {
        rebuild()
    }

    /** Rebuilds the widgets for the currently visible rows (on page switches and scrolling). */
    private fun rebuild() {
        clearWidgets()
        visibleRows.clear()

        val definitions = pageRows(page)
        val visibleCount = maxVisibleRows()
        scrollRows = scrollRows.coerceIn(0, (definitions.size - visibleCount).coerceAtLeast(0))

        definitions.drop(scrollRows).take(visibleCount).forEachIndexed { index, definition ->
            val y = rowsTop() + index * ROW_SPACING
            val buttons = definition.createButtons(width - BUTTON_WIDTH - ROW_MARGIN, y)
            visibleRows += definition to buttons
            buttons.forEach { addRenderableWidget(it) }
        }

        Page.entries.forEachIndexed { index, pageEntry ->
            val tabX = (width - Page.entries.size * 84) / 2 + index * 86
            addRenderableWidget(
                Button.builder(Component.literal(pageEntry.label)) { setPage(pageEntry) }
                    .bounds(tabX, 26, 80, 20)
                    .build()
            )
        }
    }

    private fun rowsTop(): Int = 58

    private fun maxVisibleRows(): Int = ((height - rowsTop() - 30) / ROW_SPACING).coerceAtLeast(1)

    private fun setPage(newPage: Page) {
        page = newPage
        scrollRows = 0
        rebuild()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (pageRows(page).size > maxVisibleRows()) {
            val before = scrollRows
            scrollRows -= if (scrollY > 0) 1 else -1
            if (scrollRows != before) {
                rebuild()
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    /**
     * Modern screens render through the extract pipeline; draw our labels on top of the widgets.
     */
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        graphics.centeredText(font, title.string, width / 2, 10, 0xFFFFFF)

        for ((definition, buttons) in visibleRows) {
            if (buttons.isEmpty()) continue
            val button = buttons.first()
            graphics.text(font, definition.label, ROW_MARGIN, button.y + (BUTTON_HEIGHT - 8) / 2, 0xFFFFFF, true)

            if (definition.valueText != null && buttons.size > 1) {
                val first = buttons.first()
                val last = buttons.last()
                val centerX = (first.x + first.width + last.x) / 2
                graphics.centeredText(font, definition.valueText(), centerX, button.y + 6, 0xFFFFFF)
            }
        }
    }

    override fun onClose() {
        GhostConfig.save()
        HudManager.save()
        minecraft?.setScreen(null)
        super.onClose()
    }

    //</editor-fold desc="Option row builders">
    private fun toggle(label: String, get: () -> Boolean, set: (Boolean) -> Unit): RowDefinition =
        RowDefinition(label) { x, y ->
            listOf(
                Button.builder(Component.literal(onOff(get()))) {
                    set(!get())
                    it.message = Component.literal(onOff(get()))
                }.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            )
        }

    private fun onOff(current: Boolean) = if (current) "ON" else "OFF"

    /**
     * A button that cycles through some values. Its label always comes from [display], which reads
     * the backing option, so it stays correct over multiple clicks.
     */
    private fun cyclingButton(label: String, display: () -> String, cycle: () -> Unit): RowDefinition =
        RowDefinition(label) { x, y ->
            listOf(
                Button.builder(Component.literal(display())) {
                    cycle()
                    it.message = Component.literal(display())
                }.bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            )
        }

    private fun plusMinus(label: String, min: Int, max: Int, get: () -> Int, set: (Int) -> Unit): RowDefinition {
        val dec: () -> Unit = { set((get() - 1).coerceAtLeast(min)) }
        val inc: () -> Unit = { set((get() + 1).coerceAtMost(max)) }
        return RowDefinition(label, { get().toString() }) { x, y ->
            listOf(
                Button.builder(Component.literal("-")) { dec() }.bounds(x, y, 30, BUTTON_HEIGHT).build(),
                Button.builder(Component.literal("+")) { inc() }.bounds(x + BUTTON_WIDTH - 30, y, 30, BUTTON_HEIGHT).build()
            )
        }
    }

    private fun actionButton(label: String, widthOverride: Int = BUTTON_WIDTH, action: () -> Unit): RowDefinition =
        RowDefinition(label) { x, y ->
            listOf(
                Button.builder(Component.literal(label)) { action() }
                    .bounds(x, y, widthOverride, BUTTON_HEIGHT).build()
            )
        }

    /**
     * Keybinding display rows. The keybind itself executes the action when pressed here; the actual
     * rebinding happens in the vanilla controls menu (Options > Controls > Keybinds).
     */
    private fun keybind(label: String, mapping: () -> KeyMapping, action: () -> Unit): RowDefinition {
        val boundKeyDisplay = mapping().let { net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(it).displayName }
        return actionButton("$label (${boundKeyDisplay?.string ?: "-"})", BUTTON_WIDTH, action)
    }
    //</editor-fold>

    //</editor-fold desc="Pages">
    private fun pageRows(page: Page): List<RowDefinition> = when (page) {
        Page.GENERAL -> listOf(
            toggle("Show everywhere", { GhostConfig.showEverywhere }, { GhostConfig.showEverywhere = it }),
            toggle("Show icons", { GhostConfig.showIcons }, { GhostConfig.showIcons = it }),
            toggle("Abbreviate text", { GhostConfig.abbreviate }, { GhostConfig.abbreviate = it }),
            toggle("Show margins", { GhostConfig.showMargins }, { GhostConfig.showMargins = it }),
            cyclingButton(
                "Shadow type",
                { listOf("None", "Shadow", "Full shadow")[GhostConfig.shadowOption] }
            ) { GhostConfig.cycleShadowOption() },
            plusMinus("Looting level", 0, 10, { GhostConfig.lootingLevel }, { GhostConfig.lootingLevel = it }),
            plusMinus("Luck level", 0, 10, { GhostConfig.luckLevel }, { GhostConfig.luckLevel = it }),
            cyclingButton(
                "Price timespan",
                { listOf("Hour", "Day", "Week")[GhostConfig.priceTimespanOption] }
            ) { GhostConfig.priceTimespanOption = (GhostConfig.priceTimespanOption + 1) % 3 },
            plusMinus("Price fetch freq (min)", 5, 120, { GhostConfig.priceFrequency }, { GhostConfig.priceFrequency = it }),
            cyclingButton(
                "Gemstone display",
                { listOf("Count", "Coin value", "Both")[GhostConfig.gemstoneDisplayOption] }
            ) { GhostConfig.gemstoneDisplayOption = (GhostConfig.gemstoneDisplayOption + 1) % 3 }
        )
        Page.STAT_TRACKER -> listOf(
            actionButton("Edit HUD", 120) { openHudEditor(0) },
            actionButton("Reset tracker", 120) { GhostTracker.resetStats() },
            keybind("Reset keybind", { Platform.trackerResetKb }) { GhostTracker.resetStats() },
            toggle("Show kill count", { GhostConfig.tracker_kills }, { GhostConfig.tracker_kills = it }),
            toggle("Show sorrow count", { GhostConfig.tracker_sorrow }, { GhostConfig.tracker_sorrow = it }),
            toggle("Show volta count", { GhostConfig.tracker_volta }, { GhostConfig.tracker_volta = it }),
            toggle("Show plasma count", { GhostConfig.tracker_plasma }, { GhostConfig.tracker_plasma = it }),
            toggle("Show ghostly boots count", { GhostConfig.tracker_boots }, { GhostConfig.tracker_boots = it }),
            toggle("Show 1m coin drop count", { GhostConfig.tracker_coins }, { GhostConfig.tracker_coins = it }),
            toggle("Show avg magic find", { GhostConfig.tracker_mf }, { GhostConfig.tracker_mf = it }),
            toggle("Show avg combat XP", { GhostConfig.tracker_averageXp }, { GhostConfig.tracker_averageXp = it }),
            toggle("Show total combat XP", { GhostConfig.tracker_totalXp }, { GhostConfig.tracker_totalXp = it }),
            toggle("Show shard count", { GhostConfig.tracker_shardCount }, { GhostConfig.tracker_shardCount = it }),
            toggle("Show average shards", { GhostConfig.tracker_shardAverage }, { GhostConfig.tracker_shardAverage = it }),
            toggle("Show gemstones", { GhostConfig.tracker_gemstones }, { GhostConfig.tracker_gemstones = it }),
            toggle("Show scavenger coins", { GhostConfig.tracker_scavenger }, { GhostConfig.tracker_scavenger = it }),
            toggle("Show total money", { GhostConfig.tracker_totalMoney }, { GhostConfig.tracker_totalMoney = it })
        )
        Page.SESSION_TIMER -> listOf(
            actionButton("Edit HUD", 120) { openHudEditor(1) },
            actionButton("Start timer", 120) { GhostTimer.start() },
            actionButton("Pause timer", 120) { GhostTimer.pause() },
            actionButton("Reset timer", 120) { GhostTimer.reset() },
            keybind("Start/pause keybind", { Platform.timerStartPauseKb }) {
                if (GhostTimer.isTracking) GhostTimer.pause() else GhostTimer.start()
            },
            toggle("Show kills per hour", { GhostConfig.timer_kills }, { GhostConfig.timer_kills = it }),
            toggle("Show sorrows per hour", { GhostConfig.timer_sorrow }, { GhostConfig.timer_sorrow = it }),
            toggle("Show voltas per hour", { GhostConfig.timer_volta }, { GhostConfig.timer_volta = it }),
            toggle("Show plasmas per hour", { GhostConfig.timer_plasma }, { GhostConfig.timer_plasma = it }),
            toggle("Show ghostly boots per hour", { GhostConfig.timer_boots }, { GhostConfig.timer_boots = it }),
            toggle("Show 1m coins per hour", { GhostConfig.timer_coins }, { GhostConfig.timer_coins = it }),
            toggle("Show average magic find", { GhostConfig.timer_mf }, { GhostConfig.timer_mf = it }),
            toggle("Show average combat XP", { GhostConfig.timer_averageXp }, { GhostConfig.timer_averageXp = it }),
            toggle("Show combat XP per hour", { GhostConfig.timer_xpRate }, { GhostConfig.timer_xpRate = it }),
            toggle("Show scavenger coins", { GhostConfig.timer_scavenger }, { GhostConfig.timer_scavenger = it }),
            toggle("Show money per hour", { GhostConfig.timer_moneyRate }, { GhostConfig.timer_moneyRate = it }),
            toggle("Show shard count per hour", { GhostConfig.timer_shardCount }, { GhostConfig.timer_shardCount = it }),
            toggle("Show average shards", { GhostConfig.timer_shardAverage }, { GhostConfig.timer_shardAverage = it }),
            toggle("Show gemstones per hour", { GhostConfig.timer_gemstones }, { GhostConfig.timer_gemstones = it }),
            toggle("Show session time", { GhostConfig.timer_time }, { GhostConfig.timer_time = it })
        )
        Page.COLORS -> listOf(
            cyclingButton(
                "Kill color",
                { GhostConfig.killColor.name }
            ) { GhostConfig.killColor = nextColor(GhostConfig.killColor) },
            cyclingButton(
                "Drop color",
                { GhostConfig.dropColor.name }
            ) { GhostConfig.dropColor = nextColor(GhostConfig.dropColor) },
            cyclingButton(
                "Percent difference color",
                { GhostConfig.marginColor.name }
            ) { GhostConfig.marginColor = nextColor(GhostConfig.marginColor) },
            cyclingButton(
                "Magic find color",
                { GhostConfig.mfColor.name }
            ) { GhostConfig.mfColor = nextColor(GhostConfig.mfColor) },
            cyclingButton(
                "Combat XP color",
                { GhostConfig.xpColor.name }
            ) { GhostConfig.xpColor = nextColor(GhostConfig.xpColor) },
            cyclingButton(
                "Time color",
                { GhostConfig.timeColor.name }
            ) { GhostConfig.timeColor = nextColor(GhostConfig.timeColor) },
            cyclingButton(
                "Money color",
                { GhostConfig.coinColor.name }
            ) { GhostConfig.coinColor = nextColor(GhostConfig.coinColor) },
            cyclingButton(
                "Pause indicator color",
                { GhostConfig.pauseColor.name }
            ) { GhostConfig.pauseColor = nextColor(GhostConfig.pauseColor) }
        )
    }

    private fun nextColor(current: HudColor): HudColor {
        val index = HudColor.PALETTE.indexOfFirst { it.packed == current.packed }
        return HudColor.PALETTE[(index + 1).mod(HudColor.PALETTE.size)]
    }
    //</editor-fold>

    private fun openHudEditor(hudIndex: Int) {
        GhostConfig.save()
        HudManager.save()
        minecraft?.setScreen(HudEditorScreen(this, hudIndex))
    }
}

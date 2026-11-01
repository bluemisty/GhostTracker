package me.fireandice.ghosttracker.config

import com.google.gson.JsonObject
import me.fireandice.ghosttracker.hud.GhostHud
import me.fireandice.ghosttracker.hud.HudManager
import me.fireandice.ghosttracker.hud.TimerHud
import me.fireandice.ghosttracker.utils.gson
import me.fireandice.ghosttracker.utils.logError
import java.io.File

/**
 * A simple color held by the config. Modern versions use packed argb ints when drawing text; values here
 * keep vanilla palette names so the config screen can cycle through them with readable labels.
 */
class HudColor(val name: String, val packed: Int) {
    constructor(packed: Int) : this(colorName(packed), packed)
    constructor(hex: String) : this(colorName(parseHex(hex)), parseHex(hex))

    companion object {
        const val AQUA = 0x55FFFF
        const val BLUE = 0x5555FF
        const val GOLD = 0xFFAA00
        const val RED = 0xFF5555
        const val YELLOW = 0xFFFF55
        const val GREEN = 0x55FF55
        const val LIGHT_PURPLE = 0xFF55FF
        const val WHITE = 0xFFFFFF
        const val GRAY = 0xAAAAAA
        const val DARK_GRAY = 0x555555
        const val DARK_BLUE = 0x0000AA
        const val BLACK = 0x000000

        val PALETTE: List<HudColor> = listOf(
            HudColor("aqua", AQUA),
            HudColor("blue", BLUE),
            HudColor("gold", GOLD),
            HudColor("red", RED),
            HudColor("yellow", YELLOW),
            HudColor("green", GREEN),
            HudColor("light purple", LIGHT_PURPLE),
            HudColor("white", WHITE),
            HudColor("gray", GRAY),
            HudColor("dark gray", DARK_GRAY),
            HudColor("dark blue", DARK_BLUE),
            HudColor("black", BLACK)
        )

        private fun colorName(packed: Int): String =
            PALETTE.firstOrNull { it.packed == packed }?.name ?: String.format("%06x", packed)

        private fun parseHex(hex: String): Int = hex.trim().toLong(16).toInt() and 0xFFFFFF
    }
}

/**
 * All config options. Replaced OneConfig; the field names, defaults and semantics are kept the same
 * as the old 1.8.9 mod so the HUD classes stayed untouched.
 */
object GhostConfig {

    private var file: File? = null

    //<editor-fold desc="General settings">
    var showEverywhere = false
    var showIcons = true
    var abbreviate = false
    val showPrefixes get() = !abbreviate
    var showMargins = true

    /** None / Shadow / Full shadow: the last two map onto the vanilla text drop shadow */
    var shadowOption = 1
        private set

    val shadow: Boolean
        get() = shadowOption == 1 || shadowOption == 2

    fun cycleShadowOption() {
        shadowOption = (shadowOption + 1) % 3
    }

    /** Enchant levels used for percent difference calculations */
    var lootingLevel = 5
    var luckLevel = 7

    var priceTimespanOption = 2

    val priceTimespan: String
        get() = when (priceTimespanOption) {
            0 -> "hour"
            1 -> "day"
            else -> "week"
        }

    var priceFrequency: Int = 20

    /** How the combined gemstones category is shown: 0 count, 1 coin value, 2 both */
    var gemstoneDisplayOption = 2
    //</editor-fold>

    //<editor-fold desc="Colors">
    var killColor = HudColor(HudColor.AQUA)        // aqua
    var dropColor = HudColor(HudColor.BLUE)        // blue
    var marginColor = HudColor(HudColor.DARK_GRAY) // dark gray
    var mfColor = HudColor(HudColor.GOLD)          // gold
    var xpColor = HudColor(HudColor.RED)           // red
    var timeColor = HudColor(HudColor.AQUA)        // aqua
    var coinColor = HudColor(HudColor.GOLD)        // gold
    var pauseColor = HudColor(HudColor.DARK_GRAY)  // dark gray
    //</editor-fold>

    //<editor-fold desc="Stat tracker display">
    var tracker_kills = true
    var tracker_sorrow = true
    var tracker_volta = true
    var tracker_plasma = true
    var tracker_boots = true
    var tracker_coins = true
    var tracker_mf = true
    var tracker_averageXp = true
    var tracker_totalXp = true
    var tracker_scavenger = true
    var tracker_totalMoney = true
    var tracker_shardCount = true
    var tracker_shardAverage = true
    var tracker_gemstones = true
    //</editor-fold>

    //<editor-fold desc="Session timer display">
    var timer_kills = true
    var timer_sorrow = true
    var timer_volta = true
    var timer_plasma = true
    var timer_boots = true
    var timer_coins = true
    var timer_mf = true
    var timer_averageXp = true
    var timer_xpRate = true
    var timer_scavenger = true
    var timer_moneyRate = true
    var timer_time = true
    var timer_shardCount = true
    var timer_shardAverage = true
    var timer_gemstones = true
    //</editor-fold>

    //<editor-fold desc="Huds">
    val trackerHud = GhostHud(HudManager.tracker)
    val timerHud = TimerHud(HudManager.timer)
    //</editor-fold>

    private fun file(): File {
        if (file == null) file = File(me.fireandice.ghosttracker.GhostTracker.dataDir, "GhostConfig.json")
        return file!!
    }

    fun save() {
        val json = JsonObject().apply {
            addProperty("showEverywhere", showEverywhere)
            addProperty("showIcons", showIcons)
            addProperty("abbreviate", abbreviate)
            addProperty("showMargins", showMargins)
            addProperty("shadowOption", shadowOption)
            addProperty("lootingLevel", lootingLevel)
            addProperty("luckLevel", luckLevel)
            addProperty("priceTimespanOption", priceTimespanOption)
            addProperty("priceFrequency", priceFrequency)
            addProperty("gemstoneDisplayOption", gemstoneDisplayOption)
            addProperty("killColor", String.format("%06X", killColor.packed))
            addProperty("dropColor", String.format("%06X", dropColor.packed))
            addProperty("marginColor", String.format("%06X", marginColor.packed))
            addProperty("mfColor", String.format("%06X", mfColor.packed))
            addProperty("xpColor", String.format("%06X", xpColor.packed))
            addProperty("timeColor", String.format("%06X", timeColor.packed))
            addProperty("coinColor", String.format("%06X", coinColor.packed))
            addProperty("pauseColor", String.format("%06X", pauseColor.packed))
            addProperty("tracker_kills", tracker_kills)
            addProperty("tracker_sorrow", tracker_sorrow)
            addProperty("tracker_volta", tracker_volta)
            addProperty("tracker_plasma", tracker_plasma)
            addProperty("tracker_boots", tracker_boots)
            addProperty("tracker_coins", tracker_coins)
            addProperty("tracker_mf", tracker_mf)
            addProperty("tracker_averageXp", tracker_averageXp)
            addProperty("tracker_totalXp", tracker_totalXp)
            addProperty("tracker_scavenger", tracker_scavenger)
            addProperty("tracker_totalMoney", tracker_totalMoney)
            addProperty("tracker_shardCount", tracker_shardCount)
            addProperty("tracker_shardAverage", tracker_shardAverage)
            addProperty("tracker_gemstones", tracker_gemstones)
            addProperty("timer_kills", timer_kills)
            addProperty("timer_sorrow", timer_sorrow)
            addProperty("timer_volta", timer_volta)
            addProperty("timer_plasma", timer_plasma)
            addProperty("timer_boots", timer_boots)
            addProperty("timer_coins", timer_coins)
            addProperty("timer_mf", timer_mf)
            addProperty("timer_averageXp", timer_averageXp)
            addProperty("timer_xpRate", timer_xpRate)
            addProperty("timer_scavenger", timer_scavenger)
            addProperty("timer_moneyRate", timer_moneyRate)
            addProperty("timer_time", timer_time)
            addProperty("timer_shardCount", timer_shardCount)
            addProperty("timer_shardAverage", timer_shardAverage)
            addProperty("timer_gemstones", timer_gemstones)
        }
        file().bufferedWriter().use { it.write(gson.toJson(json)) }
    }

    fun load() {
        try {
            val json: JsonObject = file().bufferedReader().use { gson.fromJson(it, JsonObject::class.java) }
            for ((key, element) in json.entrySet()) {
                try {
                    readOption(key, element.asString)
                } catch (e: Exception) {
                    logError("Config option '$key' had invalid value")
                }
            }
        } catch (_: Exception) {
            // Missing file, presumably first launch
        }
    }

    private fun readOption(key: String, value: String) {
        when (key) {
            "showEverywhere" -> showEverywhere = value.toBoolean()
            "showIcons" -> showIcons = value.toBoolean()
            "abbreviate" -> abbreviate = value.toBoolean()
            "showMargins" -> showMargins = value.toBoolean()
            "shadowOption" -> shadowOption = value.toIntOrNull()?.coerceIn(0, 2) ?: 1
            "lootingLevel" -> lootingLevel = value.toInt()
            "luckLevel" -> luckLevel = value.toInt()
            "priceTimespanOption" -> priceTimespanOption = value.toInt().coerceIn(0, 2)
            "priceFrequency" -> priceFrequency = value.toInt()
            "gemstoneDisplayOption" -> gemstoneDisplayOption = value.toInt().coerceIn(0, 2)
            "killColor" -> killColor = HudColor(value)
            "dropColor" -> dropColor = HudColor(value)
            "marginColor" -> marginColor = HudColor(value)
            "mfColor" -> mfColor = HudColor(value)
            "xpColor" -> xpColor = HudColor(value)
            "timeColor" -> timeColor = HudColor(value)
            "coinColor" -> coinColor = HudColor(value)
            "pauseColor" -> pauseColor = HudColor(value)
            "tracker_kills" -> tracker_kills = value.toBoolean()
            "tracker_sorrow" -> tracker_sorrow = value.toBoolean()
            "tracker_volta" -> tracker_volta = value.toBoolean()
            "tracker_plasma" -> tracker_plasma = value.toBoolean()
            "tracker_boots" -> tracker_boots = value.toBoolean()
            "tracker_coins" -> tracker_coins = value.toBoolean()
            "tracker_mf" -> tracker_mf = value.toBoolean()
            "tracker_averageXp" -> tracker_averageXp = value.toBoolean()
            "tracker_totalXp" -> tracker_totalXp = value.toBoolean()
            "tracker_scavenger" -> tracker_scavenger = value.toBoolean()
            "tracker_totalMoney" -> tracker_totalMoney = value.toBoolean()
            "tracker_shardCount" -> tracker_shardCount = value.toBoolean()
            "tracker_shardAverage" -> tracker_shardAverage = value.toBoolean()
            "tracker_gemstones" -> tracker_gemstones = value.toBoolean()
            "timer_kills" -> timer_kills = value.toBoolean()
            "timer_sorrow" -> timer_sorrow = value.toBoolean()
            "timer_volta" -> timer_volta = value.toBoolean()
            "timer_plasma" -> timer_plasma = value.toBoolean()
            "timer_boots" -> timer_boots = value.toBoolean()
            "timer_coins" -> timer_coins = value.toBoolean()
            "timer_mf" -> timer_mf = value.toBoolean()
            "timer_averageXp" -> timer_averageXp = value.toBoolean()
            "timer_xpRate" -> timer_xpRate = value.toBoolean()
            "timer_scavenger" -> timer_scavenger = value.toBoolean()
            "timer_moneyRate" -> timer_moneyRate = value.toBoolean()
            "timer_time" -> timer_time = value.toBoolean()
            "timer_shardCount" -> timer_shardCount = value.toBoolean()
            "timer_shardAverage" -> timer_shardAverage = value.toBoolean()
            "timer_gemstones" -> timer_gemstones = value.toBoolean()
        }
    }
}

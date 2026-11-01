package me.fireandice.ghosttracker.tracker

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import me.fireandice.ghosttracker.api.PriceData
import me.fireandice.ghosttracker.config.GhostConfig
import me.fireandice.ghosttracker.tracker.GhostDrops.*
import me.fireandice.ghosttracker.utils.logError
import java.text.DecimalFormat

class GhostStats {

    // for json parsing
    private val stats: MutableMap<String, Number> = mutableMapOf(
        "sorrowCount" to 0,
        "voltaCount" to 0,
        "plasmaCount" to 0,
        "bootsCount" to 0,
        "coinsCount" to 0,
        "kills" to 0,
        "totalMf" to 0,
        "mfDropCount" to 0,
        "totalXp" to 0f,
        "scavenger" to 0,
        "shardCount" to 0,
        "charmCount" to 0,
        "roughJadeCount" to 0,
        "flawedJadeCount" to 0,
        "roughAmberCount" to 0,
        "flawedAmberCount" to 0,
        "roughSapphireCount" to 0,
        "flawedSapphireCount" to 0,
        "roughTopazCount" to 0,
        "flawedTopazCount" to 0,
        "roughAmethystCount" to 0,
        "flawedAmethystCount" to 0
    )

    var sorrowCount: Int by stats
    var voltaCount: Int by stats
    var plasmaCount: Int by stats
    var bootsCount: Int by stats
    var coinsCount: Int by stats
    var kills: Int by stats
    var totalMf: Int by stats
    var mfDropCount: Int by stats
    var totalXp: Float by stats
    var scavenger: Int by stats

    var shardCount: Int by stats
    var charmCount: Int by stats

    var roughJadeCount: Int by stats
    var flawedJadeCount: Int by stats
    var roughAmberCount: Int by stats
    var flawedAmberCount: Int by stats
    var roughSapphireCount: Int by stats
    var flawedSapphireCount: Int by stats
    var roughTopazCount: Int by stats
    var flawedTopazCount: Int by stats
    var roughAmethystCount: Int by stats
    var flawedAmethystCount: Int by stats

    val totalValue: Int
        get() = (sorrowCount * PriceData.sorrowPrice +
                voltaCount * PriceData.voltaPrice +
                plasmaCount * PriceData.plasmaPrice +
                bootsCount * PriceData.bootsPrice +
                coinsCount * 1_000_000 +
                scavenger).toInt()

    fun addGem(drop: GhostDrops) {
        when (drop) {
            RoughJade -> roughJadeCount++
            FlawedJade -> flawedJadeCount++
            RoughAmber -> roughAmberCount++
            FlawedAmber -> flawedAmberCount++
            RoughSapphire -> roughSapphireCount++
            FlawedSapphire -> flawedSapphireCount++
            RoughTopaz -> roughTopazCount++
            FlawedTopaz -> flawedTopazCount++
            RoughAmethyst -> roughAmethystCount++
            FlawedAmethyst -> flawedAmethystCount++
            else -> {}
        }
    }

    /** Combined count of all ten gemstone drops */
    val gemstoneCount: Int
        get() = roughJadeCount + flawedJadeCount +
                roughAmberCount + flawedAmberCount +
                roughSapphireCount + flawedSapphireCount +
                roughTopazCount + flawedTopazCount +
                roughAmethystCount + flawedAmethystCount

    /** Combined coin value of all ten gemstone drops, at bazaar prices */
    val gemstoneValue: Float
        get() = roughJadeCount * PriceData.roughJadePrice + flawedJadeCount * PriceData.flawedJadePrice +
                roughAmberCount * PriceData.roughAmberPrice + flawedAmberCount * PriceData.flawedAmberPrice +
                roughSapphireCount * PriceData.roughSapphirePrice + flawedSapphireCount * PriceData.flawedSapphirePrice +
                roughTopazCount * PriceData.roughTopazPrice + flawedTopazCount * PriceData.flawedTopazPrice +
                roughAmethystCount * PriceData.roughAmethystPrice + flawedAmethystCount * PriceData.flawedAmethystPrice

    /**
     * The combined gemstones category display: 0 shows only the count, 1 the coin value, 2 both.
     */
    fun getGemstoneDisplay(displayOption: Int, format: DecimalFormat): String = when (displayOption) {
        0 -> format.format(gemstoneCount)
        1 -> format.format(gemstoneValue)
        else -> "${format.format(gemstoneCount)} (${format.format(gemstoneValue)})"
    }

    fun getAverageShards(format: DecimalFormat): String {
        if (charmCount == 0) return "-"
        return format.format(shardCount.toFloat() / charmCount)
    }

    private fun getAverageMf(): Float? {
        if (mfDropCount > 0) return totalMf.toFloat() / mfDropCount
        return null
    }

    fun getAverageMf(format: DecimalFormat): String {
        val mf = getAverageMf() ?: return "-"
        return format.format(mf)
    }

    fun getAverageXp(format: DecimalFormat): String {
        if (kills == 0) return "-"
        return format.format(totalXp / kills)
    }

    private fun getRelativeDifference(drop: GhostDrops): Float? {
        if (kills == 0) return null
        var chanceModifier = 1f

        val actual = when (drop) {
            Sorrow -> {
                chanceModifier += (getAverageMf() ?: 0f) / 100
                chanceModifier += GhostConfig.lootingLevel.toFloat() * 0.15f
                sorrowCount
            }
            Volta -> {
                chanceModifier += (getAverageMf() ?: 0f) / 100
                chanceModifier += GhostConfig.lootingLevel.toFloat() * 0.15f
                voltaCount
            }
            Plasma -> {
                chanceModifier += (getAverageMf() ?: 0f) / 100
                chanceModifier += GhostConfig.lootingLevel.toFloat() * 0.15f
                plasmaCount
            }
            Boots -> {
                chanceModifier += (getAverageMf() ?: 0f) / 100
                chanceModifier += GhostConfig.luckLevel.toFloat() * 0.05f
                bootsCount
            }
            Coins -> coinsCount
            else -> return null    // gemstones are excluded from the comparisons, they have no known base chance
        }
        if (actual == 0) return null    // this would display "-100.00%" which I don't really want

        val theoretical = kills * drop.baseChance * chanceModifier

        return (actual - theoretical) / theoretical
    }

    fun getPercentDifference(drop: GhostDrops, format: DecimalFormat): String {
        val diff = getRelativeDifference(drop) ?: return ""
        val percentString = format.format(diff * 100)
        if (diff >= 0) return " (+$percentString%)"
        return " ($percentString%)"    // it already puts the - sign there if it's negative
    }

    fun reset() {
        sorrowCount = 0
        voltaCount = 0
        plasmaCount = 0
        bootsCount = 0
        coinsCount = 0
        kills = 0
        totalMf = 0
        mfDropCount = 0
        totalXp = 0f
        scavenger = 0
        shardCount = 0
        charmCount = 0
        roughJadeCount = 0
        flawedJadeCount = 0
        roughAmberCount = 0
        flawedAmberCount = 0
        roughSapphireCount = 0
        flawedSapphireCount = 0
        roughTopazCount = 0
        flawedTopazCount = 0
        roughAmethystCount = 0
        flawedAmethystCount = 0
    }

    fun toJson() = JsonObject().apply { for (stat in stats) add(stat.key, JsonPrimitive(stat.value)) }

    fun fromJson(json: JsonObject) {
        for (stat in stats) {
            val jsonElement = json[stat.key] ?: continue
            try {
                // restore each stat in its own type, ints stored as floats would crash increments later
                stats[stat.key] = if (stat.value is Float) jsonElement.asFloat else jsonElement.asInt
            } catch (e: ClassCastException) {
                logError("${stat.key} couldn't be cast to the tracked type")
            } catch (e: Exception) {
                e.message?.let { logError(it) }
            }
        }
    }
}

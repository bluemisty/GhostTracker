package me.fireandice.ghosttracker.tracker

import me.fireandice.ghosttracker.GhostTracker
import me.fireandice.ghosttracker.tracker.GhostDrops.*
import me.fireandice.ghosttracker.utils.ScoreboardUtils
import me.fireandice.ghosttracker.utils.inGhostArea
import me.fireandice.ghosttracker.utils.stripControlCodes
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt

object GhostListener {

    // Patterns now match plain text (formatting codes are stripped before matching), which is
    // independent of how the servers colorizes the messages.
    private val RARE_DROP_PATTERN = "RARE DROP! (?<drop>[A-Za-z ]+) \\((?<mf>\\d+)% [✯*] Magic Find\\)".toPattern()
    private const val COIN_DROP_MESSAGE = "The ghost's death materialized 1,000,000 coins from the mists!"
    private val COMBAT_XP_PATTERN = "\\+(?<gained>[\\d.]+) Combat \\((?<progress>.+)\\)".toPattern()

    /**
     * Shards are received from charming the ghost; the trailing number in the message is the profile total,
     * which is not tracked. Individual charms give 1-3 shards and Hunter's Fortune can give more, so the
     * per-charm average shows the intended stat.
     */
    private val SHARD_PATTERN = "CHARM! You charmed the Ghost and received (?<shards>\\d+) Ghost Shards!(\\s+\\(\\d+\\))?".toPattern()
    private val numberFormat: NumberFormat = NumberFormat.getInstance(Locale.US)
    private var previousXp = -1f

    /**
     * Called in `EventListener.onChat()` for every chat/system message.
     */
    fun onChat(message: String) {
        if (!ScoreboardUtils.inDwarvenMines || !inGhostArea) return

        val plain = message.stripControlCodes()

        // Detecting if 1m coins dropped
        if (COIN_DROP_MESSAGE == plain) {
            GhostTracker.ghostStats.coinsCount++
            if (GhostTimer.isTracking) GhostTimer.stats.coinsCount++
            return
        }

        // Detecting ghost shard charms
        val shardMatcher = SHARD_PATTERN.matcher(plain)
        if (shardMatcher.matches()) {
            trackShards(shardMatcher.group("shards").toInt())
            return
        }

        // Detecting one of the various normal rng drops
        val matcher = RARE_DROP_PATTERN.matcher(plain)

        if (matcher.matches()) {
            val drop = GhostDrops.get(matcher.group("drop").trim()) ?: return
            val mf = numberFormat.parse(matcher.group("mf")).toInt()
            trackDrops(drop, mf)
        }
    }

    /**
     * Some logic was taken from https://www.chattriggers.com/modules/v/GhostCounterV3.
     * Called in `EventListener.onChat()` for every action bar (game overlay) message.
     */
    fun onActionBar(message: String) {
        val plain = message.stripControlCodes()
        val matcher = COMBAT_XP_PATTERN.matcher(plain)

        if (matcher.find()) {
            val xpGained = numberFormat.parse(matcher.group("gained")).toFloat()

            var progress: String = matcher.group("progress")
            if (progress.endsWith("/0")) progress = progress.substring(0, progress.length - 2)

            val newXp = numberFormat.parse(progress).toFloat()

            // avoids tracking non ghost kills but still saves the xp value
            if (!inGhostArea) {
                previousXp = newXp
                return
            }

            // if there are no previously tracked kills
            if (previousXp == -1f) trackKills(1, xpGained)
            else {
                val actualXpGained = newXp - previousXp
                // if you gain a bestiary level and gain 1m xp it might count all of those as kills, hence the
                // coerceAtMost (15 was an arbitrary choice)
                val killsGained = (actualXpGained / xpGained).roundToInt().coerceAtMost(15)

                // xpGained * killsGained is more accurate and avoids rounding error
                if (previousXp != 0f && killsGained >= 0) trackKills(killsGained, xpGained * killsGained)
            }
            previousXp = newXp
        }
    }

    private fun trackDrops(drop: GhostDrops, magicFind: Int) {
        val ghostStats = GhostTracker.ghostStats
        val timerStats = GhostTimer.stats

        // the gemstone drops are only counted per type, they don't use the magic find statistics
        if (drop.isGemstone) {
            ghostStats.addGem(drop)
            if (GhostTimer.isTracking) timerStats.addGem(drop)
            return
        }

        // coin drops are already handled before this method is called
        when (drop) {
            Sorrow -> ghostStats.sorrowCount++
            Volta -> ghostStats.voltaCount++
            Plasma -> ghostStats.plasmaCount++
            Boots -> ghostStats.bootsCount++
            else -> {}
        }
        ghostStats.totalMf += magicFind
        ghostStats.mfDropCount++

        if (GhostTimer.isTracking) {
            when (drop) {
                Sorrow -> timerStats.sorrowCount++
                Volta -> timerStats.voltaCount++
                Plasma -> timerStats.plasmaCount++
                Boots -> timerStats.bootsCount++
                else -> {}
            }
            timerStats.totalMf += magicFind
            timerStats.mfDropCount++
        }
    }

    private fun trackKills(killsGained: Int, xpGained: Float) {
        GhostTracker.ghostStats.kills += killsGained
        GhostTracker.ghostStats.totalXp += xpGained

        if (GhostTimer.isTracking) {
            GhostTimer.stats.kills += killsGained
            GhostTimer.stats.totalXp += xpGained
        }
    }

    private fun trackShards(shards: Int) {
        GhostTracker.ghostStats.shardCount += shards
        GhostTracker.ghostStats.charmCount++

        if (GhostTimer.isTracking) {
            GhostTimer.stats.shardCount += shards
            GhostTimer.stats.charmCount++
        }
    }
}

package me.fireandice.ghosttracker.utils

import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerScoreEntry

/**
 * Modified from https://github.com/inglettronald/DulkirMod under the GNU Affero General Public License v3.0
 */
object ScoreboardUtils {

    /**
     * Only updates after [checkLocations] is called
     */
    var inSkyblock = false

    /**
     * Only updates after [checkLocations] is called
     */
    var inDwarvenMines = false

    private val locations: Array<String> = arrayOf(
        "Dwarven Village", "Miner's Guild", "Palace Bridge", "Royal Palace", "Puzzler", "Grand Library",
        "Barracks of Heroes", "Royal Mines", "Cliffside Veins", "Forge Basin", "The Forge", "Rampart's Quarry",
        "Far Reserve", "Upper Mines", "Goblin Burrows", "Great Ice Wall", "Aristocrat Passage", "Hanging Court",
        "Divan's Gateway", "Lava Springs", "The Mist", "Dwarven Mines", "Gates to the Mines", "The Lift"
    )

    /**
     * The sidebar lines, top to bottom, with formatting codes still in them.
     */
    private fun getLines(): List<String> {
        val level = Minecraft.getInstance().level ?: return emptyList()
        val scoreboard = level.scoreboard
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        val entries: List<PlayerScoreEntry> = scoreboard.listPlayerScores(objective).toList()
        return entries.sortedByDescending { it.value() }.map { it.display()?.getString() ?: "" }
    }

    /**
     * Called in `EventListener.onTickStart()`
     */
    private fun inSkyblock(): Boolean {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return false
        val server = mc.currentServer ?: return false          // null on the main menu or in singleplayer
        if (mc.isSingleplayer) return false
        if (!server.ip.contains("hypixel", ignoreCase = true)) return false
        val objective = level.scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return false
        val title = objective.displayName?.getString() ?: return false
        return title.stripControlCodes().contains("skyblock", ignoreCase = true)
    }

    /**
     * Called in `EventListener.onTickStart()`
     */
    fun checkLocations() {
        inSkyblock = inSkyblock()
        if (!inSkyblock) {
            inDwarvenMines = false
            return
        }

        val lines = getLines()

        if (lines.size < 5) {
            inDwarvenMines = false
            return
        }

        // The location is displayed on the fifth sidebar line. The extra loop is a robustness fallback in case
        // hypixel changes the sidebar layout: the location names are unique so scanning the rest is safe.
        val line = lines.getOrNull(4)?.stripControlCodes()
        if (line != null && locations.any { it in line }) {
            inDwarvenMines = true
            return
        }
        for (line in lines) {
            val stripped = line.stripControlCodes()
            if (locations.any { it in stripped }) {
                inDwarvenMines = true
                return
            }
        }
    }

    /**
     * Returns a string of coins in purse. Includes the scavenger coin gain if present. Removes any characters that
     * aren't numbers or spaces.
     */
    fun getPurse(): String? {
        if (!inSkyblock) return null

        val lines = getLines()
        for (line in lines) if (line.contains("Purse") || line.contains("Piggy")) {
            return line.stripControlCodes().filter {
                it.isDigit() || it == ' '
            }.trim()
        }

        return null
    }
}

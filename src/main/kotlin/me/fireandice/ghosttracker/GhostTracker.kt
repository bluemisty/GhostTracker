package me.fireandice.ghosttracker

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import me.fireandice.ghosttracker.tracker.GhostStats
import me.fireandice.ghosttracker.tracker.GhostTimer
import me.fireandice.ghosttracker.utils.gson
import me.fireandice.ghosttracker.utils.logInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Holds the mod-wide constants and the stats of the total (non-timed) tracker.
 * The migration from the old 1.8.9-forge mod also kept `GhostTracker` as the
 * central object that saves/loads persisted data.
 */
object GhostTracker {

    const val MODID = "ghosttracker"
    const val NAME = "GhostTracker"
    const val VERSION = "2.0.4"
    const val PREFIX = "§b§lGhostTracker§r§8 »§r"

    val ghostStats = GhostStats()
    val logger: Logger = LoggerFactory.getLogger("GhostTracker")

    private lateinit var statsFile: File

    /**
     * The directory all of this mod's persisted data lives in, like the old
     * `.minecraft/config/GhostTracker` folder.
     */
    lateinit var dataDir: File
        private set

    /**
     * Called by [GhostTrackerClient] on client init, before any events fire.
     */
    fun init(configDir: File) {
        dataDir = File(configDir, "GhostTracker")
        dataDir.mkdirs()
        statsFile = File(dataDir, "GhostStats.json")

        if (statsFile.createNewFile()) this.save()
        else this.load()

        if (GhostTimer.file().createNewFile()) GhostTimer.save()
        else GhostTimer.load()
    }

    fun resetStats(message: Boolean = true) {
        ghostStats.reset()
        if (message) sendResetMessage()
    }

    private fun sendResetMessage() {
        ChatHelper.send("$PREFIX §cMain tracker reset")
    }

    fun save() {
        val jsonString = gson.toJson(ghostStats.toJson())
        statsFile.bufferedWriter().use { it.write(jsonString) }
        logInfo("Tracker stats saved")
    }

    private fun load() {
        try {
            val jsonString = statsFile.bufferedReader().use { it.readText() }
            gson.fromJson(jsonString, JsonObject::class.java).also { ghostStats.fromJson(it) }
        } catch (_: Exception) {
        }
        logInfo("Tracker stats loaded")
    }
}

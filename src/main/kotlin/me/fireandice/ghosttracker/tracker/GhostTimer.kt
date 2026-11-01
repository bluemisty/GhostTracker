package me.fireandice.ghosttracker.tracker

import com.google.gson.JsonPrimitive
import me.fireandice.ghosttracker.ChatHelper
import me.fireandice.ghosttracker.GhostTracker
import me.fireandice.ghosttracker.utils.gson
import me.fireandice.ghosttracker.utils.logError
import me.fireandice.ghosttracker.utils.logInfo
import java.io.File

object GhostTimer {

    private var startTime = -1L     // stores the time of the last start or unpause
    private var totalTime = 0L      // stores elapsed time up to the last pause
    val elapsedTime: Long           // "stores" the real elapsed time of the timed session
        get() = totalTime + if (isTracking) System.currentTimeMillis() - startTime else 0

    var isTracking = false
    val isPaused
        get() = !isTracking && totalTime != 0L

    var stats = GhostStats()
    private var file: File? = null

    fun file(): File {
        if (file == null) file = File(GhostTracker.dataDir, "GhostTimer.json")
        return file!!
    }

    fun start(message: Boolean = true) {
        if (isTracking) {
            if (message) ChatHelper.send("${GhostTracker.PREFIX} §aSession timer is already running")
            return
        }
        startTime = System.currentTimeMillis()
        isTracking = true
        if (message) ChatHelper.send("${GhostTracker.PREFIX} §aSession timer started")
    }

    fun pause(message: Boolean = true) {
        if (!isTracking) {
            if (message) ChatHelper.send("${GhostTracker.PREFIX} §eSession timer is already inactive")
            return
        }
        totalTime += System.currentTimeMillis() - startTime
        isTracking = false
        if (message) ChatHelper.send("${GhostTracker.PREFIX} §eSession timer paused")
    }

    fun reset(message: Boolean = true) {
        isTracking = false
        stats.reset()
        startTime = -1L
        totalTime = 0L
        if (message) ChatHelper.send("${GhostTracker.PREFIX} §cSession timer reset")
    }

    fun save() {
        val jsonObj = stats.toJson().apply { add("time", JsonPrimitive(elapsedTime)) }
        val jsonString = gson.toJson(jsonObj)
        file().bufferedWriter().use { it.write(jsonString) }
        logInfo("Timer stats saved")
    }

    fun load() {
        pause(false)
        try {
            val jsonString = file().bufferedReader().use { it.readText() }
            val jsonObject: com.google.gson.JsonObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)

            stats.fromJson(jsonObject)
            totalTime = jsonObject["time"]?.asLong ?: 0L
        } catch (e: Exception) {
            if (e is com.google.gson.JsonSyntaxException) logError("Couldn't parse timer stats file")
            else if (e is ClassCastException) logError("Time couldn't be cast to long")
            else e.message?.let { logError(it) }
        }

        logInfo("Timer stats loaded")
    }
}

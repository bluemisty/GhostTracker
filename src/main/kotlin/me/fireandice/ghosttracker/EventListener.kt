package me.fireandice.ghosttracker

import me.fireandice.ghosttracker.api.PriceData
import me.fireandice.ghosttracker.config.GhostConfig
import me.fireandice.ghosttracker.tracker.GhostListener
import me.fireandice.ghosttracker.tracker.GhostTimer
import me.fireandice.ghosttracker.tracker.PurseListener
import me.fireandice.ghosttracker.utils.ScoreboardUtils
import net.minecraft.network.chat.Component

/**
 * All per-tick/per-message handling is collected here. The actual Minecraft event registration
 * lives in `platform.Platform` so it is easy to isolate when upgrading to a newer Minecraft version.
 */
object EventListener {

    fun onTickStart() {
        ScoreboardUtils.checkLocations()
        PurseListener.onTick()
        everyFiveMinutes()
        GhostConfig.trackerHud.refreshLines()
        GhostConfig.timerHud.refreshLines()

        if (ScoreboardUtils.inSkyblock) {
            PriceData.fetchPrices()
        }
    }

    private var lastFiveMinuteUpdate = -1L
    private fun everyFiveMinutes() {
        if (lastFiveMinuteUpdate != -1L && System.currentTimeMillis() - lastFiveMinuteUpdate <= 300_000) return

        GhostTracker.save()
        lastFiveMinuteUpdate = System.currentTimeMillis()
    }

    fun onWorldUnload() = GhostTimer.pause(false)

    /**
     * `isActionBar` is true for action bar (overlay) messages.
     */
    fun onChat(text: Component, isActionBar: Boolean) {
        if (isActionBar) {
            GhostListener.onActionBar(text.getString())
        } else {
            GhostListener.onChat(text.getString())
        }
    }
}

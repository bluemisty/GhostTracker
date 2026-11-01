package me.fireandice.ghosttracker

import me.fireandice.ghosttracker.config.GhostConfig
import me.fireandice.ghosttracker.hud.HudManager
import me.fireandice.ghosttracker.platform.Platform
import me.fireandice.ghosttracker.tracker.GhostTimer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader

object GhostTrackerClient : ClientModInitializer {

    override fun onInitializeClient() {
        GhostTracker.init(FabricLoader.getInstance().configDir.toFile())

        GhostConfig.load()
        HudManager.load()
        Platform.register()

        Runtime.getRuntime().addShutdownHook(Thread {
            GhostTracker.save()
            GhostTimer.save()
            GhostConfig.save()
            HudManager.save()
        })
    }
}

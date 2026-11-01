package me.fireandice.ghosttracker.platform

import me.fireandice.ghosttracker.GhostTracker
import me.fireandice.ghosttracker.EventListener
import me.fireandice.ghosttracker.config.GhostConfig
import me.fireandice.ghosttracker.config.GhostConfigScreen
import me.fireandice.ghosttracker.tracker.GhostTimer
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

/**
 * Everything that is bound to this specific Minecraft version lives here so updating the mod
 * to a newer version means porting accurately this file (and re-verifying Utils/ScoreboardUtils).
 */
object Platform {

    /**
     * Keybindings for the tracked hud actions. They can be rebound anywhere in the vanilla controls menu
     * (they show under the GhostTracker section).
     */
    lateinit var trackerResetKb: KeyMapping
        private set
    lateinit var timerResetKb: KeyMapping
        private set
    lateinit var timerStartPauseKb: KeyMapping
        private set

    private val hudElementIdTracker = Identifier.fromNamespaceAndPath(GhostTracker.MODID, "tracker_hud")
    private val hudElementIdTimer = Identifier.fromNamespaceAndPath(GhostTracker.MODID, "timer_hud")

    fun register() {
        registerEvents()
        registerCommands()
        registerKeyBindings()
        registerHud()
    }

    private fun registerEvents() {
        ClientTickEvents.START_CLIENT_TICK.register {
            EventListener.onTickStart()
            checkKeyBindings()
        }
        ClientReceiveMessageEvents.GAME.register { text, isActionBar -> EventListener.onChat(text, isActionBar) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> EventListener.onWorldUnload() }
    }

    private fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("ghost").executes { _ ->
                    openConfig()
                    1
                }
                    .then(ClientCommands.literal("start").executes { _ -> GhostTimer.start(); 1 })
                    .then(ClientCommands.literal("pause").executes { _ -> GhostTimer.pause(); 1 })
                    .then(ClientCommands.literal("stop").executes { _ -> GhostTimer.pause(); 1 })
                    .then(ClientCommands.literal("reset").executes { _ -> GhostTimer.reset(); 1 })
                    .then(ClientCommands.literal("clear").executes { _ -> GhostTimer.reset(); 1 })
                    .then(
                        ClientCommands.literal("stats")
                            .then(ClientCommands.literal("reset").executes { _ -> GhostTracker.resetStats(); 1 })
                            .then(ClientCommands.literal("clear").executes { _ -> GhostTracker.resetStats(); 1 })
                    )
            )
        }
    }

    private fun registerKeyBindings() {
        val category = KeyMapping.Category.MISC
        trackerResetKb = keybinding("Reset stat tracker", GLFW.GLFW_KEY_UNKNOWN, category)
        timerResetKb = keybinding("Reset session timer", GLFW.GLFW_KEY_UNKNOWN, category)
        timerStartPauseKb = keybinding("Start/pause session timer", GLFW.GLFW_KEY_UNKNOWN, category)
        KeyMappingHelper.registerKeyMapping(trackerResetKb)
        KeyMappingHelper.registerKeyMapping(timerResetKb)
        KeyMappingHelper.registerKeyMapping(timerStartPauseKb)
    }

    private fun keybinding(desc: String, key: Int, category: KeyMapping.Category): KeyMapping =
        KeyMapping("GhostTracker: $desc", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, key, category)

    private fun checkKeyBindings() {
        while (trackerResetKb.consumeClick()) GhostTracker.resetStats()
        while (timerResetKb.consumeClick()) GhostTimer.reset()
        while (timerStartPauseKb.consumeClick()) {
            if (GhostTimer.isTracking) GhostTimer.pause()
            else GhostTimer.start()
        }
    }

    private fun registerHud() {
        HudElementRegistry.addLast(hudElementIdTracker, HudElement { graphics, _ -> GhostConfig.trackerHud.render(graphics) })
        HudElementRegistry.addLast(hudElementIdTimer, HudElement { graphics, _ -> GhostConfig.timerHud.render(graphics) })
    }

    fun openConfig() {
        val mc = Minecraft.getInstance()
        mc.execute { mc.setScreen(GhostConfigScreen()) }
    }
}

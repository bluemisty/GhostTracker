package me.fireandice.ghosttracker

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/**
 * Small helpers for sending chat messages. Kept separate so the version specific
 * Minecraft API calls stay in one place (see `platform`/utils policies).
 */
object ChatHelper {

    fun send(message: String) {
        Minecraft.getInstance().gui.chat.addClientSystemMessage(Component.literal(message))
    }
}

package me.fireandice.ghosttracker.hud

import com.google.gson.JsonObject
import me.fireandice.ghosttracker.GhostTracker
import me.fireandice.ghosttracker.utils.gson
import me.fireandice.ghosttracker.utils.logError
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.io.File

/**
 * Common surface for the huds so the editor screen can move/resize either of them without caring
 * about their line contents.
 */
interface HudInstance {
    fun drawExample(graphics: GuiGraphicsExtractor)
    val width: Float
    val height: Float
}

/**
 * Position of one hud on the screen, in unscaled gui coordinates (like what old OneConfig used).
 */
class HudPosition(var x: Int, var y: Int, var scale: Float = 1f)

/**
 * Owns the positions of both huds and persists them. OneConfig used to store them, so the mod keeps
 * its own file now.
 */
object HudManager {

    val tracker = HudPosition(100, 100)
    val timer = HudPosition(100, 120)

    private val hudSlots = listOf(HudSlot("tracker", tracker), HudSlot("timer", timer))

    private class HudSlot(val id: String, val position: HudPosition)

    private var file: File? = null

    fun file(): File {
        if (file == null) file = File(GhostTracker.dataDir, "HudPositions.json")
        return file!!
    }

    fun save() {
        val json = JsonObject().apply {
            for (slot in hudSlots) {
                add(slot.id, JsonObject().apply {
                    addProperty("x", slot.position.x)
                    addProperty("y", slot.position.y)
                    addProperty("scale", slot.position.scale)
                })
            }
        }
        file().bufferedWriter().use { it.write(gson.toJson(json)) }
    }

    fun load() {
        try {
            val json: JsonObject = file().bufferedReader().use { gson.fromJson(it, JsonObject::class.java) }
            for (slot in hudSlots) {
                val element = json.get(slot.id) as? JsonObject ?: continue
                slot.position.x = element.get("x")?.asInt ?: slot.position.x
                slot.position.y = element.get("y")?.asInt ?: slot.position.y
                slot.position.scale = element.get("scale")?.asFloat ?: slot.position.scale
            }
        } catch (e: Exception) {
            logError("Couldn't read hud positions file: ${e.message}")
        }
    }
}

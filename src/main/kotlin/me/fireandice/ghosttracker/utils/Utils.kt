package me.fireandice.ghosttracker.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.fireandice.ghosttracker.GhostTracker
import net.minecraft.client.Minecraft

val gson: Gson = GsonBuilder().setPrettyPrinting().create()

/**
 * Since we get plain text components on modern versions (not formatted ones) the only codes that can appear here
 * come from the scoreboard team prefixes, which still nest § codes.
 */
val controlCodePattern = "§.".toRegex()
fun String.stripControlCodes(): String = controlCodePattern.replace(this, "")

/**
 * Creates a new [StringBuilder], passes it as the block receiver, and returns it as a [String]
 * @return The result of `StringBuilder::toString()`
 */
inline fun stringBuilder(block: StringBuilder.() -> Unit): String {
    val builder = StringBuilder()
    builder.block()
    return builder.toString()
}

/**
 * Mainly for use in for-loops, to iterate backwards without mutating the list
 */
fun <T> Iterable<T>.reverseIterator(): Iterator<T> {
    val list = this.toList()

    return object : Iterator<T> {
        private var current: Int = list.size
        override fun hasNext() = current > 0
        override fun next() = list[--current]
    }
}

val inGhostArea: Boolean
    get() = ScoreboardUtils.inDwarvenMines && (Minecraft.getInstance().player?.blockY ?: 999) <= 100

const val FONT_HEIGHT = 9    // modern versions use a fixed 9px gui font height

fun logError(message: String) = GhostTracker.logger.error(message)
fun logInfo(message: String) = GhostTracker.logger.info(message)

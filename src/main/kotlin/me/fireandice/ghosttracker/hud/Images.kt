package me.fireandice.ghosttracker.hud

import me.fireandice.ghosttracker.GhostTracker
import net.minecraft.resources.Identifier

object Images {

    fun id(name: String): Identifier = Identifier.fromNamespaceAndPath(GhostTracker.MODID, name)

    val Kills = id("kills.png")
    val Sorrow = id("sorrow.png")
    val Volta = id("volta.png")
    val Plasma = id("plasma.png")
    val Boots = id("ghostly_boots.png")
    val Coins = id("coin_drop.png")
    val MagicFind = id("magic_find.png")
    val CombatXp = id("combat.png")
    val Time = id("time.png")
    val Money = id("money.png")
    val Shards = id("shards.png")
    val Gemstones = id("gemstones.png")
}

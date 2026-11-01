package me.fireandice.ghosttracker.tracker

enum class GhostDrops(val baseChance: Float, val isGemstone: Boolean = false) {
    Sorrow(0.0012f), Volta(0.002f), Plasma(0.001f), Boots(0.0001f), Coins(0.0001f),
    RoughJade(0f, true), FlawedJade(0f, true),
    RoughAmber(0f, true), FlawedAmber(0f, true),
    RoughSapphire(0f, true), FlawedSapphire(0f, true),
    RoughTopaz(0f, true), FlawedTopaz(0f, true),
    RoughAmethyst(0f, true), FlawedAmethyst(0f, true);

    companion object {
        fun get(name: String): GhostDrops? {
            // 'Coins' is excluded from this because it has no formal name, so this method isn't useful for it
            // the gemstone drops always end in "Gemstone", the rest is the clean name
            return when (name.removeSuffix("Gemstone").trim()) {
                "Sorrow" -> Sorrow
                "Volta" -> Volta
                "Plasma" -> Plasma
                "Ghostly Boots" -> Boots
                "Rough Jade" -> RoughJade
                "Flawed Jade" -> FlawedJade
                "Rough Amber" -> RoughAmber
                "Flawed Amber" -> FlawedAmber
                "Rough Sapphire" -> RoughSapphire
                "Flawed Sapphire" -> FlawedSapphire
                "Rough Topaz" -> RoughTopaz
                "Flawed Topaz" -> FlawedTopaz
                "Rough Amethyst" -> RoughAmethyst
                "Flawed Amethyst" -> FlawedAmethyst
                else -> null
            }
        }
    }
}

package me.fireandice.ghosttracker.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import me.fireandice.ghosttracker.GhostTracker
import me.fireandice.ghosttracker.utils.gson
import me.fireandice.ghosttracker.config.GhostConfig
import me.fireandice.ghosttracker.utils.logError
import me.fireandice.ghosttracker.utils.logInfo
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object PriceData {

    private const val USER_AGENT: String = "GhostTracker/${GhostTracker.VERSION}"
    // example url: https://sky.coflnet.com/api/bazaar/SORROW/history/day
    private const val BASE_URL: String = "https://sky.coflnet.com/api/bazaar"
    private const val TIMEOUT = 5_000
    private const val PRICE_KEY = "buy"

    /**
     * One fetchable price. Bazaar prices are used when the api responds, otherwise the npc price
     * (a lower bound approximation) is used.
     */
    private class TrackedPrice(val itemId: String, val npc: Float) {
        var price: Float = npc
    }

    private val trackedPrices = listOf(
        TrackedPrice("SORROW", 13f),
        TrackedPrice("VOLTA", 1_111f),
        TrackedPrice("PLASMA", 20_000f),
        // gemstone drops; the npc prices are approximations and only a local fallback until the bazaar prices arrive
        TrackedPrice("ROUGH_JADE_GEM", 1f),
        TrackedPrice("FLAWED_JADE_GEM", 4f),
        TrackedPrice("ROUGH_AMBER_GEM", 1f),
        TrackedPrice("FLAWED_AMBER_GEM", 4f),
        TrackedPrice("ROUGH_SAPPHIRE_GEM", 1f),
        TrackedPrice("FLAWED_SAPPHIRE_GEM", 4f),
        TrackedPrice("ROUGH_TOPAZ_GEM", 1f),
        TrackedPrice("FLAWED_TOPAZ_GEM", 4f),
        TrackedPrice("ROUGH_AMETHYST_GEM", 1f),
        TrackedPrice("FLAWED_AMETHYST_GEM", 4f)
    )

    @Suppress("ConstPropertyName") // I want the name to be consistent with the others
    const val bootsPrice = 77_777f

    private fun priceOf(itemId: String): Float = trackedPrices.first { it.itemId == itemId }.price

    val sorrowPrice: Float get() = priceOf("SORROW")
    val voltaPrice: Float get() = priceOf("VOLTA")
    val plasmaPrice: Float get() = priceOf("PLASMA")
    val roughJadePrice: Float get() = priceOf("ROUGH_JADE_GEM")
    val flawedJadePrice: Float get() = priceOf("FLAWED_JADE_GEM")
    val roughAmberPrice: Float get() = priceOf("ROUGH_AMBER_GEM")
    val flawedAmberPrice: Float get() = priceOf("FLAWED_AMBER_GEM")
    val roughSapphirePrice: Float get() = priceOf("ROUGH_SAPPHIRE_GEM")
    val flawedSapphirePrice: Float get() = priceOf("FLAWED_SAPPHIRE_GEM")
    val roughTopazPrice: Float get() = priceOf("ROUGH_TOPAZ_GEM")
    val flawedTopazPrice: Float get() = priceOf("FLAWED_TOPAZ_GEM")
    val roughAmethystPrice: Float get() = priceOf("ROUGH_AMETHYST_GEM")
    val flawedAmethystPrice: Float get() = priceOf("FLAWED_AMETHYST_GEM")

    private var lastFetch: Long = -1L

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIMEOUT.toLong()))
        .build()

    /**
     * Updates price data. Called in `EventListener.onTickStart()` while in skyblock.
     */
    fun fetchPrices() {
        val time = System.currentTimeMillis()
        if (time - lastFetch <= GhostConfig.priceFrequency * 60_000) return

        lastFetch = time

        for (trackedPrice in trackedPrices) {
            val json = getApiData(trackedPrice.itemId)
            if (json != null) {
                trackedPrice.price = getAverage(json).coerceAtLeast(trackedPrice.npc)
                logInfo("Fetched price for ${trackedPrice.itemId}: ${trackedPrice.price}")
            }
            else logError("Failed to fetch price for ${trackedPrice.itemId}")
        }
    }

    /**
     * Makes the api request and gets the response as a json array. Plain java http client since OneConfig
     * is no longer used.
     */
    private fun getApiData(itemId: String): JsonArray? {
        val url = "$BASE_URL/$itemId/history/${GhostConfig.priceTimespan}"
        try {
            val request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofMillis(TIMEOUT.toLong()))
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            return gson.fromJson(response.body(), JsonArray::class.java)
        } catch (e: Exception) {
            logError("Failed to fetch api data from $url: ${e.message}")
        }

        return null
    }

    private fun getAverage(jsonArray: JsonArray): Float {
        var total = 0f
        var count = 0

        for (jsonElement: JsonElement in jsonArray) {
            try {
                total += (jsonElement as JsonObject)[PRICE_KEY].asFloat
            } catch (e: ClassCastException) {
                logError("Couldn't parse price data as a float")
                continue
            } catch (e: Exception) {
                logError("Unexpected error when parsing price data")
                continue
            }
            count++
        }

        return total / count
    }
}

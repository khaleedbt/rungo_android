package dev.batipy.rungo.data.catalog

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.CityDto
import dev.batipy.rungo.data.network.dto.MerchantDetailDto
import dev.batipy.rungo.data.network.dto.MerchantDto
import dev.batipy.rungo.data.network.dto.ServiceDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val TAG = "CatalogRepository"

class CatalogRepository(private val api: RunGoApi) {
    suspend fun getServices(): Result<List<ServiceDto>> =
        runCatching { api.getServices().results }
            .onFailure { Log.e(TAG, "getServices failed", it) }

    suspend fun getMerchants(cityId: Int? = null): Result<List<MerchantDto>> =
        runCatching { api.getMerchants(cityId).results }
            .onFailure { Log.e(TAG, "getMerchants failed", it) }

    suspend fun getMerchant(id: Int): Result<MerchantDetailDto> =
        runCatching { api.getMerchant(id) }
            .onFailure { Log.e(TAG, "getMerchant failed", it) }

    suspend fun getCities(): Result<List<CityDto>> =
        runCatching { api.getCities().results }
            .onFailure { Log.e(TAG, "getCities failed", it) }

    /**
     * Response looks like {"try_rate": "47.0803", "syp_rate": "12900.0000", "updated_at": "..."} —
     * confirmed from a live response. Also tries a couple of looser variants
     * ("try"/"TRY") in case the shape changes, before giving up on a code.
     */
    suspend fun getExchangeRates(): Result<Map<String, Double>> =
        runCatching {
            val json = api.getExchangeRate()
            buildMap {
                listOf("try", "syp").forEach { code ->
                    findRate(json, code)?.let { rate ->
                        // syp_rate comes back 100x too large (e.g. 12900.0000 instead of 129.00).
                        put(code, if (code == "syp") rate / 100 else rate)
                    }
                }
            }
        }.onFailure { Log.e(TAG, "getExchangeRates failed", it) }

    private fun findRate(json: JsonObject, code: String): Double? {
        val candidateKeys = listOf("${code}_rate", code)
        fun search(obj: JsonObject): Double? {
            for ((key, value) in obj) {
                if (candidateKeys.any { key.equals(it, ignoreCase = true) }) {
                    return (value as? JsonPrimitive)?.content?.toDoubleOrNull()
                }
            }
            return null
        }
        search(json)?.let { return it }
        (json["rates"] as? JsonObject)?.let { search(it)?.let { rate -> return rate } }
        return null
    }
}

package dev.batipy.rungo.data.catalog

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.CityDto
import dev.batipy.rungo.data.network.dto.MerchantDto
import dev.batipy.rungo.data.network.dto.ServiceDto

private const val TAG = "CatalogRepository"

class CatalogRepository(private val api: RunGoApi) {
    suspend fun getServices(): Result<List<ServiceDto>> =
        runCatching { api.getServices().results }
            .onFailure { Log.e(TAG, "getServices failed", it) }

    suspend fun getMerchants(): Result<List<MerchantDto>> =
        runCatching { api.getMerchants().results }
            .onFailure { Log.e(TAG, "getMerchants failed", it) }

    suspend fun getCities(): Result<List<CityDto>> =
        runCatching { api.getCities().results }
            .onFailure { Log.e(TAG, "getCities failed", it) }
}

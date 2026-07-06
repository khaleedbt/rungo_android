package dev.batipy.rungo.ui.common

import dev.batipy.rungo.data.network.dto.CategoryDto
import dev.batipy.rungo.data.network.dto.MerchantDetailDto
import dev.batipy.rungo.data.network.dto.MerchantDto
import dev.batipy.rungo.data.network.dto.ServiceDto
import java.util.Locale

/**
 * Some catalog content (services, merchants, categories) is translated by the
 * backend into separate `_en`/`_ar` fields alongside the base (Russian) one,
 * rather than served pre-localized via Accept-Language — so the client must
 * pick the right field itself based on the app's current language, falling
 * back to the base field when a translation is missing/blank.
 */
private fun localizedField(base: String, en: String?, ar: String?): String {
    return when (Locale.getDefault().language) {
        "en" -> en?.takeIf { it.isNotBlank() } ?: base
        "ar" -> ar?.takeIf { it.isNotBlank() } ?: base
        else -> base
    }
}

val ServiceDto.localizedName: String
    get() = localizedField(name, nameEn, nameAr)

val ServiceDto.localizedDescription: String
    get() = localizedField(description, descriptionEn, descriptionAr)

val MerchantDto.localizedDescription: String
    get() = localizedField(description, descriptionEn, descriptionAr)

val MerchantDetailDto.localizedDescription: String
    get() = localizedField(description, descriptionEn, descriptionAr)

val CategoryDto.localizedName: String
    get() = localizedField(name, nameEn, nameAr)

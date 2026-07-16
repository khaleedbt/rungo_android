package dev.batipy.rungo.ui.services

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.ServiceDto
import dev.batipy.rungo.ui.common.localizedName
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoBackground
import dev.batipy.rungo.ui.theme.RunGoBrandOrange
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoLightAccentText
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoLightField
import dev.batipy.rungo.ui.theme.RunGoLightSurfaceMuted
import dev.batipy.rungo.ui.theme.RunGoLightTextPrimary
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoOnBrandOrange
import dev.batipy.rungo.ui.theme.RunGoPlaceholder
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary
import java.util.Locale

private val ErrorColor = Color(0xFFFF6B6B)
private val ErrorColorLight = Color(0xFFB3261E)
private val FeeCardBackgroundLight = Color(0xFFFCEACB)

private data class CurrencyOption(val code: String, val label: String)

private val currencyOptions = listOf(
    CurrencyOption("usd", "USD $"),
    CurrencyOption("try", "TRY ₺"),
    CurrencyOption("syp", "SYP S£")
)

@Composable
private fun kindLabel(kind: String) = when (kind) {
    "visit" -> stringResource(R.string.service_kind_visit)
    "delivery" -> stringResource(R.string.service_kind_delivery)
    else -> kind
}

/**
 * Converts the USD base fare into the selected currency using rates fetched
 * from /api/v1/exchange-rate/. Falls back to a 1:1 rate (i.e. shows the USD
 * amount with the target symbol) if that rate wasn't found in the response.
 */
private fun formatServicePrice(baseFareUsd: String, currency: String, rates: Map<String, Double>): String {
    val base = baseFareUsd.toDoubleOrNull() ?: 0.0
    return when (currency) {
        "try" -> "₺" + String.format(Locale.US, "%.2f", base * (rates["try"] ?: 1.0))
        "syp" -> "S£" + String.format(Locale.US, "%.0f", base * (rates["syp"] ?: 1.0))
        else -> "\$" + String.format(Locale.US, "%.2f", base)
    }
}

@Composable
fun ServiceOrderScreen(
    service: ServiceDto,
    uiState: CreateOrderUiState,
    onBack: () -> Unit,
    onPickupLocationSelect: (Int) -> Unit,
    onPickupManualEntrySelect: () -> Unit,
    onPickupManualAddressChange: (String) -> Unit,
    onLocationSelect: (Int) -> Unit,
    onManualEntrySelect: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onCurrencySelect: (String) -> Unit,
    onSubmit: () -> Unit,
    light: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDelivery = service.kind == "delivery"
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val onAccent = if (light) RunGoOnBrandOrange else Color.White
    val fieldColor = if (light) RunGoLightField else RunGoField
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    val errorColor = if (light) ErrorColorLight else ErrorColor
    Column(modifier = modifier.fillMaxSize().background(if (light) RunGoLightBackground else Color.Unspecified)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(fieldColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.common_back),
                    tint = textPrimary
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = service.localizedName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = textPrimary
                )
                Text(
                    text = kindLabel(service.kind),
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        when (uiState) {
            is CreateOrderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is CreateOrderUiState.LoadError -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = textSecondary)
                }
            }

            is CreateOrderUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isDelivery) {
                        item {
                            SectionCard(title = stringResource(R.string.section_pickup_address), light = light) {
                                AddressPicker(
                                    locations = uiState.locations,
                                    selectedLocationId = uiState.selectedPickupLocationId,
                                    manualAddress = uiState.manualPickupAddress,
                                    placeholder = stringResource(R.string.pickup_placeholder),
                                    onLocationSelect = onPickupLocationSelect,
                                    onManualEntrySelect = onPickupManualEntrySelect,
                                    onManualAddressChange = onPickupManualAddressChange,
                                    light = light
                                )
                            }
                        }
                        item {
                            SectionCard(title = stringResource(R.string.section_delivery_address), light = light) {
                                AddressPicker(
                                    locations = uiState.locations,
                                    selectedLocationId = uiState.selectedLocationId,
                                    manualAddress = uiState.manualAddress,
                                    placeholder = stringResource(R.string.delivery_placeholder),
                                    onLocationSelect = onLocationSelect,
                                    onManualEntrySelect = onManualEntrySelect,
                                    onManualAddressChange = onManualAddressChange,
                                    light = light
                                )
                            }
                        }
                    } else {
                        item {
                            SectionCard(title = stringResource(R.string.section_your_address), light = light) {
                                AddressPicker(
                                    locations = uiState.locations,
                                    selectedLocationId = uiState.selectedLocationId,
                                    manualAddress = uiState.manualAddress,
                                    placeholder = stringResource(R.string.your_address_placeholder),
                                    onLocationSelect = onLocationSelect,
                                    onManualEntrySelect = onManualEntrySelect,
                                    onManualAddressChange = onManualAddressChange,
                                    light = light
                                )
                            }
                        }
                    }
                    item {
                        SectionCard(title = stringResource(R.string.section_comment), light = light) {
                            OutlinedTextField(
                                value = uiState.comment,
                                onValueChange = onCommentChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.comment_placeholder), color = if (light) textSecondary else RunGoPlaceholder) },
                                minLines = 2,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors(light)
                            )
                        }
                    }
                    item {
                        SectionCard(title = stringResource(R.string.section_currency), light = light) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                currencyOptions.forEach { option ->
                                    val selected = option.code == uiState.currency
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onCurrencySelect(option.code) },
                                        color = if (selected) accent else fieldColor,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = option.label,
                                            color = if (selected) onAccent else textSecondary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (uiState.error != null) {
                        item {
                            Text(text = uiState.error, color = errorColor)
                        }
                    }
                    item {
                        val accentText = if (light) RunGoLightAccentText else RunGoAccent
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (light) FeeCardBackgroundLight else Color(0xFFEAF1FB),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.service_fee_label),
                                        color = accentText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatServicePrice(
                                            baseFareUsd = service.baseFareUsd,
                                            currency = uiState.currency,
                                            rates = uiState.exchangeRates
                                        ),
                                        color = accentText,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.cod_note),
                                    color = accentText.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = onSubmit,
                            enabled = !uiState.submitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(containerColor = if (light) RunGoBrandOrange else MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.submitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = onAccent,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.create_order_button), color = onAccent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, light: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (light) RunGoLightField else RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = if (light) RunGoLightTextSecondary else RunGoTextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Box(modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun AddressPicker(
    locations: List<LocationDto>,
    selectedLocationId: Int?,
    manualAddress: String,
    placeholder: String,
    onLocationSelect: (Int) -> Unit,
    onManualEntrySelect: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    light: Boolean = false
) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(locations) { location ->
                AddressChip(
                    label = "📍 " + location.label.ifBlank { stringResource(R.string.location_label) },
                    selected = location.id == selectedLocationId,
                    onClick = { onLocationSelect(location.id) },
                    light = light
                )
            }
            item {
                AddressChip(
                    label = stringResource(R.string.manual_entry_chip),
                    selected = selectedLocationId == null,
                    onClick = onManualEntrySelect,
                    light = light
                )
            }
        }
        if (selectedLocationId == null) {
            OutlinedTextField(
                value = manualAddress,
                onValueChange = onManualAddressChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                placeholder = { Text(placeholder, color = if (light) RunGoLightTextSecondary else RunGoPlaceholder) },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors(light)
            )
        }
    }
}

@Composable
private fun AddressChip(label: String, selected: Boolean, onClick: () -> Unit, light: Boolean = false) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val accentText = if (light) RunGoLightAccentText else RunGoAccent
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) accent else if (light) RunGoLightSurfaceMuted else RunGoBackground,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            color = if (selected) { if (light) RunGoOnBrandOrange else Color.White } else accentText,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun fieldColors(light: Boolean = false) = if (light) OutlinedTextFieldDefaults.colors(
    focusedContainerColor = RunGoLightSurfaceMuted,
    unfocusedContainerColor = RunGoLightSurfaceMuted,
    focusedTextColor = RunGoLightTextPrimary,
    unfocusedTextColor = RunGoLightTextPrimary
) else OutlinedTextFieldDefaults.colors(
    focusedContainerColor = RunGoBackground,
    unfocusedContainerColor = RunGoBackground,
    focusedTextColor = RunGoTextPrimary,
    unfocusedTextColor = RunGoTextPrimary
)

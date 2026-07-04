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
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.data.network.dto.CityDto
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.ServiceDto
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoBackground
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoPlaceholder
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary

private val ErrorColor = Color(0xFFFF6B6B)

private data class CurrencyOption(val code: String, val label: String)

private val currencyOptions = listOf(
    CurrencyOption("usd", "USD $"),
    CurrencyOption("try", "TRY ₺"),
    CurrencyOption("syp", "SYP S£")
)

private fun kindLabel(kind: String) = when (kind) {
    "visit" -> "Выезд к клиенту"
    "delivery" -> "Доставка А→Б"
    else -> kind
}

@Composable
fun ServiceOrderScreen(
    service: ServiceDto,
    uiState: CreateOrderUiState,
    onBack: () -> Unit,
    onCitySelect: (Int) -> Unit,
    onLocationSelect: (Int) -> Unit,
    onManualEntrySelect: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onCurrencySelect: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(RunGoField, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = RunGoTextPrimary
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = service.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = RunGoTextPrimary
                )
                Text(
                    text = kindLabel(service.kind),
                    color = RunGoTextSecondary,
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
                    Text(text = uiState.message, color = RunGoTextSecondary)
                }
            }

            is CreateOrderUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SectionCard(title = "ГОРОД") {
                            CityDropdown(
                                cities = uiState.cities,
                                selectedCityId = uiState.selectedCityId,
                                onCitySelect = onCitySelect
                            )
                        }
                    }
                    item {
                        SectionCard(title = "ВАШ АДРЕС") {
                            AddressPicker(
                                locations = uiState.locations,
                                selectedLocationId = uiState.selectedLocationId,
                                manualAddress = uiState.manualAddress,
                                onLocationSelect = onLocationSelect,
                                onManualEntrySelect = onManualEntrySelect,
                                onManualAddressChange = onManualAddressChange
                            )
                        }
                    }
                    item {
                        SectionCard(title = "КОММЕНТАРИЙ") {
                            OutlinedTextField(
                                value = uiState.comment,
                                onValueChange = onCommentChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Что нужно сделать?", color = RunGoPlaceholder) },
                                minLines = 2,
                                colors = fieldColors()
                            )
                        }
                    }
                    item {
                        SectionCard(title = "ВАЛЮТА") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                currencyOptions.forEach { option ->
                                    val selected = option.code == uiState.currency
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onCurrencySelect(option.code) },
                                        color = if (selected) RunGoAccent else RunGoField,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = option.label,
                                            color = if (selected) Color.White else RunGoTextSecondary,
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
                            Text(text = uiState.error, color = ErrorColor)
                        }
                    }
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFEAF1FB),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Стоимость услуги",
                                        color = RunGoAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "\$${service.baseFareUsd}",
                                        color = RunGoAccent,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "Оплата наличными курьеру при получении",
                                    color = RunGoAccent.copy(alpha = 0.7f),
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.submitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Создать заказ", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = RunGoTextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Box(modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CityDropdown(
    cities: List<CityDto>,
    selectedCityId: Int?,
    onCitySelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = cities.find { it.id == selectedCityId }?.name ?: "Выберите город"

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            color = RunGoBackground,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selectedName, color = RunGoTextPrimary, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = RunGoTextSecondary)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name) },
                    onClick = {
                        onCitySelect(city.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddressPicker(
    locations: List<LocationDto>,
    selectedLocationId: Int?,
    manualAddress: String,
    onLocationSelect: (Int) -> Unit,
    onManualEntrySelect: () -> Unit,
    onManualAddressChange: (String) -> Unit
) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(locations) { location ->
                AddressChip(
                    label = "📍 " + location.label.ifBlank { "Локация" },
                    selected = location.id == selectedLocationId,
                    onClick = { onLocationSelect(location.id) }
                )
            }
            item {
                AddressChip(
                    label = "✏️ Вручную",
                    selected = selectedLocationId == null,
                    onClick = onManualEntrySelect
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
                placeholder = { Text("Ваш адрес", color = RunGoPlaceholder) },
                colors = fieldColors()
            )
        }
    }
}

@Composable
private fun AddressChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) RunGoAccent else RunGoBackground,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else RunGoAccent,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = RunGoBackground,
    unfocusedContainerColor = RunGoBackground,
    focusedTextColor = RunGoTextPrimary,
    unfocusedTextColor = RunGoTextPrimary
)

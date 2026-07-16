package dev.batipy.rungo.ui.cart

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.batipy.rungo.R
import dev.batipy.rungo.data.cart.CartItem
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.ui.common.QuantityStepButton
import dev.batipy.rungo.ui.common.currencySymbol
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

private val ErrorColorLight = Color(0xFFB3261E)
private val FeeCardBackgroundLight = Color(0xFFFCEACB)

private data class MerchantGroup(
    val merchantId: Int,
    val merchantName: String,
    val feeUsd: Double,
    val items: List<CartItem>
)

private fun groupByMerchant(items: List<CartItem>): List<MerchantGroup> =
    items.groupBy { it.product.merchant }.map { (merchantId, groupItems) ->
        MerchantGroup(
            merchantId = merchantId,
            merchantName = groupItems.first().product.merchantName,
            feeUsd = groupItems.first().product.merchantDeliveryFeeUsd.toDoubleOrNull() ?: 0.0,
            items = groupItems
        )
    }

private val currencyOptions = listOf("usd" to "USD $", "try" to "TRY ₺", "syp" to "SYP S£")

@Composable
fun CartScreen(
    uiState: CartUiState,
    cartItems: List<CartItem>,
    onGoToShopClick: () -> Unit,
    onUpdateQuantity: (productId: Int, quantity: Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onLocationSelect: (Int) -> Unit,
    onManualEntrySelect: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onCurrencySelect: (String) -> Unit,
    onSubmit: () -> Unit,
    onRequestCurrentLocation: () -> Unit = {},
    isAddingCurrentLocation: Boolean = false,
    onCurrentLocationPermissionDenied: () -> Unit = {},
    message: String? = null,
    onConsumeMessage: () -> Unit = {},
    light: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (cartItems.isEmpty()) {
        EmptyCart(onGoToShopClick, light, modifier)
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onConsumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    when (uiState) {
        is CartUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(if (light) RunGoLightBackground else Color.Unspecified), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is CartUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(if (light) RunGoLightBackground else Color.Unspecified), contentAlignment = Alignment.Center) {
                Text(text = uiState.message, color = if (light) RunGoLightTextSecondary else RunGoTextSecondary)
            }
        }

        is CartUiState.Ready -> {
            CartForm(
                uiState = uiState,
                cartItems = cartItems,
                onUpdateQuantity = onUpdateQuantity,
                onRemoveItem = onRemoveItem,
                onClearCart = onClearCart,
                onLocationSelect = onLocationSelect,
                onManualEntrySelect = onManualEntrySelect,
                onManualAddressChange = onManualAddressChange,
                onCommentChange = onCommentChange,
                onCurrencySelect = onCurrencySelect,
                onSubmit = onSubmit,
                onRequestCurrentLocation = onRequestCurrentLocation,
                isAddingCurrentLocation = isAddingCurrentLocation,
                onCurrentLocationPermissionDenied = onCurrentLocationPermissionDenied,
                light = light,
                modifier = Modifier
            )
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }
}

@Composable
private fun CartForm(
    uiState: CartUiState.Ready,
    cartItems: List<CartItem>,
    onUpdateQuantity: (productId: Int, quantity: Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onLocationSelect: (Int) -> Unit,
    onManualEntrySelect: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onCurrencySelect: (String) -> Unit,
    onSubmit: () -> Unit,
    onRequestCurrentLocation: () -> Unit = {},
    isAddingCurrentLocation: Boolean = false,
    onCurrentLocationPermissionDenied: () -> Unit = {},
    light: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onRequestCurrentLocation() else onCurrentLocationPermissionDenied() }
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val onAccent = if (light) RunGoOnBrandOrange else Color.White
    val accentText = if (light) RunGoLightAccentText else RunGoAccent
    val fieldColor = if (light) RunGoLightField else RunGoField
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    val merchantGroups = groupByMerchant(cartItems)
    val goodsUsd = cartItems.sumOf { (it.product.priceUsd.toDoubleOrNull() ?: 0.0) * it.quantity }
    val deliveryUsd = merchantGroups.sumOf { it.feeUsd }
    val rate = when (uiState.currency) {
        "try" -> uiState.exchangeRates["try"] ?: 1.0
        "syp" -> uiState.exchangeRates["syp"] ?: 1.0
        else -> 1.0
    }
    val sym = currencySymbol(uiState.currency)
    val totalConverted = (goodsUsd + deliveryUsd) * rate

    LazyColumn(
        modifier = modifier.fillMaxSize().background(if (light) RunGoLightBackground else Color.Unspecified),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cart_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (light) RunGoLightTextPrimary else Color.Unspecified
                )
                Row(
                    modifier = Modifier.clickable(onClick = onClearCart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.cart_clear_button),
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.cart_clear_button),
                        color = textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        items(merchantGroups, key = { it.merchantId }) { group ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = fieldColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    if (group.merchantName.isNotBlank()) {
                        Text(
                            text = group.merchantName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    group.items.forEachIndexed { index, item ->
                        CartItemRow(
                            item = item,
                            onRemove = { onRemoveItem(item.product.id) },
                            onUpdateQuantity = { qty -> onUpdateQuantity(item.product.id, qty) },
                            light = light
                        )
                        if (index != group.items.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_delivery_address), light = light) {
                Column {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Placed first — capturing where you're standing right
                        // now is usually the fastest way to fill this field.
                        item {
                            AddressChip(
                                label = stringResource(R.string.use_current_location_chip),
                                selected = false,
                                loading = isAddingCurrentLocation,
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        onRequestCurrentLocation()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                light = light
                            )
                        }
                        items(uiState.locations) { location ->
                            AddressChip(
                                label = "📍 " + location.label.ifBlank { stringResource(R.string.location_label) },
                                selected = location.id == uiState.selectedLocationId,
                                onClick = { onLocationSelect(location.id) },
                                light = light
                            )
                        }
                        item {
                            AddressChip(
                                label = stringResource(R.string.manual_entry_chip),
                                selected = uiState.selectedLocationId == null,
                                onClick = onManualEntrySelect,
                                light = light
                            )
                        }
                    }
                    if (uiState.selectedLocationId == null) {
                        OutlinedTextField(
                            value = uiState.manualAddress,
                            onValueChange = onManualAddressChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            placeholder = { Text(stringResource(R.string.delivery_placeholder), color = if (light) textSecondary else RunGoPlaceholder) },
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors(light)
                        )
                    }
                    OutlinedTextField(
                        value = uiState.comment,
                        onValueChange = onCommentChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        placeholder = { Text(stringResource(R.string.comment_placeholder), color = if (light) textSecondary else RunGoPlaceholder) },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(light)
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_currency), light = light) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencyOptions.forEach { (code, label) ->
                        val selected = code == uiState.currency
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCurrencySelect(code) },
                            color = if (selected) accent else fieldColor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = label,
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

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (light) FeeCardBackgroundLight else Color(0xFFEAF1FB),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PriceRow(stringResource(R.string.shop_goods_total), sym + String.format(Locale.US, "%.2f", goodsUsd * rate), accentText)
                    merchantGroups.forEach { group ->
                        PriceRow(
                            label = "🏃 RunGo · ${group.merchantName}",
                            value = if (group.feeUsd > 0) {
                                sym + String.format(Locale.US, "%.2f", group.feeUsd * rate)
                            } else {
                                stringResource(R.string.shop_delivery_free)
                            },
                            color = accentText
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.cart_total_label), color = accentText, fontWeight = FontWeight.Bold)
                        Text(
                            text = sym + String.format(Locale.US, "%.2f", totalConverted),
                            color = accentText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = stringResource(R.string.cod_note),
                        color = accentText.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (uiState.error != null) {
            item {
                Text(text = uiState.error, color = if (light) ErrorColorLight else Color(0xFFFF6B6B))
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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = onAccent, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.cart_submit_button), color = onAccent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = color, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
    onUpdateQuantity: (Int) -> Unit,
    light: Boolean = false
) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val onAccent = if (light) RunGoOnBrandOrange else Color.White
    val accentText = if (light) RunGoLightAccentText else RunGoAccent
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = item.product.name,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.profile_location_delete_desc),
                    tint = textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityStepButton(
                    symbol = "−",
                    containerColor = textSecondary.copy(alpha = 0.15f),
                    contentColor = textPrimary,
                    onClick = { onUpdateQuantity(item.quantity - 1) }
                )
                Text(
                    text = "${item.quantity}",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                QuantityStepButton(
                    symbol = "+",
                    containerColor = accent,
                    contentColor = onAccent,
                    onClick = { onUpdateQuantity(item.quantity + 1) }
                )
            }
            val itemTotal = (item.product.priceUsd.toDoubleOrNull() ?: 0.0) * item.quantity
            Text(
                text = "$" + String.format(Locale.US, "%.2f", itemTotal),
                color = accentText,
                fontWeight = FontWeight.Bold
            )
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
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            content()
        }
    }
}

@Composable
private fun AddressChip(label: String, selected: Boolean, onClick: () -> Unit, loading: Boolean = false, light: Boolean = false) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val accentText = if (light) RunGoLightAccentText else RunGoAccent
    Surface(
        modifier = Modifier.clickable(enabled = !loading, onClick = onClick),
        color = if (selected) accent else if (light) RunGoLightSurfaceMuted else RunGoBackground,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .size(18.dp),
                color = accentText,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = label,
                color = if (selected) { if (light) RunGoOnBrandOrange else Color.White } else accentText,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
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

@Composable
private fun EmptyCart(onGoToShopClick: () -> Unit, light: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (light) RunGoLightBackground else Color.Unspecified)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (light) RunGoLightField else RunGoField),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = null,
                tint = if (light) RunGoLightTextSecondary else RunGoTextSecondary,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = stringResource(R.string.cart_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = if (light) RunGoLightTextSecondary else RunGoTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
        )
        Button(
            onClick = onGoToShopClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (light) RunGoBrandOrange else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = stringResource(R.string.cart_go_to_shop), color = if (light) RunGoOnBrandOrange else Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

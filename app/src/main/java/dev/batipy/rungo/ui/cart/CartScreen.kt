package dev.batipy.rungo.ui.cart

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.R
import dev.batipy.rungo.data.cart.CartItem
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.ui.common.QuantityStepButton
import dev.batipy.rungo.ui.common.currencySymbol
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoBackground
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoPlaceholder
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary
import java.util.Locale

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
    onLocationSelect: (Int) -> Unit,
    onManualEntrySelect: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onCurrencySelect: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (cartItems.isEmpty()) {
        EmptyCart(onGoToShopClick, modifier)
        return
    }

    when (uiState) {
        is CartUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is CartUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.message, color = RunGoTextSecondary)
            }
        }

        is CartUiState.Ready -> {
            CartForm(
                uiState = uiState,
                cartItems = cartItems,
                onUpdateQuantity = onUpdateQuantity,
                onRemoveItem = onRemoveItem,
                onLocationSelect = onLocationSelect,
                onManualEntrySelect = onManualEntrySelect,
                onManualAddressChange = onManualAddressChange,
                onCommentChange = onCommentChange,
                onCurrencySelect = onCurrencySelect,
                onSubmit = onSubmit,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CartForm(
    uiState: CartUiState.Ready,
    cartItems: List<CartItem>,
    onUpdateQuantity: (productId: Int, quantity: Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onLocationSelect: (Int) -> Unit,
    onManualEntrySelect: () -> Unit,
    onManualAddressChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onCurrencySelect: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.cart_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(merchantGroups, key = { it.merchantId }) { group ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = RunGoField,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    if (group.merchantName.isNotBlank()) {
                        Text(
                            text = group.merchantName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = RunGoTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    group.items.forEachIndexed { index, item ->
                        CartItemRow(
                            item = item,
                            onRemove = { onRemoveItem(item.product.id) },
                            onUpdateQuantity = { qty -> onUpdateQuantity(item.product.id, qty) }
                        )
                        if (index != group.items.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_delivery_address)) {
                Column {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.locations) { location ->
                            AddressChip(
                                label = "📍 " + location.label.ifBlank { stringResource(R.string.location_label) },
                                selected = location.id == uiState.selectedLocationId,
                                onClick = { onLocationSelect(location.id) }
                            )
                        }
                        item {
                            AddressChip(
                                label = stringResource(R.string.manual_entry_chip),
                                selected = uiState.selectedLocationId == null,
                                onClick = onManualEntrySelect
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
                            placeholder = { Text(stringResource(R.string.delivery_placeholder), color = RunGoPlaceholder) },
                            colors = fieldColors()
                        )
                    }
                    OutlinedTextField(
                        value = uiState.comment,
                        onValueChange = onCommentChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        placeholder = { Text(stringResource(R.string.comment_placeholder), color = RunGoPlaceholder) },
                        minLines = 2,
                        colors = fieldColors()
                    )
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.section_currency)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencyOptions.forEach { (code, label) ->
                        val selected = code == uiState.currency
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCurrencySelect(code) },
                            color = if (selected) RunGoAccent else RunGoField,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = label,
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

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFEAF1FB),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PriceRow(stringResource(R.string.shop_goods_total), sym + String.format(Locale.US, "%.2f", goodsUsd * rate))
                    merchantGroups.forEach { group ->
                        PriceRow(
                            label = "🏃 RunGo · ${group.merchantName}",
                            value = if (group.feeUsd > 0) {
                                sym + String.format(Locale.US, "%.2f", group.feeUsd * rate)
                            } else {
                                stringResource(R.string.shop_delivery_free)
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = stringResource(R.string.cart_total_label), color = RunGoAccent, fontWeight = FontWeight.Bold)
                        Text(
                            text = sym + String.format(Locale.US, "%.2f", totalConverted),
                            color = RunGoAccent,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = stringResource(R.string.cod_note),
                        color = RunGoAccent.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (uiState.error != null) {
            item {
                Text(text = uiState.error, color = Color(0xFFFF6B6B))
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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.cart_submit_button), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = RunGoAccent, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = RunGoAccent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = item.product.name,
                fontWeight = FontWeight.SemiBold,
                color = RunGoTextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.profile_location_delete_desc),
                    tint = RunGoTextSecondary,
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
                    containerColor = RunGoTextSecondary.copy(alpha = 0.15f),
                    contentColor = RunGoTextPrimary,
                    onClick = { onUpdateQuantity(item.quantity - 1) }
                )
                Text(
                    text = "${item.quantity}",
                    color = RunGoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                QuantityStepButton(
                    symbol = "+",
                    containerColor = RunGoAccent,
                    contentColor = Color.White,
                    onClick = { onUpdateQuantity(item.quantity + 1) }
                )
            }
            val itemTotal = (item.product.priceUsd.toDoubleOrNull() ?: 0.0) * item.quantity
            Text(
                text = "$" + String.format(Locale.US, "%.2f", itemTotal),
                color = RunGoAccent,
                fontWeight = FontWeight.Bold
            )
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
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            content()
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

@Composable
private fun EmptyCart(onGoToShopClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(RunGoField),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = null,
                tint = RunGoTextSecondary,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = stringResource(R.string.cart_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = RunGoTextSecondary,
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
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = stringResource(R.string.cart_go_to_shop), fontWeight = FontWeight.SemiBold)
        }
    }
}

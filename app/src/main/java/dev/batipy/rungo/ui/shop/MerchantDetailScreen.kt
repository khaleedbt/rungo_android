package dev.batipy.rungo.ui.shop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.batipy.rungo.R
import dev.batipy.rungo.data.cart.CartItem
import dev.batipy.rungo.data.network.dto.CategoryDto
import dev.batipy.rungo.data.network.dto.MerchantDetailDto
import dev.batipy.rungo.data.network.dto.ProductDto
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary
import java.util.Locale

@Composable
fun MerchantDetailScreen(
    uiState: MerchantDetailUiState,
    cartItems: List<CartItem>,
    onBack: () -> Unit,
    onAddToCart: (ProductDto) -> Unit,
    onUpdateQuantity: (productId: Int, quantity: Int) -> Unit,
    onGoToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        contentDescription = stringResource(R.string.common_back),
                        tint = RunGoTextPrimary
                    )
                }
                if (uiState is MerchantDetailUiState.Success) {
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = uiState.merchant.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = RunGoTextPrimary
                        )
                        if (uiState.merchant.description.isNotBlank()) {
                            Text(
                                text = uiState.merchant.description,
                                color = RunGoTextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            when (uiState) {
                is MerchantDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is MerchantDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.message, color = RunGoTextSecondary)
                    }
                }

                is MerchantDetailUiState.Success -> {
                    val merchant = uiState.merchant
                    if (merchant.categories.all { it.products.isEmpty() }) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.shop_no_products), color = RunGoTextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (merchant.logo != null) {
                                item {
                                    AsyncImage(
                                        model = merchant.logo,
                                        contentDescription = merchant.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                    )
                                }
                            }
                            merchant.categories.forEach { category ->
                                if (category.products.isNotEmpty()) {
                                    item {
                                        CategorySection(
                                            category = category,
                                            quantityOf = { productId -> cartItems.find { it.product.id == productId }?.quantity ?: 0 },
                                            onAddToCart = onAddToCart,
                                            onUpdateQuantity = onUpdateQuantity
                                        )
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }

        val cartCount = cartItems.sumOf { it.quantity }
        if (cartCount > 0) {
            val cartTotal = cartItems.sumOf { it.product.priceUsd.toDoubleOrNull()?.times(it.quantity) ?: 0.0 }
            Button(
                onClick = onGoToCart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "🛒 " + stringResource(R.string.shop_go_to_cart),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
                    Text(
                        text = "$" + String.format(Locale.US, "%.2f", cartTotal),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    category: CategoryDto,
    quantityOf: (Int) -> Int,
    onAddToCart: (ProductDto) -> Unit,
    onUpdateQuantity: (productId: Int, quantity: Int) -> Unit
) {
    Column {
        if (category.name.isNotBlank()) {
            Text(
                text = category.name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = RunGoTextSecondary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            category.products.forEach { product ->
                ProductRow(
                    product = product,
                    quantity = quantityOf(product.id),
                    onAdd = { onAddToCart(product) },
                    onUpdateQuantity = { qty -> onUpdateQuantity(product.id, qty) }
                )
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: ProductDto,
    quantity: Int,
    onAdd: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoField,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (product.image != null) {
                AsyncImage(
                    model = product.image,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(RunGoAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🛍", style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(text = product.name, fontWeight = FontWeight.Bold, color = RunGoTextPrimary)
                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = RunGoTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = "$" + (product.priceUsd.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: product.priceUsd),
                    color = RunGoAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (quantity == 0) {
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(36.dp)
                        .background(RunGoAccent, CircleShape)
                ) {
                    Text(text = "+", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onUpdateQuantity(quantity - 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(RunGoTextSecondary.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Text(text = "−", color = RunGoTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$quantity",
                        color = RunGoTextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        onClick = { onUpdateQuantity(quantity + 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(RunGoAccent, CircleShape)
                    ) {
                        Text(text = "+", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

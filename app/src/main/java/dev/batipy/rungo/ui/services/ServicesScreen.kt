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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.batipy.rungo.data.network.dto.MerchantDto
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.network.dto.ServiceDto
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoBackground
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary

private val ActiveOrderPillBackground = Color(0xFFFCE4C4)
private val ActiveOrderPillText = Color(0xFFB56A17)

private fun kindLabel(kind: String) = when (kind) {
    "visit" -> "Выезд к клиенту"
    "delivery" -> "Доставка А→Б"
    else -> kind
}

private fun activeOrderStatusLabel(status: String) = when (status) {
    "new" -> "Новый"
    "confirmed" -> "Подтверждён"
    "in_progress" -> "Собирается"
    "in_delivery" -> "Доставляется"
    else -> status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    uiState: ServicesUiState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onServiceClick: (ServiceDto) -> Unit = {},
    onActiveOrderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when (uiState) {
            is ServicesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ServicesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = RunGoTextSecondary)
                }
            }

            is ServicesUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Услуги",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.activeOrder != null) {
                        item {
                            ActiveOrderCard(uiState.activeOrder, onClick = onActiveOrderClick)
                        }
                    }
                    items(uiState.services) { service ->
                        ServiceCard(service, onClick = { onServiceClick(service) })
                    }
                    if (uiState.merchants.isNotEmpty()) {
                        item {
                            Text(
                                text = "ПАРТНЁРЫ",
                                style = MaterialTheme.typography.labelMedium,
                                color = RunGoTextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    items(uiState.merchants) { merchant ->
                        MerchantCard(merchant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveOrderCard(order: OrderDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RunGoField)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(RunGoAccent)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Text(
                text = "АКТИВНЫЙ ЗАКАЗ",
                color = RunGoAccent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Заказ #${order.id}",
                    color = RunGoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(color = ActiveOrderPillBackground, shape = RoundedCornerShape(50)) {
                    Text(
                        text = activeOrderStatusLabel(order.status),
                        color = ActiveOrderPillText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (!order.serviceName.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = order.serviceName,
                        color = RunGoAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = RunGoTextSecondary
                    )
                }
            }
            if (!order.deliveryAddress.isNullOrBlank()) {
                Text(
                    text = order.deliveryAddress,
                    color = RunGoTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(service: ServiceDto, onClick: () -> Unit) {
    CatalogCard(
        imageUrl = service.image,
        title = service.name,
        description = service.description,
        badge = kindLabel(service.kind),
        price = "от \$${service.baseFareUsd}",
        onClick = onClick
    )
}

@Composable
private fun MerchantCard(merchant: MerchantDto) {
    CatalogCard(
        imageUrl = merchant.logo,
        title = merchant.name,
        description = merchant.description,
        badge = null,
        price = "Доставка \$${merchant.deliveryFeeUsd}",
        onClick = {}
    )
}

@Composable
private fun CatalogCard(
    imageUrl: String?,
    title: String,
    description: String,
    badge: String?,
    price: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RunGoAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, color = RunGoTextPrimary)
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = RunGoTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (badge != null) {
                        Surface(
                            color = RunGoTextPrimary,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = badge,
                                color = RunGoBackground,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = price,
                        color = RunGoAccent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = RunGoTextSecondary
            )
        }
    }
}

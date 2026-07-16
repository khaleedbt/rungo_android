package dev.batipy.rungo.ui.services

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.MerchantDto
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.network.dto.ServiceDto
import dev.batipy.rungo.ui.common.localizedDescription
import dev.batipy.rungo.ui.common.localizedName
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoAccentLight
import dev.batipy.rungo.ui.theme.RunGoBackground
import dev.batipy.rungo.ui.theme.RunGoBrandOrange
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoLightAccentText
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoLightField
import dev.batipy.rungo.ui.theme.RunGoLightTextPrimary
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoOnBrandOrange
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary

private val ActiveOrderPillBackground = Color(0xFFFCE4C4)
private val ActiveOrderPillText = Color(0xFFB56A17)

@Composable
private fun kindLabel(kind: String) = when (kind) {
    "visit" -> stringResource(R.string.service_kind_visit)
    "delivery" -> stringResource(R.string.service_kind_delivery)
    else -> kind
}

@Composable
private fun activeOrderStatusLabel(status: String) = when (status) {
    "new" -> stringResource(R.string.services_status_new)
    "confirmed" -> stringResource(R.string.services_status_confirmed)
    "in_progress" -> stringResource(R.string.services_status_in_progress)
    "in_delivery" -> stringResource(R.string.services_status_in_delivery)
    else -> status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    uiState: ServicesUiState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onServiceClick: (ServiceDto) -> Unit = {},
    onActiveOrderClick: (OrderDto) -> Unit = {},
    onMerchantClick: (MerchantDto) -> Unit = {},
    light: Boolean = false,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(if (light) RunGoLightBackground else Color.Unspecified)
    ) {
        when (uiState) {
            is ServicesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ServicesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = if (light) RunGoLightTextSecondary else RunGoTextSecondary)
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
                            text = stringResource(R.string.services_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (light) RunGoLightTextPrimary else Color.Unspecified
                        )
                    }
                    items(uiState.activeOrders, key = { it.id }) { order ->
                        ActiveOrderCard(order, onClick = { onActiveOrderClick(order) }, light = light)
                    }
                    items(uiState.services) { service ->
                        ServiceCard(service, onClick = { onServiceClick(service) }, light = light)
                    }
                    if (uiState.merchants.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.services_partners_header),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (light) RunGoLightTextSecondary else RunGoTextSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    items(uiState.merchants) { merchant ->
                        MerchantCard(merchant, onClick = { onMerchantClick(merchant) }, light = light)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveOrderCard(order: OrderDto, onClick: () -> Unit, light: Boolean = false) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val accentText = if (light) RunGoLightAccentText else RunGoAccent
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (light) RunGoLightField else RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.order_number, order.id),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!order.serviceName.isNullOrBlank()) {
                        Text(
                            text = " · " + order.serviceName,
                            color = accentText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!order.deliveryAddress.isNullOrBlank()) {
                    Text(
                        text = order.deliveryAddress,
                        color = textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Surface(color = ActiveOrderPillBackground, shape = RoundedCornerShape(50)) {
                Text(
                    text = activeOrderStatusLabel(order.status),
                    color = ActiveOrderPillText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ServiceCard(service: ServiceDto, onClick: () -> Unit, light: Boolean = false) {
    CatalogCard(
        imageUrl = service.image,
        title = service.localizedName,
        description = service.localizedDescription,
        badge = kindLabel(service.kind),
        price = stringResource(R.string.services_price_from, service.baseFareUsd),
        onClick = onClick,
        light = light
    )
}

@Composable
private fun MerchantCard(merchant: MerchantDto, onClick: () -> Unit, light: Boolean = false) {
    CatalogCard(
        imageUrl = merchant.logo,
        title = merchant.name,
        description = merchant.localizedDescription,
        badge = null,
        price = stringResource(R.string.services_delivery_price, merchant.deliveryFeeUsd),
        bordered = true,
        onClick = onClick,
        light = light
    )
}

@Composable
private fun CatalogCard(
    imageUrl: String?,
    title: String,
    description: String,
    badge: String?,
    price: String,
    bordered: Boolean = false,
    onClick: () -> Unit,
    light: Boolean = false
) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val accentText = if (light) RunGoLightAccentText else RunGoAccent
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    // The "kind" badge is meant to read as the opposite tone from the body
    // text — inverted (dark chip + light text) in both themes, not just a
    // straight color swap.
    val badgeBg = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val badgeText = if (light) RunGoLightBackground else RunGoBackground
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (light) RunGoLightField else RunGoField,
        border = if (bordered) BorderStroke(1.5.dp, RunGoAccentLight.copy(alpha = 0.55f)) else null,
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
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.take(1).uppercase(),
                        color = if (light) RunGoOnBrandOrange else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, color = textPrimary)
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
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
                            color = badgeBg,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = badge,
                                color = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = price,
                        color = accentText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textSecondary
            )
        }
    }
}

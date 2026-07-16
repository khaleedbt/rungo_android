package dev.batipy.rungo.ui.partner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.ui.common.StatusBadge
import dev.batipy.rungo.ui.theme.RunGoBrandOrange
import dev.batipy.rungo.ui.theme.RunGoLightAccentText
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoLightField
import dev.batipy.rungo.ui.theme.RunGoLightSurfaceMuted
import dev.batipy.rungo.ui.theme.RunGoLightTextPrimary
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoOnBrandOrange
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private fun formatPartnerOrderDate(iso: String): String = try {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.getDefault()))
} catch (e: DateTimeParseException) {
    iso
}

private data class PartnerStatusStyle(val label: String, val container: Color, val content: Color)

@Composable
private fun partnerStatusStyle(status: String): PartnerStatusStyle = when (status) {
    "new" -> PartnerStatusStyle(stringResource(R.string.orders_list_status_new), Color(0xFFEDEAE3), Color(0xFF4A4438))
    "confirmed" -> PartnerStatusStyle(stringResource(R.string.orders_list_status_confirmed), Color(0xFFE4ECFB), Color(0xFF2E4A73))
    "in_progress" -> PartnerStatusStyle(stringResource(R.string.orders_list_status_in_progress), Color(0xFFFFF0C7), Color(0xFF7A5416))
    "in_delivery" -> PartnerStatusStyle(stringResource(R.string.orders_list_status_in_delivery), Color(0xFFFFF0C7), Color(0xFF7A5416))
    "delivered" -> PartnerStatusStyle(stringResource(R.string.orders_list_status_delivered), Color(0xFFCFF7D9), Color(0xFF1B7A3A))
    "cancelled" -> PartnerStatusStyle(stringResource(R.string.orders_list_status_cancelled), Color(0xFFFBE1DE), Color(0xFFB3261E))
    else -> PartnerStatusStyle(status, RunGoLightField, RunGoLightTextSecondary)
}

private fun vehicleIcon(vehicleType: String?): String = when (vehicleType) {
    "car" -> "🚗"
    "motorcycle" -> "🏍️"
    "scooter" -> "🛵"
    "bicycle" -> "🚴"
    "on_foot" -> "🚶"
    else -> "🚴"
}

private fun formatMoney(amount: Double): String = "$" + String.format(Locale.US, "%.2f", amount)

@Composable
fun PartnerOrdersScreen(
    uiState: PartnerOrdersUiState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(RunGoLightBackground)) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            is PartnerOrdersUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is PartnerOrdersUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = RunGoLightTextSecondary)
                }
            }

            is PartnerOrdersUiState.Success -> {
                var selectedTab by remember { mutableIntStateOf(0) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = uiState.merchantName?.ifBlank { null } ?: stringResource(R.string.partner_orders_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = RunGoLightTextPrimary
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PartnerStatTile(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.partner_stats_today),
                                stats = uiState.statsToday
                            )
                            PartnerStatTile(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.partner_stats_month),
                                stats = uiState.statsMonth
                            )
                            PartnerStatTile(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.partner_stats_all_time),
                                stats = uiState.statsAllTime
                            )
                        }
                    }
                    item {
                        PartnerTabSelector(
                            selectedTab = selectedTab,
                            activeCount = uiState.activeOrders.size,
                            onSelect = { selectedTab = it }
                        )
                    }

                    val orders = if (selectedTab == 0) uiState.activeOrders else uiState.historyOrders
                    if (orders.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(
                                    if (selectedTab == 0) R.string.partner_no_active_orders else R.string.courier_no_history_orders
                                ),
                                color = RunGoLightTextSecondary,
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                    }
                    items(orders, key = { it.id }) { order ->
                        PartnerOrderCard(
                            order = order,
                            showCourierBlock = selectedTab == 0
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun PartnerStatTile(label: String, stats: PartnerStats, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = RunGoLightField, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = RunGoLightTextSecondary)
            Text(
                text = formatMoney(stats.revenue),
                color = RunGoLightTextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(R.string.partner_stats_orders_count, stats.deliveredCount),
                style = MaterialTheme.typography.labelSmall,
                color = RunGoLightAccentText
            )
        }
    }
}

@Composable
private fun PartnerTabSelector(selectedTab: Int, activeCount: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        stringResource(R.string.partner_tab_active) + if (activeCount > 0) " ($activeCount)" else "",
        stringResource(R.string.courier_tab_history)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedTab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) },
                color = if (selected) RunGoBrandOrange else RunGoLightField,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = label,
                    color = if (selected) RunGoOnBrandOrange else RunGoLightTextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun PartnerOrderCard(order: OrderDto, showCourierBlock: Boolean) {
    val style = partnerStatusStyle(order.status)
    val hasAddress = !order.deliveryAddress.isNullOrBlank()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoLightField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Section 1 — identity: which order, at what status.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.order_number, order.id),
                    color = RunGoLightTextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    label = style.label,
                    container = style.container,
                    content = style.content,
                    pulse = order.status == "in_progress" || order.status == "in_delivery",
                    textStyle = MaterialTheme.typography.labelSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Section 2 — when and where, each on its own line with an icon
            // rather than run together as plain text.
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = RunGoLightTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = formatPartnerOrderDate(order.createdAt),
                        color = RunGoLightTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                if (hasAddress) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = RunGoLightTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = order.deliveryAddress!!,
                            color = RunGoLightTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            // Section 3 — the goods themselves, set apart in their own muted
            // block so they don't visually run into the date/address above.
            if (order.items.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                    color = RunGoLightSurfaceMuted
                )
                Surface(color = RunGoLightSurfaceMuted, shape = RoundedCornerShape(10.dp)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.productName,
                                    color = RunGoLightTextPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = "×${item.quantity}",
                                    color = RunGoLightTextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 4 — the bottom line: what the partner is owed.
            val goods = order.goodsAmount?.toDoubleOrNull()
            if (goods != null && goods > 0) {
                Text(
                    text = formatMoney(goods),
                    color = RunGoLightAccentText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    textAlign = TextAlign.End
                )
            }
            if (showCourierBlock) {
                CourierInfoBlock(order = order, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun CourierInfoBlock(order: OrderDto, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val courierName = order.courierDisplayName
    Surface(modifier = modifier.fillMaxWidth(), color = RunGoBrandOrange.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (courierName != null) {
                Text(text = vehicleIcon(order.courierVehicleType), modifier = Modifier.padding(end = 8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = courierName, color = RunGoLightTextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    if (order.status == "in_delivery") {
                        Text(
                            text = stringResource(R.string.partner_courier_en_route),
                            color = RunGoLightTextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (!order.courierPhone.isNullOrBlank()) {
                    IconButton(onClick = {
                        val uri = Uri.parse("tel:${order.courierPhone}")
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                        } catch (e: ActivityNotFoundException) {
                            // No dialer app — nothing to do.
                        }
                    }) {
                        Icon(imageVector = Icons.Filled.Call, contentDescription = stringResource(R.string.courier_call_button), tint = RunGoLightAccentText)
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.partner_no_courier_assigned),
                    color = RunGoLightTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

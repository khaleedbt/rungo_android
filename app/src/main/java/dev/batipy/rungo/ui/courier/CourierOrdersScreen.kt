package dev.batipy.rungo.ui.courier

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.ui.common.StatusBadge
import dev.batipy.rungo.ui.common.formatOrderAmount
import dev.batipy.rungo.ui.theme.RunGoBrandOrange
import dev.batipy.rungo.ui.theme.RunGoLightAccentText
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoLightField
import dev.batipy.rungo.ui.theme.RunGoLightSurfaceMuted
import dev.batipy.rungo.ui.theme.RunGoLightTextPrimary
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoOnBrandOrange

// Light-theme trial (see the note in Color.kt) — status-pill colors need
// their own light-vs-dark pairing, not just a global text/bg swap, since a
// container tuned to sit on a dark screen (dark fill + light text) has to
// flip to a light fill + dark text here, not just get lighter.
private val OnlineColor = Color(0xFF1B7A3A)
private val OnlineContainer = Color(0xFFCFF7D9)
private val OfflineContainer = RunGoLightSurfaceMuted

data class CourierStatusStyle(val label: String, val container: Color, val content: Color)

@Composable
fun courierStatusStyle(status: String): CourierStatusStyle = when (status) {
    "confirmed" -> CourierStatusStyle(stringResource(R.string.orders_list_status_confirmed), Color(0xFFE4ECFB), Color(0xFF2E4A73))
    "in_progress" -> CourierStatusStyle(stringResource(R.string.orders_list_status_in_progress), Color(0xFFFFF0C7), Color(0xFF7A5416))
    "in_delivery" -> CourierStatusStyle(stringResource(R.string.orders_list_status_in_delivery), Color(0xFFFFF0C7), Color(0xFF7A5416))
    "delivered" -> CourierStatusStyle(stringResource(R.string.orders_list_status_delivered), Color(0xFFCFF7D9), Color(0xFF1B7A3A))
    "cancelled" -> CourierStatusStyle(stringResource(R.string.orders_list_status_cancelled), Color(0xFFFBE1DE), Color(0xFFB3261E))
    else -> CourierStatusStyle(status, RunGoLightField, RunGoLightTextSecondary)
}

@Composable
fun CourierOrdersScreen(
    uiState: CourierOrdersUiState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onToggleAvailability: () -> Unit = {},
    onTakeOrder: (Int) -> Unit = {},
    onOrderClick: (OrderDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(RunGoLightBackground)) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            is CourierOrdersUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is CourierOrdersUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = RunGoLightTextSecondary)
                }
            }

            is CourierOrdersUiState.Success -> {
                var selectedTab by remember { mutableIntStateOf(0) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.nav_orders),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = RunGoLightTextPrimary
                        )
                    }
                    item {
                        AvailabilityToggle(
                            isAvailable = uiState.isAvailable,
                            updating = uiState.updatingAvailability,
                            onClick = onToggleAvailability
                        )
                    }
                    item {
                        CourierTabSelector(selectedTab = selectedTab, onSelect = { selectedTab = it })
                    }

                    if (selectedTab == 0) {
                        if (uiState.availableOrders.isEmpty() && uiState.activeOrders.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.orders_empty),
                                    color = RunGoLightTextSecondary,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                        if (uiState.availableOrders.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.courier_section_available),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RunGoLightTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(uiState.availableOrders, key = { it.id }) { order ->
                                AvailableOrderCard(
                                    order = order,
                                    taking = order.id in uiState.takingOrderIds,
                                    onDetailsClick = { onOrderClick(order) },
                                    onTakeClick = { onTakeOrder(order.id) }
                                )
                            }
                        }
                        if (uiState.activeOrders.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.courier_section_active),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RunGoLightTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(uiState.activeOrders, key = { it.id }) { order ->
                                CourierOrderRow(order = order, onClick = { onOrderClick(order) })
                            }
                        }
                    } else {
                        if (uiState.historyOrders.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.courier_no_history_orders),
                                    color = RunGoLightTextSecondary,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                        items(uiState.historyOrders, key = { it.id }) { order ->
                            CourierOrderRow(order = order, onClick = { onOrderClick(order) })
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun AvailabilityToggle(isAvailable: Boolean, updating: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !updating, onClick = onClick),
        color = if (isAvailable) OnlineContainer else OfflineContainer,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isAvailable) OnlineColor else RunGoLightTextSecondary)
            )
            Text(
                text = stringResource(if (isAvailable) R.string.courier_online else R.string.courier_offline),
                color = if (isAvailable) OnlineColor else RunGoLightTextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            )
            if (updating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = if (isAvailable) OnlineColor else RunGoLightTextPrimary
                )
            }
        }
    }
}

@Composable
private fun CourierTabSelector(selectedTab: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(R.string.courier_tab_available, R.string.courier_tab_history)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEachIndexed { index, labelRes ->
            val selected = index == selectedTab
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) },
                color = if (selected) RunGoBrandOrange else RunGoLightField,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(labelRes),
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
private fun AvailableOrderCard(
    order: OrderDto,
    taking: Boolean,
    onDetailsClick: () -> Unit,
    onTakeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoLightField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.order_number, order.id),
                color = RunGoLightTextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (!order.serviceName.isNullOrBlank()) {
                Text(text = order.serviceName, color = RunGoLightAccentText, style = MaterialTheme.typography.bodyMedium)
            }
            if (!order.deliveryAddress.isNullOrBlank()) {
                Text(
                    text = order.deliveryAddress,
                    color = RunGoLightTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatOrderAmount(order.codTotal, order.currency),
                    color = RunGoLightAccentText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )
                OutlinedButton(
                    onClick = onDetailsClick,
                    enabled = !taking,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RunGoLightTextPrimary),
                    border = BorderStroke(1.dp, RunGoLightTextSecondary)
                ) {
                    Text(stringResource(R.string.common_details))
                }
                Button(
                    onClick = onTakeClick,
                    enabled = !taking,
                    colors = ButtonDefaults.buttonColors(containerColor = RunGoBrandOrange)
                ) {
                    if (taking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RunGoOnBrandOrange, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.courier_take_button), color = RunGoOnBrandOrange)
                    }
                }
            }
        }
    }
}

@Composable
private fun CourierOrderRow(order: OrderDto, onClick: () -> Unit) {
    val style = courierStatusStyle(order.status)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = RunGoLightField,
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
                    .background(RunGoBrandOrange)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.order_number, order.id),
                        color = RunGoLightTextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!order.serviceName.isNullOrBlank()) {
                        Text(
                            text = " · " + order.serviceName,
                            color = RunGoLightAccentText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!order.deliveryAddress.isNullOrBlank()) {
                    Text(
                        text = order.deliveryAddress,
                        color = RunGoLightTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            StatusBadge(
                label = style.label,
                container = style.container,
                content = style.content,
                pulse = order.status == "in_progress" || order.status == "in_delivery",
                textStyle = MaterialTheme.typography.labelSmall,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = RunGoLightTextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

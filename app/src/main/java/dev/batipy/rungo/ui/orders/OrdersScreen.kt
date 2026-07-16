package dev.batipy.rungo.ui.orders

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.ui.common.StatusBadge
import dev.batipy.rungo.ui.common.formatOrderAmount
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoLightAccentText
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoLightField
import dev.batipy.rungo.ui.theme.RunGoLightTextPrimary
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private fun orderDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.getDefault())

private data class StatusStyle(val label: String, val container: Color, val content: Color)

@Composable
private fun statusStyle(status: String, light: Boolean = false): StatusStyle = if (light) when (status) {
    "new" -> StatusStyle(stringResource(R.string.orders_list_status_new), Color(0xFFEDEAE3), Color(0xFF4A4438))
    "confirmed" -> StatusStyle(stringResource(R.string.orders_list_status_confirmed), Color(0xFFE4ECFB), Color(0xFF2E4A73))
    "in_progress" -> StatusStyle(stringResource(R.string.orders_list_status_in_progress), Color(0xFFFFF0C7), Color(0xFF7A5416))
    "in_delivery" -> StatusStyle(stringResource(R.string.orders_list_status_in_delivery), Color(0xFFFFF0C7), Color(0xFF7A5416))
    "delivered" -> StatusStyle(stringResource(R.string.orders_list_status_delivered), Color(0xFFCFF7D9), Color(0xFF1B7A3A))
    "cancelled" -> StatusStyle(stringResource(R.string.orders_list_status_cancelled), Color(0xFFFBE1DE), Color(0xFFB3261E))
    else -> StatusStyle(status, RunGoLightField, RunGoLightTextSecondary)
} else when (status) {
    "new" -> StatusStyle(stringResource(R.string.orders_list_status_new), Color(0xFF3A4657), Color(0xFFD7E3F5))
    "confirmed" -> StatusStyle(stringResource(R.string.orders_list_status_confirmed), Color(0xFF2E4A73), Color(0xFFBFD9FF))
    "in_progress" -> StatusStyle(stringResource(R.string.orders_list_status_in_progress), Color(0xFF6B5420), Color(0xFFFFE1A6))
    "in_delivery" -> StatusStyle(stringResource(R.string.orders_list_status_in_delivery), Color(0xFF6B5420), Color(0xFFFFE1A6))
    "delivered" -> StatusStyle(stringResource(R.string.orders_list_status_delivered), Color(0xFFCFF7D9), Color(0xFF1B7A3A))
    "cancelled" -> StatusStyle(stringResource(R.string.orders_list_status_cancelled), Color(0xFF6B2A2A), Color(0xFFFFC2C2))
    else -> StatusStyle(status, RunGoField, RunGoTextSecondary)
}

private fun formatOrderDate(iso: String): String = try {
    OffsetDateTime.parse(iso).format(orderDateFormatter())
} catch (e: DateTimeParseException) {
    iso
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    uiState: OrdersUiState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onOrderClick: (OrderDto) -> Unit = {},
    light: Boolean = false,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(if (light) RunGoLightBackground else Color.Unspecified)
    ) {
        when (uiState) {
            is OrdersUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is OrdersUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = if (light) RunGoLightTextSecondary else RunGoTextSecondary)
                }
            }

            is OrdersUiState.Success -> {
                if (uiState.orders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.orders_empty), color = if (light) RunGoLightTextSecondary else RunGoTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.orders_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (light) RunGoLightTextPrimary else Color.Unspecified
                            )
                        }
                        items(uiState.orders) { order ->
                            OrderCard(order, onClick = { onOrderClick(order) }, light = light)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderDto, onClick: () -> Unit, light: Boolean = false) {
    val style = statusStyle(order.status, light)
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (light) RunGoLightField else RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.order_number, order.id),
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                StatusBadge(
                    label = style.label,
                    container = style.container,
                    content = style.content,
                    pulse = order.status == "in_progress" || order.status == "in_delivery"
                )
            }
            if (order.cityName.isNotBlank()) {
                Text(
                    text = order.cityName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = formatOrderDate(order.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Text(
                    text = formatOrderAmount(order.codTotal, order.currency),
                    color = if (light) RunGoLightAccentText else RunGoAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

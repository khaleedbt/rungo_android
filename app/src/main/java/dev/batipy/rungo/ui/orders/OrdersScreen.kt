package dev.batipy.rungo.ui.orders

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
import dev.batipy.rungo.ui.common.formatOrderAmount
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val OrderDateFormatter = DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale("ru"))

private data class StatusStyle(val label: String, val container: Color, val content: Color)

@Composable
private fun statusStyle(status: String): StatusStyle = when (status) {
    "new" -> StatusStyle(stringResource(R.string.orders_list_status_new), Color(0xFF3A4657), Color(0xFFD7E3F5))
    "confirmed" -> StatusStyle(stringResource(R.string.orders_list_status_confirmed), Color(0xFF2E4A73), Color(0xFFBFD9FF))
    "in_progress" -> StatusStyle(stringResource(R.string.orders_list_status_in_progress), Color(0xFF6B5420), Color(0xFFFFE1A6))
    "in_delivery" -> StatusStyle(stringResource(R.string.orders_list_status_in_delivery), Color(0xFF6B5420), Color(0xFFFFE1A6))
    "delivered" -> StatusStyle(stringResource(R.string.orders_list_status_delivered), Color(0xFFCFF7D9), Color(0xFF1B7A3A))
    "cancelled" -> StatusStyle(stringResource(R.string.orders_list_status_cancelled), Color(0xFF6B2A2A), Color(0xFFFFC2C2))
    else -> StatusStyle(status, RunGoField, RunGoTextSecondary)
}

private fun formatOrderDate(iso: String): String = try {
    OffsetDateTime.parse(iso).format(OrderDateFormatter)
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
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when (uiState) {
            is OrdersUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is OrdersUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = RunGoTextSecondary)
                }
            }

            is OrdersUiState.Success -> {
                if (uiState.orders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.orders_empty), color = RunGoTextSecondary)
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(uiState.orders) { order ->
                            OrderCard(order, onClick = { onOrderClick(order) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderDto, onClick: () -> Unit) {
    val style = statusStyle(order.status)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = RunGoField,
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
                    color = RunGoTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(color = style.container, shape = RoundedCornerShape(50)) {
                    Text(
                        text = style.label,
                        color = style.content,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (order.cityName.isNotBlank()) {
                Text(
                    text = order.cityName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RunGoTextSecondary,
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
                        tint = RunGoTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = formatOrderDate(order.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = RunGoTextSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Text(
                    text = formatOrderAmount(order.codTotal, order.currency),
                    color = RunGoAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

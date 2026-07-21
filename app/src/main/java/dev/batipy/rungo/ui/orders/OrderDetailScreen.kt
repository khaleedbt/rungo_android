package dev.batipy.rungo.ui.orders

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.OrderDetailDto
import dev.batipy.rungo.data.network.dto.ReviewDto
import dev.batipy.rungo.ui.common.ElapsedTimeText
import dev.batipy.rungo.ui.common.StatusBadge
import dev.batipy.rungo.ui.common.formatOrderAmount
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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val ErrorColor = Color(0xFFFF6B6B)
private val ErrorColorLight = Color(0xFFB3261E)
private val SuccessColor = Color(0xFF4CAF6D)
private val SuccessColorLight = Color(0xFF1B7A3A)
private val BalanceColor = Color(0xFF5B3E8C)
private fun detailDateFormatter(): DateTimeFormatter {
    val locale = Locale.getDefault()
    val pattern = when (locale.language) {
        "ru" -> "d MMMM 'в' HH:mm"
        "ar" -> "d MMMM HH:mm"
        else -> "d MMMM 'at' HH:mm"
    }
    return DateTimeFormatter.ofPattern(pattern, locale)
}

private val deliverySteps = listOf("new", "confirmed", "in_progress", "in_delivery", "delivered")

// Server only allows cancelling while status is "new" (rejects with 400 once
// a courier is assigned/confirmed): "Отменить можно только заказ в статусе «новый»."
private val cancellableStatuses = setOf("new")

private data class StatusPillStyle(val label: String, val container: Color, val content: Color)

@Composable
private fun stepLabel(status: String): String = when (status) {
    "new" -> stringResource(R.string.order_status_new)
    "confirmed" -> stringResource(R.string.order_status_confirmed)
    "in_progress" -> stringResource(R.string.order_status_in_progress)
    "in_delivery" -> stringResource(R.string.order_status_in_delivery)
    "delivered" -> stringResource(R.string.order_status_delivered)
    else -> status
}

@Composable
private fun statusPillStyle(status: String, light: Boolean = false): StatusPillStyle = if (light) when (status) {
    "new" -> StatusPillStyle(stringResource(R.string.order_status_new), Color(0xFFEDEAE3), Color(0xFF4A4438))
    "confirmed" -> StatusPillStyle(stringResource(R.string.order_status_confirmed), Color(0xFFE4ECFB), Color(0xFF2E4A73))
    "in_progress" -> StatusPillStyle(stringResource(R.string.order_status_in_progress), Color(0xFFFFF0C7), Color(0xFF7A5416))
    "in_delivery" -> StatusPillStyle(stringResource(R.string.order_status_in_delivery), Color(0xFFFFF0C7), Color(0xFF7A5416))
    "delivered" -> StatusPillStyle(stringResource(R.string.order_status_delivered), Color(0xFFCFF7D9), Color(0xFF1B7A3A))
    "cancelled" -> StatusPillStyle(stringResource(R.string.order_status_cancelled), Color(0xFFFBE1DE), Color(0xFFB3261E))
    else -> StatusPillStyle(status, RunGoLightField, RunGoLightTextSecondary)
} else when (status) {
    "new" -> StatusPillStyle(stringResource(R.string.order_status_new), Color(0xFF3A4657), Color(0xFFD7E3F5))
    "confirmed" -> StatusPillStyle(stringResource(R.string.order_status_confirmed), Color(0xFF2E4A73), Color(0xFFBFD9FF))
    "in_progress" -> StatusPillStyle(stringResource(R.string.order_status_in_progress), Color(0xFF6B5420), Color(0xFFFFE1A6))
    "in_delivery" -> StatusPillStyle(stringResource(R.string.order_status_in_delivery), Color(0xFF6B5420), Color(0xFFFFE1A6))
    "delivered" -> StatusPillStyle(stringResource(R.string.order_status_delivered), Color(0xFFCFF7D9), Color(0xFF1B7A3A))
    "cancelled" -> StatusPillStyle(stringResource(R.string.order_status_cancelled), Color(0xFF6B2A2A), Color(0xFFFFC2C2))
    else -> StatusPillStyle(status, RunGoField, RunGoTextSecondary)
}

@Composable
private fun paymentMethodLabel(method: String?): String = when (method) {
    "shamcash" -> stringResource(R.string.payment_method_shamcash)
    "balance" -> stringResource(R.string.payment_method_balance)
    else -> stringResource(R.string.payment_method_cash)
}

/**
 * The stored balance is in USD, so a non-USD order total needs converting
 * before comparison. If the rate for that currency wasn't available (failed
 * fetch), we can't verify it — treat as insufficient rather than risk letting
 * the user pick an option that fails server-side.
 */
private fun hasSufficientBalance(
    order: OrderDetailDto,
    userBalance: String?,
    exchangeRates: Map<String, Double>
): Boolean {
    val total = (order.codTotal ?: order.serviceFee)?.toDoubleOrNull() ?: return false
    val balance = userBalance?.toDoubleOrNull() ?: return false
    val totalInUsd = when (order.currency) {
        "usd" -> total
        else -> {
            val rate = exchangeRates[order.currency] ?: return false
            if (rate <= 0.0) return false
            total / rate
        }
    }
    return balance >= totalInUsd
}

private fun formatDetailDate(iso: String): String = try {
    OffsetDateTime.parse(iso).format(detailDateFormatter())
} catch (e: DateTimeParseException) {
    iso
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    uiState: OrderDetailUiState,
    message: String?,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onConsumeMessage: () -> Unit,
    onBack: () -> Unit,
    onCancelOrder: () -> Unit,
    onRequestConfirmDelivery: () -> Unit,
    onCancelConfirmDelivery: () -> Unit,
    onConfirmDelivery: (String) -> Unit,
    onSelectRating: (Int) -> Unit,
    onReviewTextChange: (String) -> Unit,
    onSubmitReview: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenTracking: () -> Unit = {},
    light: Boolean = false,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val onAccent = if (light) RunGoOnBrandOrange else Color.White
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    val errorColor = if (light) ErrorColorLight else ErrorColor
    val successColor = if (light) SuccessColorLight else SuccessColor

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onConsumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (light) RunGoLightBackground else Color.Unspecified)) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is OrderDetailUiState.Loading -> {
                HeaderBar(title = stringResource(R.string.order_detail_title), subtitle = null, statusStyle = null, onBack = onBack, light = light)
            }

            is OrderDetailUiState.Error -> {
                HeaderBar(title = stringResource(R.string.order_detail_title), subtitle = null, statusStyle = null, onBack = onBack, light = light)
            }

            is OrderDetailUiState.Success -> {
                val order = uiState.order
                val isTerminal = order.status == "delivered" || order.status == "cancelled"
                HeaderBar(
                    title = stringResource(R.string.order_number, order.id),
                    subtitle = formatDetailDate(order.createdAt),
                    statusStyle = statusPillStyle(order.status, light),
                    statusPulse = order.status == "in_progress" || order.status == "in_delivery",
                    elapsedStartIso = order.createdAt,
                    elapsedEndIso = if (isTerminal) order.updatedAt else null,
                    onBack = onBack,
                    light = light
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
        when (uiState) {
            is OrderDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is OrderDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = textSecondary)
                }
            }

            is OrderDetailUiState.Success -> {
                val order = uiState.order
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Chat only makes sense once a courier is actually on the
                    // job (in_progress/in_delivery) — before that there's no
                    // one to write to, and it should disappear again once the
                    // order is finished (backend closes the chat too).
                    if (order.status == "in_progress" || order.status == "in_delivery") {
                        item {
                            Button(
                                onClick = onOpenChat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = onAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.chat_open_button),
                                    color = onAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = onOpenTracking,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = null,
                                    tint = onAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.order_detail_track_button),
                                    color = onAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    if (order.status != "cancelled") {
                        item {
                            SectionCard(title = stringResource(R.string.section_delivery_status), light = light) {
                                DeliveryStepper(currentStatus = order.status, light = light)
                            }
                        }
                    }
                    item {
                        SectionCard(title = null, light = light) {
                            Column {
                                if (order.serviceName != null) {
                                    InfoRow(stringResource(R.string.label_service), order.serviceName, light)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                if (order.cityName != null) {
                                    InfoRow(stringResource(R.string.label_city), order.cityName, light)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                if (order.deliveryAddress != null) {
                                    InfoRow(stringResource(R.string.label_address), order.deliveryAddress, light)
                                }
                                val courierName = order.courierDisplayName
                                if (courierName != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    InfoRow(stringResource(R.string.label_courier), courierName, light)
                                }
                            }
                        }
                    }
                    if (order.items.isNotEmpty()) {
                        item {
                            SectionCard(title = stringResource(R.string.section_items), light = light) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    order.items.forEachIndexed { index, orderItem ->
                                        if (index > 0) {
                                            HorizontalDivider()
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${orderItem.productName} ×${orderItem.quantity}",
                                                color = textPrimary
                                            )
                                            Text(
                                                text = formatOrderAmount(orderItem.price, order.currency),
                                                color = textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SectionCard(title = stringResource(R.string.section_payment), light = light) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.service_fee_label), color = textSecondary)
                                    Text(
                                        text = formatOrderAmount(order.serviceFee ?: order.codTotal, order.currency),
                                        color = textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = stringResource(R.string.payment_cod_total_label), color = textSecondary, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = formatOrderAmount(order.codTotal, order.currency),
                                        color = if (light) RunGoLightAccentText else RunGoAccent,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = paymentMethodLabel(order.paymentMethod),
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    if (order.status == "delivered") {
                        item {
                            val review = order.review
                            if (review != null) {
                                SectionCard(title = stringResource(R.string.section_review_given), light = light) {
                                    ReviewSummary(review, light)
                                }
                            } else {
                                SectionCard(title = stringResource(R.string.section_review_form), light = light) {
                                    ReviewForm(
                                        rating = uiState.reviewRating,
                                        text = uiState.reviewText,
                                        submitting = uiState.submittingReview,
                                        onSelectRating = onSelectRating,
                                        onTextChange = onReviewTextChange,
                                        onSubmit = onSubmitReview,
                                        light = light
                                    )
                                }
                            }
                        }
                    }
                    if (order.status == "in_delivery") {
                        if (uiState.showingPaymentPicker) {
                            item {
                                SectionCard(title = null, light = light) {
                                    PaymentMethodPicker(
                                        userBalance = uiState.userBalance,
                                        balanceSufficient = hasSufficientBalance(
                                            order = order,
                                            userBalance = uiState.userBalance,
                                            exchangeRates = uiState.exchangeRates
                                        ),
                                        confirming = uiState.confirming,
                                        onSelect = onConfirmDelivery,
                                        onCancel = onCancelConfirmDelivery,
                                        light = light
                                    )
                                }
                            }
                        } else {
                            item {
                                Button(
                                    onClick = onRequestConfirmDelivery,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.buttonColors(containerColor = successColor)
                                ) {
                                    Text(stringResource(R.string.order_received_button), fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                    if (order.status in cancellableStatuses) {
                        item {
                            OutlinedButton(
                                onClick = onCancelOrder,
                                enabled = !uiState.cancelling,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                border = BorderStroke(1.dp, errorColor),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = errorColor)
                            ) {
                                if (uiState.cancelling) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = errorColor, strokeWidth = 2.dp)
                                } else {
                                    Text(stringResource(R.string.order_cancel_button), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HeaderBar(
    title: String,
    subtitle: String?,
    statusStyle: StatusPillStyle?,
    statusPulse: Boolean = false,
    elapsedStartIso: String? = null,
    elapsedEndIso: String? = null,
    onBack: () -> Unit,
    light: Boolean = false
) {
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(if (light) RunGoLightField else RunGoField, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.common_back),
                tint = textPrimary
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (elapsedStartIso != null) {
                ElapsedTimeText(
                    startIso = elapsedStartIso,
                    endIso = elapsedEndIso,
                    color = textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (statusStyle != null) {
            StatusBadge(
                label = statusStyle.label,
                container = statusStyle.container,
                content = statusStyle.content,
                pulse = statusPulse
            )
        }
    }
}

@Composable
private fun SectionCard(title: String?, light: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (light) RunGoLightField else RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    color = if (light) RunGoLightTextSecondary else RunGoTextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, light: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = if (light) RunGoLightTextSecondary else RunGoTextSecondary)
        Text(text = value, color = if (light) RunGoLightTextPrimary else RunGoTextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeliveryStepper(currentStatus: String, light: Boolean = false) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val onAccent = if (light) RunGoOnBrandOrange else Color.White
    val fieldColor = if (light) RunGoLightField else RunGoField
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    val currentIndex = deliverySteps.indexOfFirst { it == currentStatus }.coerceAtLeast(0)

    val activeStepPulse = rememberInfiniteTransition(label = "activeStepPulse")
    val pulseAlpha by activeStepPulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by activeStepPulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        deliverySteps.forEachIndexed { index, status ->
            val label = stepLabel(status)
            // The final step is a terminal state, not "in progress" — treat it
            // as completed (checkmark, no pulse) rather than the active step.
            val completed = index < currentIndex || (index == currentIndex && index == deliverySteps.lastIndex)
            val active = index == currentIndex && index != deliverySteps.lastIndex
            val circleColor by animateColorAsState(
                targetValue = if (completed || active) accent else fieldColor,
                label = "stepCircleColor"
            )
            val labelColor by animateColorAsState(
                targetValue = if (completed || active) accent else textSecondary,
                label = "stepLabelColor"
            )
            Column(
                modifier = Modifier.weight(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .then(
                            if (active) {
                                Modifier
                                    .scale(pulseScale)
                                    .border(2.dp, accent.copy(alpha = pulseAlpha), CircleShape)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(circleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (completed) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = onAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = if (active) onAccent else textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = label,
                    color = labelColor,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (index != deliverySteps.lastIndex) {
                val lineColor by animateColorAsState(
                    targetValue = if (index < currentIndex) accent else textSecondary.copy(alpha = 0.3f),
                    label = "stepLineColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 18.dp)
                        .height(2.dp)
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
private fun ReviewForm(
    rating: Int,
    text: String,
    submitting: Boolean,
    onSelectRating: (Int) -> Unit,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    light: Boolean = false
) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val onAccent = if (light) RunGoOnBrandOrange else Color.White
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (star in 1..5) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.rating_star_desc, star),
                    tint = if (star <= rating) Color(0xFFFFC107) else textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onSelectRating(star) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.review_comment_placeholder), color = if (light) textSecondary else RunGoPlaceholder) },
            minLines = 2,
            colors = if (light) OutlinedTextFieldDefaults.colors(
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
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSubmit,
            enabled = !submitting && rating > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            if (submitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = onAccent, strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.review_submit_button), color = onAccent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PaymentMethodPicker(
    userBalance: String?,
    balanceSufficient: Boolean,
    confirming: Boolean,
    onSelect: (String) -> Unit,
    onCancel: () -> Unit,
    light: Boolean = false
) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val onAccent = if (light) RunGoOnBrandOrange else Color.White
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    val successColor = if (light) SuccessColorLight else SuccessColor
    Column {
        Text(
            text = stringResource(R.string.choose_payment_method_title),
            color = textSecondary,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        PaymentMethodButton(
            label = stringResource(R.string.payment_cash_button),
            color = successColor,
            contentColor = Color.White,
            enabled = !confirming,
            light = light,
            onClick = { onSelect("cash") }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PaymentMethodButton(
            label = stringResource(R.string.payment_shamcash_button),
            color = accent,
            contentColor = onAccent,
            enabled = !confirming,
            light = light,
            onClick = { onSelect("shamcash") }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PaymentMethodButton(
            label = if (userBalance != null) {
                stringResource(R.string.payment_balance_button_with_amount, userBalance)
            } else {
                stringResource(R.string.payment_balance_button)
            },
            color = BalanceColor,
            contentColor = Color.White,
            enabled = !confirming && balanceSufficient,
            light = light,
            onClick = { onSelect("balance") }
        )
        if (!balanceSufficient) {
            Text(
                text = stringResource(R.string.payment_insufficient_balance),
                color = textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onCancel,
            enabled = !confirming,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.large,
            // Same reasoning as PaymentMethodButton above — spell out the
            // disabled content color instead of letting it fall back to one
            // derived from the app's permanently-dark MaterialTheme.
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = textSecondary,
                disabledContentColor = textSecondary.copy(alpha = 0.5f)
            )
        ) {
            Text(stringResource(R.string.common_cancel))
        }
        if (confirming) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PaymentMethodButton(
    label: String,
    color: Color,
    contentColor: Color = Color.White,
    enabled: Boolean,
    light: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = MaterialTheme.shapes.large,
        // Disabled colors need to be spelled out explicitly here — left to
        // their Material3 defaults they're derived from MaterialTheme's
        // colorScheme, which (per this app's light-theme-via-per-composable-
        // overrides approach) stays permanently dark even on a light screen.
        // That made a disabled button here render as a near-invisible pale
        // container with barely-visible text on a light card — any color
        // emoji in the label still showed (glyph color ignores text tint),
        // which is what made it look like an empty white box with a stray
        // 💰 floating in it instead of a legibly "disabled" button.
        colors = if (light) {
            ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = contentColor,
                disabledContainerColor = RunGoLightSurfaceMuted,
                disabledContentColor = RunGoLightTextSecondary
            )
        } else {
            ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor)
        }
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, color = if (enabled) contentColor else LocalContentColor.current)
    }
}

@Composable
private fun ReviewSummary(review: ReviewDto, light: Boolean = false) {
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (star in 1..5) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (star <= review.rating) Color(0xFFFFC107) else textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (review.text.isNotBlank()) {
            Text(
                text = review.text,
                color = textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

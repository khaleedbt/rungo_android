package dev.batipy.rungo.ui.courier

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.OrderDetailDto
import dev.batipy.rungo.ui.common.StatusBadge
import dev.batipy.rungo.ui.common.formatOrderAmount
import dev.batipy.rungo.ui.theme.RunGoBrandOrange
import dev.batipy.rungo.ui.theme.RunGoLightAccentText
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoLightField
import dev.batipy.rungo.ui.theme.RunGoLightTextPrimary
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoOnBrandOrange

// Darker than the dark-theme originals — same reasoning as ErrorColor there,
// but tuned to stay readable on the light background instead of the dark one.
private val ErrorColor = Color(0xFFB3261E)
private val SuccessColor = Color(0xFF1B7A3A)

@Composable
private fun openMaps(latitude: String?, longitude: String?, onNoMapsApp: () -> Unit): () -> Unit {
    val context = LocalContext.current
    return {
        if (latitude != null && longitude != null) {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: ActivityNotFoundException) {
                onNoMapsApp()
            }
        } else {
            onNoMapsApp()
        }
    }
}

@Composable
fun CourierOrderDetailScreen(
    uiState: CourierOrderDetailUiState,
    message: String?,
    onConsumeMessage: () -> Unit,
    onBack: () -> Unit,
    onTakeOrder: () -> Unit,
    onMarkInDelivery: () -> Unit,
    onReleaseOrder: () -> Unit,
    onCollectPayment: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onConsumeMessage()
        }
    }

    // CourierLocationService (started once the order is in_progress/in_delivery
    // — see CourierOrderDetailViewModel) needs this granted before it can do
    // anything; ask for it as soon as the courier is looking at a delivery
    // that actually needs tracking, rather than waiting for them to stumble
    // into it via the profile screen.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op either way — tracking just won't start if denied */ }
    val trackedStatus = (uiState as? CourierOrderDetailUiState.Success)?.order?.status
    LaunchedEffect(trackedStatus) {
        if (trackedStatus == "in_progress" || trackedStatus == "in_delivery") {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(RunGoLightBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        .background(RunGoLightField, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.common_back),
                        tint = RunGoLightTextPrimary
                    )
                }
                val successState = uiState as? CourierOrderDetailUiState.Success
                Text(
                    text = if (successState != null) stringResource(R.string.order_number, successState.order.id) else "",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = RunGoLightTextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                if (successState != null) {
                    val style = courierStatusStyle(successState.order.status)
                    StatusBadge(
                        label = style.label,
                        container = style.container,
                        content = style.content,
                        pulse = successState.order.status == "in_progress" || successState.order.status == "in_delivery"
                    )
                }
            }

            when (uiState) {
                is CourierOrderDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is CourierOrderDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.message, color = RunGoLightTextSecondary)
                    }
                }

                is CourierOrderDetailUiState.Success -> {
                    CourierOrderDetailContent(
                        order = uiState.order,
                        performingAction = uiState.performingAction,
                        onTakeOrder = onTakeOrder,
                        onMarkInDelivery = onMarkInDelivery,
                        onReleaseOrder = onReleaseOrder,
                        onCollectPayment = onCollectPayment,
                        onOpenChat = onOpenChat
                    )
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
private fun CourierOrderDetailContent(
    order: OrderDetailDto,
    performingAction: Boolean,
    onTakeOrder: () -> Unit,
    onMarkInDelivery: () -> Unit,
    onReleaseOrder: () -> Unit,
    onCollectPayment: () -> Unit,
    onOpenChat: () -> Unit
) {
    var showCollectPaymentConfirm by remember { mutableStateOf(false) }
    val noMapsMessage = stringResource(R.string.courier_navigate_error)
    val context = LocalContext.current

    if (showCollectPaymentConfirm) {
        AlertDialog(
            onDismissRequest = { showCollectPaymentConfirm = false },
            title = { Text(stringResource(R.string.courier_collect_payment_button)) },
            text = { Text(stringResource(R.string.courier_collect_payment_confirm)) },
            confirmButton = {
                Button(onClick = {
                    showCollectPaymentConfirm = false
                    onCollectPayment()
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCollectPaymentConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Same rule as the client side: only while a courier is actively
            // on the job — before "confirmed" is taken, or after delivery.
            if (order.status == "in_progress" || order.status == "in_delivery") {
                item {
                    Button(
                        onClick = onOpenChat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = RunGoBrandOrange)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = RunGoOnBrandOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.chat_open_button),
                            color = RunGoOnBrandOrange,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            if (!order.pickupAddress.isNullOrBlank()) {
                item {
                    AddressCard(
                        label = stringResource(R.string.courier_pickup_address_label),
                        address = order.pickupAddress,
                        onNavigate = openMaps(order.pickupLatitude, order.pickupLongitude) {
                            openInBrowserFallback(context, noMapsMessage)
                        }
                    )
                }
            }
            if (!order.deliveryAddress.isNullOrBlank()) {
                item {
                    AddressCard(
                        label = stringResource(R.string.courier_delivery_address_label),
                        address = order.deliveryAddress,
                        onNavigate = openMaps(order.deliveryLatitude, order.deliveryLongitude) {
                            openInBrowserFallback(context, noMapsMessage)
                        }
                    )
                }
            }
            if (!order.clientName.isNullOrBlank() || !order.clientPhone.isNullOrBlank()) {
                item {
                    ClientCard(name = order.clientName, phone = order.clientPhone)
                }
            }
            if (!order.distanceKm.isNullOrBlank()) {
                item {
                    Text(
                        text = stringResource(R.string.courier_distance_label, order.distanceKm),
                        color = RunGoLightTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (!order.comment.isNullOrBlank()) {
                item {
                    SectionCard(title = stringResource(R.string.courier_comment_label)) {
                        Text(text = order.comment, color = RunGoLightTextPrimary)
                    }
                }
            }
            item {
                PaymentCard(order = order)
            }
            item {
                CourierActionButtons(
                    order = order,
                    performingAction = performingAction,
                    onTakeOrder = onTakeOrder,
                    onMarkInDelivery = onMarkInDelivery,
                    onReleaseOrder = onReleaseOrder,
                    onCollectPaymentRequested = { showCollectPaymentConfirm = true }
                )
            }
        }
    }
}

private fun openInBrowserFallback(context: android.content.Context, message: String) {
    // No maps app installed — nothing sensible to fall back to besides a toast.
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
private fun AddressCard(label: String, address: String, onNavigate: () -> Unit) {
    SectionCard(title = label) {
        Column {
            Text(text = address, color = RunGoLightTextPrimary)
            OutlinedButton(
                onClick = onNavigate,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RunGoLightTextPrimary),
                border = BorderStroke(1.dp, RunGoLightTextSecondary),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.courier_navigate_button))
            }
        }
    }
}

@Composable
private fun ClientCard(name: String?, phone: String?) {
    val context = LocalContext.current
    SectionCard(title = stringResource(R.string.client_info_title)) {
        Column {
            if (!name.isNullOrBlank()) {
                Text(text = name, color = RunGoLightTextPrimary, fontWeight = FontWeight.SemiBold)
            }
            if (!phone.isNullOrBlank()) {
                Text(text = phone, color = RunGoLightTextSecondary, modifier = Modifier.padding(top = 2.dp))
                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse("tel:$phone")
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                        } catch (e: ActivityNotFoundException) {
                            // No dialer app — nothing to do.
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RunGoLightTextPrimary),
                    border = BorderStroke(1.dp, RunGoLightTextSecondary),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.courier_call_button))
                }
            }
        }
    }
}

@Composable
private fun PaymentCard(order: OrderDetailDto) {
    SectionCard(title = stringResource(R.string.courier_payment_label)) {
        Column {
            Text(
                text = formatOrderAmount(order.codTotal, order.currency),
                color = RunGoLightAccentText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            val codTotal = order.codTotal?.toDoubleOrNull() ?: 0.0
            if (codTotal > 0) {
                Text(
                    text = stringResource(
                        if (order.paymentCollected) R.string.courier_payment_collected else R.string.courier_payment_pending
                    ),
                    color = if (order.paymentCollected) SuccessColor else RunGoLightTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CourierActionButtons(
    order: OrderDetailDto,
    performingAction: Boolean,
    onTakeOrder: () -> Unit,
    onMarkInDelivery: () -> Unit,
    onReleaseOrder: () -> Unit,
    onCollectPaymentRequested: () -> Unit
) {
    val codTotal = order.codTotal?.toDoubleOrNull() ?: 0.0
    Column {
        when (order.status) {
            "confirmed" -> {
                PrimaryActionButton(
                    text = stringResource(R.string.courier_take_button),
                    enabled = !performingAction,
                    loading = performingAction,
                    onClick = onTakeOrder
                )
            }

            "in_progress" -> {
                PrimaryActionButton(
                    text = stringResource(R.string.courier_in_delivery_button),
                    enabled = !performingAction,
                    loading = performingAction,
                    onClick = onMarkInDelivery
                )
                OutlinedButton(
                    onClick = onReleaseOrder,
                    enabled = !performingAction,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RunGoLightTextPrimary),
                    border = BorderStroke(1.dp, RunGoLightTextSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.courier_release_button))
                }
            }

            "in_delivery" -> {
                if (codTotal > 0 && !order.paymentCollected) {
                    PrimaryActionButton(
                        text = stringResource(R.string.courier_collect_payment_button),
                        enabled = !performingAction,
                        loading = performingAction,
                        onClick = onCollectPaymentRequested
                    )
                } else {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    Text(
                        text = stringResource(R.string.courier_waiting_client_confirmation),
                        color = RunGoLightTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            "delivered" -> {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    text = stringResource(R.string.courier_order_delivered_note),
                    color = SuccessColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            "cancelled" -> {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    text = stringResource(R.string.courier_order_cancelled_note),
                    color = ErrorColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(text: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(containerColor = RunGoBrandOrange)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = RunGoOnBrandOrange, strokeWidth = 2.dp)
        } else {
            Text(text, color = RunGoOnBrandOrange, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoLightField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = RunGoLightTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

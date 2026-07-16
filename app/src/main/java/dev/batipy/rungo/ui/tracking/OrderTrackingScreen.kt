package dev.batipy.rungo.ui.tracking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import dev.batipy.rungo.R
import dev.batipy.rungo.ui.common.ElapsedTimeText
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoBrandOrange
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoLightField
import dev.batipy.rungo.ui.theme.RunGoLightTextPrimary
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoOnBrandOrange
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary

// Some orders (mainly store/shop ones with a free-text address) don't carry
// delivery coordinates at all, and the courier hasn't pinged yet right when
// the screen opens — without a fallback the camera never gets positioned
// anywhere and the map just sits on its blank default canvas forever. This
// is a reasonable center of the service area to fall back to so there's
// always *something* real on screen while waiting for a better point.
private val FallbackRegionCenter = LatLng(35.9306, 36.6339)

@Composable
fun OrderTrackingScreen(
    orderId: Int,
    uiState: OrderTrackingUiState,
    onBack: () -> Unit,
    light: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = if (light) RunGoBrandOrange else RunGoAccent
    val onAccent = if (light) RunGoOnBrandOrange else Color.White
    val fieldColor = if (light) RunGoLightField else RunGoField
    val textPrimary = if (light) RunGoLightTextPrimary else RunGoTextPrimary
    val textSecondary = if (light) RunGoLightTextSecondary else RunGoTextSecondary
    Column(modifier = modifier.fillMaxSize().background(if (light) RunGoLightBackground else Color.Unspecified)) {
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
                    .background(fieldColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.common_back),
                    tint = textPrimary
                )
            }
            Text(
                text = stringResource(R.string.tracking_screen_title, orderId),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = textPrimary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        when (uiState) {
            is OrderTrackingUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is OrderTrackingUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = textSecondary)
                }
            }

            is OrderTrackingUiState.Success -> {
                val destination = if (uiState.destinationLatitude != null && uiState.destinationLongitude != null) {
                    LatLng(uiState.destinationLatitude, uiState.destinationLongitude)
                } else null
                val pickup = if (uiState.pickupLatitude != null && uiState.pickupLongitude != null) {
                    LatLng(uiState.pickupLatitude, uiState.pickupLongitude)
                } else null
                val courierPosition = if (uiState.courierLatitude != null && uiState.courierLongitude != null) {
                    LatLng(uiState.courierLatitude, uiState.courierLongitude)
                } else null

                val cameraPositionState = rememberCameraPositionState()

                LaunchedEffect(courierPosition, destination, pickup) {
                    val (target, zoom) = when {
                        courierPosition != null -> courierPosition to 15f
                        destination != null -> destination to 13f
                        pickup != null -> pickup to 13f
                        else -> FallbackRegionCenter to 6f
                    }
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, zoom))
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Edge-to-edge, no inset/rounding on the map itself — the
                    // bottom sheet below is what carries the "designed" look
                    // (rounded top corners + drag handle), matching the
                    // map-plus-sheet layout pattern the reference used.
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState
                    ) {
                        destination?.let {
                            Marker(
                                state = rememberMarkerState(position = it),
                                title = stringResource(R.string.tracking_destination_label),
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                        courierPosition?.let {
                            MarkerComposable(
                                state = rememberMarkerState(position = it),
                                title = uiState.courierName ?: stringResource(R.string.tracking_courier_label)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = accent,
                                    border = BorderStroke(2.dp, Color.White),
                                    shadowElevation = 4.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TwoWheeler,
                                        contentDescription = null,
                                        tint = onAccent,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = fieldColor,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        shadowElevation = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(bottom = 20.dp)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 10.dp, bottom = 4.dp)
                                    .size(width = 36.dp, height = 4.dp)
                                    .background(textSecondary.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            )
                            if (courierPosition == null) {
                                Text(
                                    text = stringResource(R.string.tracking_waiting_for_courier),
                                    color = textSecondary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                                ) {
                                    Surface(shape = CircleShape, color = accent) {
                                        Icon(
                                            imageVector = Icons.Filled.TwoWheeler,
                                            contentDescription = null,
                                            tint = onAccent,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text(
                                            text = uiState.courierName ?: stringResource(R.string.tracking_courier_label),
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = stringResource(R.string.tracking_courier_status),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary
                                        )
                                        ElapsedTimeText(
                                            startIso = uiState.orderCreatedAt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

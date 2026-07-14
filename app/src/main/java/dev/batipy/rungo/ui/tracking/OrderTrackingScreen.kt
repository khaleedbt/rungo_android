package dev.batipy.rungo.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import dev.batipy.rungo.R
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary

@Composable
fun OrderTrackingScreen(
    orderId: Int,
    uiState: OrderTrackingUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
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
                    .background(RunGoField, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.common_back),
                    tint = RunGoTextPrimary
                )
            }
            Text(
                text = stringResource(R.string.tracking_screen_title, orderId),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = RunGoTextPrimary,
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
                    Text(text = uiState.message, color = RunGoTextSecondary)
                }
            }

            is OrderTrackingUiState.Success -> {
                val destination = if (uiState.destinationLatitude != null && uiState.destinationLongitude != null) {
                    LatLng(uiState.destinationLatitude, uiState.destinationLongitude)
                } else null
                val courierPosition = if (uiState.courierLatitude != null && uiState.courierLongitude != null) {
                    LatLng(uiState.courierLatitude, uiState.courierLongitude)
                } else null

                val cameraPositionState = rememberCameraPositionState()

                LaunchedEffect(courierPosition, destination) {
                    val target = courierPosition ?: destination ?: return@LaunchedEffect
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f))
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                            Marker(
                                state = rememberMarkerState(position = it),
                                title = uiState.courierName ?: stringResource(R.string.tracking_courier_label),
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                    }

                    if (courierPosition == null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            color = RunGoField,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = stringResource(R.string.tracking_waiting_for_courier),
                                color = RunGoTextSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

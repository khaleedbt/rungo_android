package dev.batipy.rungo.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.CityDto
import dev.batipy.rungo.ui.login.AnimatedLogoMark
import dev.batipy.rungo.ui.theme.RunGoBrandOrange
import dev.batipy.rungo.ui.theme.RunGoLightBackground
import dev.batipy.rungo.ui.theme.RunGoLightSurfaceMuted
import dev.batipy.rungo.ui.theme.RunGoLightTextPrimary
import dev.batipy.rungo.ui.theme.RunGoLightTextSecondary
import dev.batipy.rungo.ui.theme.RunGoOnBrandOrange

private val ErrorColor = Color(0xFFB3261E)

@Composable
fun RegisterScreen(
    uiState: RegisterUiState = RegisterUiState.Idle,
    cities: List<CityDto> = emptyList(),
    onRegisterClick: (username: String, password: String, password2: String, fullName: String, phone: String, cityId: Int?) -> Unit = { _, _, _, _, _, _ -> },
    onLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var selectedCityId by remember { mutableStateOf<Int?>(null) }
    val isLoading = uiState is RegisterUiState.Loading

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RunGoLightBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedLogoMark(size = 72.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = RunGoLightTextPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.login_username_placeholder), color = RunGoLightTextSecondary) },
            singleLine = true,
            enabled = !isLoading,
            shape = MaterialTheme.shapes.large,
            colors = registerFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.register_fullname_placeholder), color = RunGoLightTextSecondary) },
            singleLine = true,
            enabled = !isLoading,
            shape = MaterialTheme.shapes.large,
            colors = registerFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.register_phone_placeholder), color = RunGoLightTextSecondary) },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = MaterialTheme.shapes.large,
            colors = registerFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        RegisterCityDropdown(
            cities = cities,
            selectedCityId = selectedCityId,
            enabled = !isLoading,
            onCitySelect = { selectedCityId = it }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.login_password_placeholder), color = RunGoLightTextSecondary) },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.large,
            colors = registerFieldColors()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password2,
            onValueChange = { password2 = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.register_password2_placeholder), color = RunGoLightTextSecondary) },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.large,
            colors = registerFieldColors()
        )

        if (uiState is RegisterUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.message,
                color = ErrorColor,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { onRegisterClick(username, password, password2, fullName, phone, selectedCityId) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = RunGoBrandOrange)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = RunGoOnBrandOrange,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(R.string.register_submit_button), fontWeight = FontWeight.SemiBold, color = RunGoOnBrandOrange)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.register_login_link),
            color = RunGoBrandOrange,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onLoginClick)
        )
    }
}

@Composable
private fun RegisterCityDropdown(
    cities: List<CityDto>,
    selectedCityId: Int?,
    enabled: Boolean,
    onCitySelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val selectedName = cities.find { it.id == selectedCityId }?.name
        ?: stringResource(R.string.city_dropdown_placeholder)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { fieldWidthPx = it.width }
                .clickable(enabled = enabled) { expanded = true },
            color = RunGoLightSurfaceMuted,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedName,
                    color = if (selectedCityId == null) RunGoLightTextSecondary else RunGoLightTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = RunGoLightTextSecondary)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { fieldWidthPx.toDp() }),
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        ) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name, color = RunGoLightTextPrimary) },
                    onClick = {
                        onCitySelect(city.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun registerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = RunGoLightSurfaceMuted,
    unfocusedContainerColor = RunGoLightSurfaceMuted,
    disabledContainerColor = RunGoLightSurfaceMuted,
    focusedBorderColor = RunGoBrandOrange,
    unfocusedBorderColor = RunGoLightTextSecondary.copy(alpha = 0.4f),
    focusedTextColor = RunGoLightTextPrimary,
    unfocusedTextColor = RunGoLightTextPrimary
)

package dev.batipy.rungo.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.UserDto
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary

private val ErrorColor = Color(0xFFFF6B6B)

private fun roleLabel(role: String) = when (role) {
    "client" -> "Клиент"
    "operator" -> "Оператор"
    "courier" -> "Курьер"
    "admin" -> "Администратор"
    "partner" -> "Партнёр"
    else -> role
}

private data class LanguageOption(val code: String, val label: String, val flag: String)

private val languageOptions = listOf(
    LanguageOption("ru", "Русский", "🇷🇺"),
    LanguageOption("en", "English", "🇬🇧"),
    LanguageOption("ar", "العربية", "🇸🇾")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    message: String?,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onConsumeMessage: () -> Unit,
    onDeleteLocation: (Int) -> Unit,
    onRequestLocation: () -> Unit,
    onLanguageSelect: (String) -> Unit,
    onSendSupportMessage: (String) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onConsumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
        when (uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = RunGoTextSecondary)
                }
            }

            is ProfileUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Профиль",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item { UserCard(uiState.user) }
                    item { BalanceCard(uiState.user.balance) }
                    item {
                        LocationsCard(
                            locations = uiState.locations,
                            onDeleteLocation = onDeleteLocation,
                            onRequestLocation = onRequestLocation
                        )
                    }
                    item {
                        LanguageCard(
                            selectedLang = uiState.user.lang,
                            onLanguageSelect = onLanguageSelect
                        )
                    }
                    item { SupportRow(onSendSupportMessage) }
                    item {
                        OutlinedButton(
                            onClick = onLogoutClick,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, ErrorColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor)
                        ) {
                            Text("Выйти")
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
private fun UserCard(user: UserDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCE7FB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color(0xFF4A5568),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = user.fullName.ifBlank { user.username },
                        fontWeight = FontWeight.Bold,
                        color = RunGoTextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        color = RunGoAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = roleLabel(user.role),
                            color = RunGoAccent,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProfileFieldRow("Логин", user.username)
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            ProfileFieldRow("Имя", user.fullName)
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            ProfileFieldRow("Город", user.cityName)
        }
    }
}

@Composable
private fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = RunGoTextSecondary)
        Text(text = value, color = RunGoTextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BalanceCard(balance: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Баланс", color = RunGoTextSecondary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = RunGoAccent
                )
                Text(
                    text = balance,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    color = RunGoTextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LocationsCard(
    locations: List<LocationDto>,
    onDeleteLocation: (Int) -> Unit,
    onRequestLocation: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Моя геолокация", color = RunGoTextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            locations.forEachIndexed { index, location ->
                LocationRow(
                    location = location,
                    index = index,
                    onDelete = { onDeleteLocation(location.id) },
                    onOpenMap = {
                        val uri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}")
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (e: ActivityNotFoundException) {
                            // No maps app installed — silently ignore.
                        }
                    }
                )
                if (index != locations.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("📍 Отправить геолокацию через бот", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LocationRow(
    location: LocationDto,
    index: Int,
    onDelete: () -> Unit,
    onOpenMap: () -> Unit
) {
    Surface(
        color = RunGoAccent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📍")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .clickable(onClick = onOpenMap)
            ) {
                Text(
                    text = location.label.ifBlank { "Локация ${index + 1}" },
                    color = RunGoAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${location.latitude}, ${location.longitude}",
                    color = RunGoTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Удалить",
                    tint = ErrorColor
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
    selectedLang: String,
    onLanguageSelect: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Язык", color = RunGoTextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languageOptions.forEach { option ->
                    val selected = option.code == selectedLang
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLanguageSelect(option.code) },
                        color = if (selected) RunGoAccent else RunGoField,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = option.flag, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = option.label,
                                color = if (selected) Color.White else RunGoTextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportRow(onSendSupportMessage: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RunGoField,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = RunGoTextSecondary
                    )
                    Text(
                        text = "Написать оператору",
                        color = RunGoTextPrimary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = RunGoTextSecondary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Сообщение") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = RunGoField,
                        unfocusedContainerColor = RunGoField
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSendSupportMessage(text)
                        text = ""
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Отправить")
                }
            }
        }
    }
}

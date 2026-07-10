package dev.batipy.rungo.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.ChatMessageDto
import dev.batipy.rungo.ui.theme.RunGoAccent
import dev.batipy.rungo.ui.theme.RunGoBackground
import dev.batipy.rungo.ui.theme.RunGoField
import dev.batipy.rungo.ui.theme.RunGoPlaceholder
import dev.batipy.rungo.ui.theme.RunGoTextPrimary
import dev.batipy.rungo.ui.theme.RunGoTextSecondary
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private fun formatChatTime(iso: String): String = try {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
} catch (e: DateTimeParseException) {
    ""
}

@Composable
private fun roleLabel(role: String): String = when (role) {
    "client" -> stringResource(R.string.role_client)
    "courier" -> stringResource(R.string.role_courier)
    "operator" -> stringResource(R.string.role_operator)
    "admin" -> stringResource(R.string.role_admin)
    "partner" -> stringResource(R.string.role_partner)
    else -> role
}

@Composable
fun ChatScreen(
    orderId: Int,
    uiState: ChatUiState,
    currentUserId: Int,
    message: String?,
    onConsumeMessage: () -> Unit,
    onBack: () -> Unit,
    onSend: (String) -> Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var draftText by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onConsumeMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
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
                        .background(RunGoField, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.common_back),
                        tint = RunGoTextPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.chat_title, orderId),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = RunGoTextPrimary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            when (uiState) {
                is ChatUiState.Connecting -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is ChatUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = uiState.message, color = RunGoTextSecondary)
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                Text(stringResource(R.string.common_retry), color = Color.White)
                            }
                        }
                    }
                }

                is ChatUiState.Ready -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        reverseLayout = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages.asReversed(), key = { it.id }) { msg ->
                            ChatBubble(msg, isMine = msg.sender == currentUserId)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.chat_input_placeholder), color = RunGoPlaceholder) },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = RunGoBackground,
                                unfocusedContainerColor = RunGoBackground,
                                focusedTextColor = RunGoTextPrimary,
                                unfocusedTextColor = RunGoTextPrimary
                            )
                        )
                        IconButton(
                            onClick = {
                                val text = draftText
                                if (text.isNotBlank()) {
                                    draftText = ""
                                    if (!onSend(text)) draftText = text
                                }
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(44.dp)
                                .background(RunGoAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.chat_send_button),
                                tint = Color.White
                            )
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
private fun ChatBubble(msg: ChatMessageDto, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            if (!isMine) {
                Text(
                    text = "${msg.senderName} · ${roleLabel(msg.senderRole)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RunGoTextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Surface(
                color = if (isMine) RunGoAccent else RunGoField,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 16.dp
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = msg.text,
                        color = if (isMine) Color.White else RunGoTextPrimary
                    )
                    Text(
                        text = formatChatTime(msg.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.7f) else RunGoTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

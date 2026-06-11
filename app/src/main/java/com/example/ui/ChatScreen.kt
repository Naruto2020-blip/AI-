package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ApiEngineConfig
import com.example.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Chat Resiliente", "Motores (Ajustes)")

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val engines by viewModel.engines.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val autoFallback by viewModel.autoFallback.collectAsStateWithLifecycle()
    val fallbackLogs by viewModel.fallbackLogs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "MultiAI Assistant",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_$index")
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> ChatTabContent(
                    sessions = sessions,
                    activeSessionId = activeSessionId,
                    messages = messages,
                    isSending = isSending,
                    fallbackLogs = fallbackLogs,
                    engines = engines,
                    onSessionCreated = { viewModel.createNewSession() },
                    onSessionSelected = { viewModel.selectSession(it) },
                    onSessionDeleted = { viewModel.deleteSession(it) },
                    onSendMessage = { viewModel.sendMessage(it) },
                    onClearLogs = { viewModel.clearLogs() }
                )
                1 -> SettingsTabContent(
                    engines = engines,
                    autoFallback = autoFallback,
                    onToggleFallback = { viewModel.toggleFallback(it) },
                    onUpdateEngine = { viewModel.updateEngineConfig(it) },
                    onClearAllHistory = { viewModel.clearAllHistory() },
                    onAddLog = { viewModel.addManualLog(it) }
                )
            }
        }
    }
}

@Composable
fun ChatTabContent(
    sessions: List<com.example.data.model.ChatSession>,
    activeSessionId: Long?,
    messages: List<ChatMessage>,
    isSending: Boolean,
    fallbackLogs: List<String>,
    engines: List<ApiEngineConfig>,
    onSessionCreated: () -> Unit,
    onSessionSelected: (Long) -> Unit,
    onSessionDeleted: (Long) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto scroll to bottom when messages change
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Session Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chats:",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSessionCreated() }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        "Crear nuevo chat...",
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    reverseLayout = false,
                    state = rememberLazyListState()
                ) {
                    // We use an alternative horizontal scrollable Row inside the weights
                }
                
                // Let's use a standard horizontal Row of chips inside a horizontal LazyRow!
                // To keep Code compile safe, we can use a Box containing a beautiful scrollable row of chips.
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(sessions) { session ->
                        val isSelected = session.id == activeSessionId
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onSessionSelected(session.id) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.title,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                            if (sessions.size > 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Borrar chat",
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onSessionDeleted(session.id) }
                                )
                            }
                        }
                    }
                }
            }

            IconButton(
                onClick = onSessionCreated,
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo Chat",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Message List / Empty State
        if (activeSessionId == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Vacío",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "¡Bienvenido a MultiAI!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Presiona el botón '+' de arriba para iniciar una nueva conversación respaldada por enrutamiento automático.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (messages.isEmpty() && !isSending) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Chat Vacío",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Inicia la conversación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Escribe tu consulta abajo. Si tu motor de IA predeterminado falla, el sistema intentará las otras API configuradas automáticamente.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message, engines = engines)
                }

                if (isSending && (messages.isEmpty() || messages.last().status != "sending")) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "AI está analizando tu respuesta...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Live Fallback System Console
        if (fallbackLogs.isNotEmpty()) {
            ConsolePanel(logs = fallbackLogs, onClear = onClearLogs)
        }

        // Send Input Bar (Disabled if no active session)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(if (activeSessionId == null) "Crea un chat primero" else "Pregúntame lo que quieras...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                enabled = activeSessionId != null && !isSending,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isSending) {
                            onSendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    }
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isSending) {
                        onSendMessage(inputText)
                        inputText = ""
                        keyboardController?.hide()
                    }
                },
                enabled = activeSessionId != null && inputText.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (inputText.isNotBlank() && !isSending && activeSessionId != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        CircleShape
                    )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (inputText.isNotBlank() && !isSending && activeSessionId != null) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, engines: List<ApiEngineConfig>) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        if (message.status == "error") MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        if (message.status == "error") MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSecondaryContainer
    }

    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    val formattedTime = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                if (!isUser && message.engineUsed != null) {
                    val displayName = engines.find { it.engineId == message.engineUsed }?.displayName ?: message.engineUsed
                    Text(
                        text = "⚡ Resuelto por $displayName",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 9.sp,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                    if (!isUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = when(message.status) {
                                "sending" -> Icons.Default.Refresh
                                "error" -> Icons.Default.Warning
                                else -> Icons.Default.Check
                            },
                            contentDescription = message.status,
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConsolePanel(logs: List<String>, onClear: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151821)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Logs",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Enrutador de IA Activo (${logs.size} eventos)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                Button(
                    onClick = onClear,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Limpiar", fontSize = 8.sp, color = Color.White)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    imageVector = if (expanded) Icons.Default.Star else Icons.Default.List,
                    contentDescription = "Expandir",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expanded || logs.size == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .background(Color.Black)
                        .padding(6.dp)
                ) {
                    logs.takeLast(7).forEach { log ->
                        Text(
                            text = log,
                            color = if (log.contains("❌") || log.contains("[FALLO]")) Color(0xFFE57373)
                            else if (log.contains("⚡") || log.contains("[ÉXITO]")) Color(0xFF81C784)
                            else Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTabContent(
    engines: List<ApiEngineConfig>,
    autoFallback: Boolean,
    onToggleFallback: (Boolean) -> Unit,
    onUpdateEngine: (ApiEngineConfig) -> Unit,
    onClearAllHistory: () -> Unit,
    onAddLog: (String) -> Unit
) {
    var expandedEngineId by remember { mutableStateOf<String?>(null) }
    var showDialogDeleteHistory by remember { mutableStateOf(false) }

    if (showDialogDeleteHistory) {
        AlertDialog(
            onDismissRequest = { showDialogDeleteHistory = false },
            title = { Text("Eliminar Historial") },
            text = { Text("¿Estás seguro de que deseas borrar todas las conversaciones e historiales de inmediato? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllHistory()
                        showDialogDeleteHistory = false
                        onAddLog("🧹 Historial de chats y logs limpiado por completo.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Borrar Todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogDeleteHistory = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "¿Cómo funciona la Resiliencia de IA?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Para no quedarte nunca sin saldo, configuras múltiples motores. Al enviar tu mensaje, si el primer motor falla (límite de créditos, caída de servidor, etc), se enruta instantáneamente e intenta con los siguientes de forma automática y transparente.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Auto Fallback global switch
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Auto-Fallback en Redes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Interconectar múltiples API de respaldo en secuencia si falla la principal.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoFallback,
                    onCheckedChange = {
                        onToggleFallback(it)
                        onAddLog("⚙️ Sistema auto-fallback cambiado a: " + if(it) "ACTIVADO" else "DESACTIVADO")
                    },
                    modifier = Modifier.testTag("fallback_switch")
                )
            }
        }

        item {
            Text(
                "Motores de IA Disponibles",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Render each Engine config
        items(engines) { config ->
            val isExpanded = expandedEngineId == config.engineId
            EngineConfigCard(
                config = config,
                isExpanded = isExpanded,
                onHeaderClick = {
                    expandedEngineId = if (isExpanded) null else config.engineId
                },
                onUpdate = onUpdateEngine
            )
        }

        // Quick Maintenance functions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showDialogDeleteHistory = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("delete_all_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpiar Base de Datos Completa")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Resilient AI Router v1.0 • Desarrollado para crédito flexible continuo",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EngineConfigCard(
    config: ApiEngineConfig,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    onUpdate: (ApiEngineConfig) -> Unit
) {
    var apiKeyText by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var modelText by remember(config.modelName) { mutableStateOf(config.modelName) }
    var urlText by remember(config.apiBaseUrl) { mutableStateOf(config.apiBaseUrl) }
    var priorityValue by remember(config.priority) { mutableStateOf(config.priority) }
    var keyVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (config.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isEnabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title, Enable switch & expand indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (config.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = priorityValue.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (config.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = config.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (config.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stat: Éxitos: ${config.successCount} | Fallas: ${config.failureCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { onUpdate(config.copy(isEnabled = it)) },
                    modifier = Modifier.testTag("switch_${config.engineId}")
                )
            }

            if (config.lastErrorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Último Error: ${config.lastErrorMessage}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // API Key
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("Llave API (Token)") },
                        placeholder = {
                            if (config.engineId == "gemini") {
                                Text("Infiltrado automáticamente si está vacío")
                            } else {
                                Text("Llave API de proveedor")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.Star else Icons.Default.Warning,
                                    contentDescription = "Ver Llave"
                                )
                            }
                        }
                    )

                    // Model Name
                    OutlinedTextField(
                        value = modelText,
                        onValueChange = { modelText = it },
                        label = { Text("Nombre de Modelo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Base URL (if openrouter or huggingface or custom)
                    if (config.engineId != "gemini") {
                        OutlinedTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            label = { Text("URL Base Final") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Priority Weight & Save button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Prioridad: ", fontSize = 13.sp)
                            IconButton(
                                onClick = { if (priorityValue > 1) priorityValue-- },
                                enabled = priorityValue > 1
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text(
                                priorityValue.toString(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { if (priorityValue < 9) priorityValue++ },
                                enabled = priorityValue < 9
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }

                        Button(
                            onClick = {
                                onUpdate(
                                    config.copy(
                                        apiKey = apiKeyText,
                                        modelName = modelText,
                                        apiBaseUrl = urlText,
                                        priority = priorityValue
                                    )
                                )
                                onHeaderClick() // Collapse card after saving changes
                            },
                            modifier = Modifier.testTag("save_${config.engineId}")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Guardar")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar Cambios")
                        }
                    }
                    
                    // Specific dynamic documentation tips
                    val supportMessage = when (config.engineId) {
                        "gemini" -> "💡 Tip: Si dejas la clave vacía, Google AI Studio inyectará la de tu servidor para que chatees de forma predeterminada."
                        "huggingface" -> "💡 Tip: ¡La API de Hugging Face es 100% libre! Funciona sin clave, pero añadiendo un Token de cuenta gratuito (hf_...) previene límites."
                        "groq" -> "💡 Tip: Groq Cloud es súper rápida y libre para desarrolladores. Consigue tu clave gratis en console.groq.com."
                        "openrouter" -> "💡 Tip: OpenRouter agrupa cientos de modelos gratis. Añade una clave y selecciona modelos terminados en ':free'."
                        else -> ""
                    }
                    if (supportMessage.isNotEmpty()) {
                        Text(
                            text = supportMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// Extensions removed for clean compile-safe Material 3 operation

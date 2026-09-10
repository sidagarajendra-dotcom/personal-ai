package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NavTab
import com.example.ui.PersonalAiViewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SessionDrawerSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PersonalAiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                PersonalAiApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PersonalAiApp(viewModel: PersonalAiViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == NavTab.CHAT,
                    onClick = { viewModel.setTab(NavTab.CHAT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubble,
                            contentDescription = "Chat",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_item_chat")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == NavTab.MEMORY,
                    onClick = { viewModel.setTab(NavTab.MEMORY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = "AI Memory",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("AI Memory", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_item_memory")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == NavTab.SETTINGS,
                    onClick = { viewModel.setTab(NavTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Settings",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Settings", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    NavTab.CHAT -> {
                        ChatScreen(
                            uiState = uiState,
                            onSendMessage = { prompt -> viewModel.sendMessage(prompt) },
                            onOpenSessionDrawer = { viewModel.toggleSessionDrawer(true) },
                            onNewChat = { viewModel.createNewSession() },
                            onToggleSpeak = { id, text -> viewModel.toggleSpeakMessage(id, text) },
                            onStartVoiceInput = { callback -> viewModel.startVoiceInput(callback) },
                            onStopVoiceInput = { viewModel.stopVoiceInput() },
                            onClearSpeechError = { viewModel.clearSpeechError() }
                        )
                    }

                    NavTab.MEMORY -> {
                        MemoryScreen(
                            memories = uiState.memories,
                            assistantName = uiState.assistantName,
                            onAddMemory = { fact, category -> viewModel.addMemory(fact, category) },
                            onDeleteMemory = { memory -> viewModel.deleteMemory(memory) }
                        )
                    }

                    NavTab.SETTINGS -> {
                        SettingsScreen(
                            assistantName = uiState.assistantName,
                            currentPersona = uiState.defaultPersona,
                            customApiKey = uiState.customApiKey,
                            temperature = uiState.temperature,
                            onUpdateName = { viewModel.updateAssistantName(it) },
                            onUpdatePersona = { viewModel.updateDefaultPersona(it) },
                            onUpdateApiKey = { viewModel.updateCustomApiKey(it) },
                            onUpdateTemperature = { viewModel.updateTemperature(it) },
                            onClearAllChats = { viewModel.clearAllChats() },
                            onClearAllMemories = { viewModel.clearAllMemories() }
                        )
                    }
                }
            }
        }
    }

    // Sessions Drawer Bottom Sheet
    if (uiState.showSessionDrawer) {
        SessionDrawerSheet(
            sessions = uiState.sessions,
            currentSessionId = uiState.currentSession?.id,
            onSelectSession = { id -> viewModel.selectSession(id) },
            onNewSession = { title, persona -> viewModel.createNewSession(title, persona) },
            onRenameSession = { session, title -> viewModel.updateSessionTitle(session, title) },
            onDeleteSession = { session -> viewModel.deleteSession(session) },
            onDismiss = { viewModel.toggleSessionDrawer(false) }
        )
    }
}

// Retain Greeting for preview and unit testing
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

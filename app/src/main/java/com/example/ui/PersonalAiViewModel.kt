package com.example.ui

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MemoryCategory
import com.example.data.model.PersonalMemory
import com.example.data.model.PersonaType
import com.example.data.remote.GeminiService
import com.example.data.repository.PersonalAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class NavTab(val label: String, val iconName: String) {
    CHAT("Chat", "ChatBubble"),
    MEMORY("AI Memory", "Psychology"),
    SETTINGS("Settings", "Tune")
}

data class PersonalAiUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSession: ChatSession? = null,
    val messages: List<ChatMessage> = emptyList(),
    val memories: List<PersonalMemory> = emptyList(),
    val activeTab: NavTab = NavTab.CHAT,
    val isGenerating: Boolean = false,
    val assistantName: String = "Aura",
    val defaultPersona: PersonaType = PersonaType.BALANCED,
    val customApiKey: String = "",
    val temperature: Float = 0.7f,
    val speakingMessageId: Long? = null,
    val showSessionDrawer: Boolean = false,
    val isListeningSpeech: Boolean = false,
    val speechError: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PersonalAiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PersonalAiRepository

    private val _activeTab = MutableStateFlow(NavTab.CHAT)
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private val _isGenerating = MutableStateFlow(false)
    private val _assistantName = MutableStateFlow("Aura")
    private val _defaultPersona = MutableStateFlow(PersonaType.BALANCED)
    private val _customApiKey = MutableStateFlow("")
    private val _temperature = MutableStateFlow(0.7f)
    private val _speakingMessageId = MutableStateFlow<Long?>(null)
    private val _showSessionDrawer = MutableStateFlow(false)
    private val _isListeningSpeech = MutableStateFlow(false)
    private val _speechError = MutableStateFlow<String?>(null)

    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = PersonalAiRepository(
            chatDao = database.chatDao(),
            memoryDao = database.memoryDao(),
            geminiService = GeminiService()
        )

        // Initialize TTS
        initTts(application)

        // Initialize session
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    _currentSessionId.value = sessions.first().id
                } else if (sessions.isEmpty()) {
                    // Create first session if none exists
                    val newId = repository.createNewSession("Welcome & Getting Started", PersonaType.BALANCED)
                    _currentSessionId.value = newId
                }
            }
        }
    }

    private fun initTts(application: Application) {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _speakingMessageId.value = null
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _speakingMessageId.value = null
                    }
                })
            }
        }
    }

    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<PersonalMemory>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMessages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSession: StateFlow<ChatSession?> = combine(
        sessions,
        _currentSessionId
    ) { allSessions, currentId ->
        allSessions.find { it.id == currentId } ?: allSessions.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<PersonalAiUiState> = combine(
        sessions,
        currentSession,
        currentMessages,
        memories,
        _activeTab,
        _isGenerating,
        _assistantName,
        _defaultPersona,
        _customApiKey,
        _temperature,
        _speakingMessageId,
        _showSessionDrawer,
        _isListeningSpeech,
        _speechError
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        PersonalAiUiState(
            sessions = args[0] as List<ChatSession>,
            currentSession = args[1] as? ChatSession,
            messages = args[2] as List<ChatMessage>,
            memories = args[3] as List<PersonalMemory>,
            activeTab = args[4] as NavTab,
            isGenerating = args[5] as Boolean,
            assistantName = args[6] as String,
            defaultPersona = args[7] as PersonaType,
            customApiKey = args[8] as String,
            temperature = args[9] as Float,
            speakingMessageId = args[10] as? Long,
            showSessionDrawer = args[11] as Boolean,
            isListeningSpeech = args[12] as Boolean,
            speechError = args[13] as? String
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PersonalAiUiState()
    )

    fun setTab(tab: NavTab) {
        _activeTab.value = tab
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        _showSessionDrawer.value = false
        stopSpeaking()
    }

    fun toggleSessionDrawer(show: Boolean) {
        _showSessionDrawer.value = show
    }

    fun createNewSession(title: String = "New Conversation", persona: PersonaType? = null) {
        viewModelScope.launch {
            val selectedPersona = persona ?: _defaultPersona.value
            val newId = repository.createNewSession(title, selectedPersona)
            _currentSessionId.value = newId
            _showSessionDrawer.value = false
            _activeTab.value = NavTab.CHAT
        }
    }

    fun updateSessionTitle(session: ChatSession, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(session, newTitle)
        }
    }

    fun updateSessionPersona(session: ChatSession, persona: PersonaType) {
        viewModelScope.launch {
            repository.updateSessionPersona(session, persona)
        }
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            val remaining = sessions.value.filter { it.id != session.id }
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = remaining.firstOrNull()?.id
            }
        }
    }

    fun sendMessage(userText: String) {
        val text = userText.trim()
        if (text.isBlank() || _isGenerating.value) return

        val session = currentSession.value ?: return

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repository.sendMessage(
                    sessionId = session.id,
                    userText = text,
                    session = session,
                    currentMessages = currentMessages.value,
                    memories = memories.value,
                    customApiKey = _customApiKey.value.ifBlank { null },
                    temperature = _temperature.value
                )
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun addMemory(fact: String, category: MemoryCategory) {
        if (fact.isBlank()) return
        viewModelScope.launch {
            repository.addMemory(fact.trim(), category.name)
        }
    }

    fun deleteMemory(memory: PersonalMemory) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearMemories()
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            repository.clearAllHistory()
            val newId = repository.createNewSession("New Conversation", _defaultPersona.value)
            _currentSessionId.value = newId
        }
    }

    fun updateAssistantName(name: String) {
        _assistantName.value = name
    }

    fun updateDefaultPersona(persona: PersonaType) {
        _defaultPersona.value = persona
    }

    fun updateCustomApiKey(apiKey: String) {
        _customApiKey.value = apiKey
    }

    fun updateTemperature(temp: Float) {
        _temperature.value = temp
    }

    fun toggleSpeakMessage(messageId: Long, content: String) {
        if (!ttsInitialized) return

        if (_speakingMessageId.value == messageId) {
            stopSpeaking()
        } else {
            stopSpeaking()
            _speakingMessageId.value = messageId
            // Clean markdown asterisks and hash marks for clean speech
            val cleanSpeech = content
                .replace(Regex("[*#_`~]"), "")
                .replace("⚠️", "Note: ")
                .trim()
            tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, messageId.toString())
        }
    }

    fun stopSpeaking() {
        if (ttsInitialized) {
            tts?.stop()
        }
        _speakingMessageId.value = null
    }

    fun startVoiceInput(onResult: (String) -> Unit) {
        stopSpeaking()
        val context = getApplication<Application>().applicationContext
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechError.value = "Speech recognition is not available on this device."
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListeningSpeech.value = true
                        _speechError.value = null
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListeningSpeech.value = false
                    }
                    override fun onError(error: Int) {
                        _isListeningSpeech.value = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client recognition error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection error"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition issue ($error)"
                        }
                        _speechError.value = errorMsg
                    }
                    override fun onResults(results: Bundle?) {
                        _isListeningSpeech.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            onResult(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to your AI...")
            }
            speechRecognizer?.startListening(intent)
            _isListeningSpeech.value = true
        } catch (e: Exception) {
            _isListeningSpeech.value = false
            _speechError.value = "Unable to start voice input: ${e.localizedMessage}"
        }
    }

    fun stopVoiceInput() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // ignore
        }
        _isListeningSpeech.value = false
    }

    fun clearSpeechError() {
        _speechError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // ignore
        }
    }
}

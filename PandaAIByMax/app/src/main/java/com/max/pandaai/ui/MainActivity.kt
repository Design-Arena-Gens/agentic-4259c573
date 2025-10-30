package com.max.pandaai.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.max.pandaai.R
import com.max.pandaai.ai.AIService
import com.max.pandaai.data.AppDatabase
import com.max.pandaai.data.ChatMessage
import com.max.pandaai.data.ChatRepository
import com.max.pandaai.intent.IntentHandler
import com.max.pandaai.intent.IntentHandler.SmsPayload
import com.max.pandaai.settings.AssistantPreferences
import com.max.pandaai.settings.SettingsActivity
import android.telephony.SmsManager
import java.util.Locale

/**
 * Main conversational surface combining voice capture, AI chat, and smart actions.
 */
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            ChatRepository(AppDatabase.getInstance(applicationContext).chatMessageDao()),
            AIService(),
            AssistantPreferences(applicationContext)
        )
    }

    private val chatAdapter = ChatAdapter()
    private lateinit var preferences: AssistantPreferences
    private lateinit var intentHandler: IntentHandler
    private lateinit var textToSpeech: TextToSpeech
    private var toneGenerator: ToneGenerator? = null
    private var isListening = false
    private var lastSpokenMessageId: Long = -1
    private var pendingCommand: String? = null
    private var pendingCallNumber: String? = null
    private var pendingSmsPayload: SmsPayload? = null

    private lateinit var greetingText: TextView
    private lateinit var listeningStatus: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var pandaLogo: ImageView

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onUserSpeechCaptured(spoken)
            }
        }
        stopListeningState()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, R.string.permission_denied_message, Toast.LENGTH_LONG).show()
        }
    }

    private val commandPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        val callGranted = results[Manifest.permission.CALL_PHONE] == true
        val smsGranted = results[Manifest.permission.SEND_SMS] == true

        pendingCallNumber?.let { number ->
            if (callGranted) {
                confirmCall(number)
            } else {
                intentHandler.openDialer(number)
            }
        }

        pendingSmsPayload?.let { payload ->
            if (smsGranted) {
                confirmAndSendSms(payload)
            } else {
                intentHandler.sendSmsIntent(payload)
            }
        }

        if (!allGranted) {
            Toast.makeText(this, R.string.permission_denied_message, Toast.LENGTH_LONG).show()
        }

        pendingCommand = null
        pendingCallNumber = null
        pendingSmsPayload = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AssistantPreferences(this)
        intentHandler = IntentHandler(this)

        AppCompatDelegate.setDefaultNightMode(
            if (preferences.darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        setContentView(R.layout.activity_main)

        textToSpeech = TextToSpeech(this, this)
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

        setupToolbar()
        bindViews()
        setupRecyclerView()
        observeViewModel()

        updateGreeting()
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        toneGenerator?.release()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.action_privacy -> {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            true
        }
        R.id.action_clear_chat -> {
            confirmClearChat()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun bindViews() {
        greetingText = findViewById(R.id.greetingText)
        listeningStatus = findViewById(R.id.listeningStatus)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        pandaLogo = findViewById(R.id.pandaLogo)

        val micFab: FloatingActionButton = findViewById(R.id.micFab)
        micFab.setOnClickListener { toggleListening() }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.chatRecyclerView)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply { stackFromEnd = true }
            adapter = chatAdapter
        }

        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        })
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    findViewById<RecyclerView>(R.id.chatRecyclerView)
                        .scrollToPosition(messages.size - 1)
                }
            }

            if (messages.isEmpty()) {
                lastSpokenMessageId = -1
            }

            val latest = messages.lastOrNull()
            if (latest != null && latest.sender == ChatMessage.Sender.ASSISTANT && latest.id != lastSpokenMessageId) {
                lastSpokenMessageId = latest.id
                speak(latest.text)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            loadingIndicator.isVisible = loading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun toggleListening() {
        if (isListening) {
            stopListeningState()
        } else {
            ensureSpeechPermissionAndStart()
        }
    }

    private fun ensureSpeechPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startSpeechRecognition()
        } else {
            showPermissionDialog()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_audio_rationale)
            .setPositiveButton(R.string.ok) { _, _ ->
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        setListeningState(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_now))
        }

        if (preferences.listeningSoundEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }

        speechRecognizerLauncher.launch(intent)
    }

    private fun stopListeningState() {
        setListeningState(false)
    }

    private fun setListeningState(listening: Boolean) {
        isListening = listening
        listeningStatus.text = if (listening) getString(R.string.listening) else getString(R.string.tap_to_talk)
        val animation = AnimationUtils.loadAnimation(this, R.anim.glow_pulse)
        if (listening) {
            pandaLogo.startAnimation(animation)
        } else {
            pandaLogo.clearAnimation()
        }
    }

    private fun onUserSpeechCaptured(transcript: String) {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) return

        val lower = trimmed.lowercase(Locale.getDefault())
        var handledNow = false
        val requiredPermissions = mutableListOf<String>()
        val rationales = mutableListOf<String>()

        val callNumber = if (lower.startsWith("call ")) intentHandler.extractPhoneNumber(trimmed) else null
        val smsPayload = intentHandler.extractSmsPayload(trimmed)

        callNumber?.let { number ->
            handledNow = true
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                confirmCall(number)
            } else {
                pendingCallNumber = number
                requiredPermissions += Manifest.permission.CALL_PHONE
                rationales += getString(R.string.permission_call_rationale)
            }
        }

        if (callNumber == null && lower.startsWith("call ")) {
            Toast.makeText(this, R.string.call_number_missing, Toast.LENGTH_LONG).show()
        }

        smsPayload?.let { payload ->
            handledNow = true
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                confirmAndSendSms(payload)
            } else {
                pendingSmsPayload = payload
                requiredPermissions += Manifest.permission.SEND_SMS
            }
        }

        if (smsPayload == null && (lower.startsWith("send sms") || lower.startsWith("send message"))) {
            Toast.makeText(this, R.string.sms_format_hint, Toast.LENGTH_LONG).show()
        }

        if (pendingSmsPayload != null) {
            rationales += getString(R.string.permission_sms_rationale)
        }

        if (requiredPermissions.isNotEmpty()) {
            pendingCommand = trimmed
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(rationales.joinToString(separator = "\n\n"))
                .setPositiveButton(R.string.ok) { _, _ ->
                    commandPermissionLauncher.launch(requiredPermissions.toTypedArray())
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    pendingCallNumber = null
                    pendingSmsPayload = null
                    pendingCommand = null
                }
                .show()
            speak("I just need your permission to do that.")
        }

        val automationHandled = intentHandler.handle(trimmed)
        viewModel.sendUserPrompt(trimmed)

        if (requiredPermissions.isEmpty() && (automationHandled || handledNow)) {
            speak("On it!")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale.getDefault()
            if (textToSpeech.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                textToSpeech.language = locale
            }
            applyVoiceSettings()
        } else {
            Toast.makeText(this, "Text to speech unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyVoiceSettings() {
        textToSpeech.setPitch(preferences.voicePitch)
        textToSpeech.setSpeechRate(preferences.voiceSpeed)
    }

    private fun speak(text: String) {
        applyVoiceSettings()
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    private fun confirmClearChat() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_chat)
            .setMessage("This will delete your chat history. Continue?")
            .setPositiveButton(R.string.clear_chat) { _, _ ->
                viewModel.clearChat()
                Toast.makeText(this, R.string.chat_history_cleared, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateGreeting() {
        val assistantName = preferences.assistantName
        val userName = preferences.userName
        greetingText.text = getString(R.string.assistant_greeting_template, userName, assistantName)
        listeningStatus.text = getString(R.string.tap_to_talk)
    }

    private fun confirmCall(number: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.call_confirmation_title)
            .setMessage(getString(R.string.call_confirmation_message, number))
            .setPositiveButton(R.string.ok) { _, _ -> performPhoneCall(number) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performPhoneCall(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$number") }
                startActivity(intent)
            } catch (ex: SecurityException) {
                intentHandler.openDialer(number)
            }
        } else {
            intentHandler.openDialer(number)
        }
    }

    private fun confirmAndSendSms(payload: SmsPayload) {
        AlertDialog.Builder(this)
            .setTitle(R.string.sms_confirmation_title)
            .setMessage(getString(R.string.sms_confirmation_message, payload.number, payload.message))
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    SmsManager.getDefault().sendTextMessage(payload.number, null, payload.message, null, null)
                    Toast.makeText(this, R.string.sms_sent, Toast.LENGTH_LONG).show()
                } catch (ex: SecurityException) {
                    intentHandler.sendSmsIntent(payload)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        preferences = AssistantPreferences(this)
        intentHandler = IntentHandler(this)
        AppCompatDelegate.setDefaultNightMode(
            if (preferences.darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        updateGreeting()
    }
}

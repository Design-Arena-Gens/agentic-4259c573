package com.max.pandaai.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.max.pandaai.ai.AIService
import com.max.pandaai.data.ChatMessage
import com.max.pandaai.data.ChatRepository
import com.max.pandaai.settings.AssistantPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel keeping UI state reactive and resilient across configuration changes.
 */
class MainViewModel(
    private val repository: ChatRepository,
    private val aiService: AIService,
    private val preferences: AssistantPreferences
) : ViewModel() {

    val messages: LiveData<List<ChatMessage>> = repository.observeMessages().asLiveData()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun clearError() {
        _error.value = null
    }

    fun sendUserPrompt(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            repository.addMessage(
                ChatMessage(
                    text = prompt,
                    timestamp = System.currentTimeMillis(),
                    sender = ChatMessage.Sender.USER
                )
            )
            requestAssistantResponse(prompt)
        }
    }

    private suspend fun requestAssistantResponse(prompt: String) {
        _isLoading.value = true
        try {
            val responseText = withContext(Dispatchers.IO) {
                aiService.generateResponse(prompt, preferences.userName, preferences.assistantName)
            }
            repository.addMessage(
                ChatMessage(
                    text = responseText,
                    timestamp = System.currentTimeMillis(),
                    sender = ChatMessage.Sender.ASSISTANT
                )
            )
        } catch (ex: Exception) {
            _error.value = ex.localizedMessage ?: "Something went wrong"
        } finally {
            _isLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    class Factory(
        private val repository: ChatRepository,
        private val aiService: AIService,
        private val preferences: AssistantPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, aiService, preferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

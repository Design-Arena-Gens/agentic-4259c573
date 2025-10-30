package com.max.pandaai.settings

import android.content.Context
import androidx.preference.PreferenceManager
import com.max.pandaai.R

/**
 * Convenience wrapper around shared preferences for assistant personalization.
 */
class AssistantPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val defaultAssistantName = appContext.getString(R.string.app_name)

    var assistantName: String
        get() = prefs.getString(KEY_ASSISTANT_NAME, defaultAssistantName) ?: defaultAssistantName
        set(value) = prefs.edit().putString(KEY_ASSISTANT_NAME, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Max") ?: "Max"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var voicePitch: Float
        get() = prefs.getString(KEY_VOICE_PITCH, "1.0")?.toFloatOrNull() ?: 1.0f
        set(value) = prefs.edit().putString(KEY_VOICE_PITCH, value.toString()).apply()

    var voiceSpeed: Float
        get() = prefs.getString(KEY_VOICE_SPEED, "1.0")?.toFloatOrNull() ?: 1.0f
        set(value) = prefs.edit().putString(KEY_VOICE_SPEED, value.toString()).apply()

    var listeningSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_LISTENING_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_LISTENING_SOUND, value).apply()

    var darkModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    companion object {
        const val KEY_ASSISTANT_NAME = "assistant_name"
        const val KEY_VOICE_PITCH = "assistant_pitch"
        const val KEY_VOICE_SPEED = "assistant_speed"
        const val KEY_LISTENING_SOUND = "listening_sound"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_USER_NAME = "user_name"
    }
}

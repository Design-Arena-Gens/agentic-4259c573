package com.max.pandaai.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.appbar.MaterialToolbar
import com.max.pandaai.R
import com.max.pandaai.ui.PrivacyPolicyActivity

/**
 * Hosts the preference screen for customizing the assistant persona.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar: MaterialToolbar = findViewById(R.id.settingsToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }

    /**
     * Nested preference fragment encapsulating the settings logic.
     */
    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var assistantPreferences: AssistantPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            assistantPreferences = AssistantPreferences(requireContext())

            initAssistantNamePreference()
            initUserNamePreference()
            initPitchPreference()
            initSpeedPreference()
            initListeningSoundPreference()
            initDarkModePreference()
            initPrivacyPreference()
            initSupportPreference()
        }

        private fun initAssistantNamePreference() {
            findPreference<Preference>(AssistantPreferences.KEY_ASSISTANT_NAME)?.apply {
                summary = assistantPreferences.assistantName
                setOnPreferenceChangeListener { _, newValue ->
                    assistantPreferences.assistantName = newValue.toString()
                    summary = newValue.toString()
                    true
                }
            }
        }

        private fun initUserNamePreference() {
            findPreference<Preference>(AssistantPreferences.KEY_USER_NAME)?.apply {
                summary = assistantPreferences.userName
                setOnPreferenceChangeListener { _, newValue ->
                    assistantPreferences.userName = newValue.toString()
                    summary = newValue.toString()
                    true
                }
            }
        }

        private fun initPitchPreference() {
            findPreference<ListPreference>(AssistantPreferences.KEY_VOICE_PITCH)?.apply {
                value = assistantPreferences.voicePitch.toString()
                summary = entries[findIndexOfValue(value)]
                setOnPreferenceChangeListener { preference, newValue ->
                    val pitch = newValue.toString()
                    assistantPreferences.voicePitch = pitch.toFloat()
                    (preference as ListPreference).summary = preference.entries[preference.findIndexOfValue(pitch)]
                    true
                }
            }
        }

        private fun initSpeedPreference() {
            findPreference<ListPreference>(AssistantPreferences.KEY_VOICE_SPEED)?.apply {
                value = assistantPreferences.voiceSpeed.toString()
                summary = entries[findIndexOfValue(value)]
                setOnPreferenceChangeListener { preference, newValue ->
                    val speed = newValue.toString()
                    assistantPreferences.voiceSpeed = speed.toFloat()
                    (preference as ListPreference).summary = preference.entries[preference.findIndexOfValue(speed)]
                    true
                }
            }
        }

        private fun initListeningSoundPreference() {
            findPreference<SwitchPreferenceCompat>(AssistantPreferences.KEY_LISTENING_SOUND)?.apply {
                isChecked = assistantPreferences.listeningSoundEnabled
                setOnPreferenceChangeListener { _, newValue ->
                    assistantPreferences.listeningSoundEnabled = newValue as Boolean
                    true
                }
            }
        }

        private fun initDarkModePreference() {
            findPreference<SwitchPreferenceCompat>(AssistantPreferences.KEY_DARK_MODE)?.apply {
                isChecked = assistantPreferences.darkModeEnabled
                setOnPreferenceChangeListener { _, newValue ->
                    val enabled = newValue as Boolean
                    assistantPreferences.darkModeEnabled = enabled
                    AppCompatDelegate.setDefaultNightMode(
                        if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                    true
                }
            }
        }

        private fun initPrivacyPreference() {
            findPreference<Preference>("privacy_policy")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), PrivacyPolicyActivity::class.java))
                true
            }
        }

        private fun initSupportPreference() {
            findPreference<Preference>("support")?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@pandaai.app")
                    putExtra(Intent.EXTRA_SUBJECT, "Panda AI by Max Support")
                }
                startActivity(Intent.createChooser(intent, getString(R.string.pref_support)))
                true
            }
        }
    }
}

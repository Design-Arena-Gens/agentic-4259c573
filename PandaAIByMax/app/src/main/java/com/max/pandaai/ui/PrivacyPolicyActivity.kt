package com.max.pandaai.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.max.pandaai.R

/**
 * Displays a short, transparent privacy statement for Play Store compliance.
 */
class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        val toolbar: MaterialToolbar = findViewById(R.id.privacyToolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}

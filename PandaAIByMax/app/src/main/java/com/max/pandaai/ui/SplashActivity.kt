package com.max.pandaai.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.max.pandaai.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Simple animated splash screen that warms up resources before the assistant loads.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<android.widget.ImageView>(R.id.splashLogo)

        // Gentle pulse animation while we warm up services.
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, "scaleX", 0.9f, 1.05f, 1f),
                ObjectAnimator.ofFloat(logo, "scaleY", 0.9f, 1.05f, 1f),
                ObjectAnimator.ofFloat(logo, "alpha", 0.3f, 1f)
            )
            duration = 1200
            start()
        }

        lifecycleScope.launch {
            delay(1500)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}

package com.example.myapplicationv10

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplicationv10.databinding.ActivityMainBinding
import com.example.myapplicationv10.utils.LocaleHelper

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Update language selector UI based on current language
        updateLanguageSelectorUI()

        // Language selector - English
        binding.langEn.setOnClickListener {
            if (LocaleHelper.getLanguage(this) != "en") {
                setAppLanguage("en")
            }
        }

        // Language selector - French
        binding.langFr.setOnClickListener {
            if (LocaleHelper.getLanguage(this) != "fr") {
                setAppLanguage("fr")
            }
        }

        // Start button
        binding.startButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateLanguageSelectorUI() {
        val currentLanguage = LocaleHelper.getLanguage(this)

        if (currentLanguage == "en") {
            binding.langEn.setTextColor(getColor(R.color.black))
            binding.langEn.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.langFr.setTextColor(getColor(R.color.gray_icon))
            binding.langFr.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            binding.langFr.setTextColor(getColor(R.color.black))
            binding.langFr.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.langEn.setTextColor(getColor(R.color.gray_icon))
            binding.langEn.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun setAppLanguage(languageCode: String) {
        LocaleHelper.setLocale(this, languageCode)
        // Recreate activity to apply language change
        recreate()
    }
}
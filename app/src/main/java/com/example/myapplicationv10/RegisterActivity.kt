package com.example.myapplicationv10

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapplicationv10.network.NetworkResult
import com.example.myapplicationv10.viewmodel.RegisterViewModel
import com.example.myapplicationv10.websocket.WebSocketManager
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * RegisterActivity - User registration screen with MVVM
 */
class RegisterActivity : BaseActivity() {

    private lateinit var firstNameField: EditText
    private lateinit var lastNameField: EditText
    private lateinit var emailField: EditText
    private lateinit var phoneField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var registerButton: Button
    private lateinit var signInText: TextView

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        firstNameField = findViewById(R.id.firstNameField)
        lastNameField = findViewById(R.id.lastNameField)
        emailField = findViewById(R.id.emailField)
        phoneField = findViewById(R.id.phoneField)
        passwordField = findViewById(R.id.passwordField)
        confirmPasswordField = findViewById(R.id.confirmPasswordField)
        registerButton = findViewById(R.id.registerButton)
        signInText = findViewById(R.id.haveAccount)
    }

    private fun setupClickListeners() {
        registerButton.setOnClickListener {
            val firstName = firstNameField.text.toString().trim()
            val lastName = lastNameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val phone = phoneField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            viewModel.register(firstName, lastName, email, phone, password, confirmPassword)
        }

        signInText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.registerState.collect { result ->
                when (result) {
                    is NetworkResult.Idle -> {
                        hideLoading()
                    }

                    is NetworkResult.Loading -> {
                        showLoading()
                    }

                    is NetworkResult.Success -> {
                        hideLoading()
                        Toast.makeText(
                            this@RegisterActivity,
                            getString(R.string.registration_success),
                            Toast.LENGTH_SHORT
                        ).show()

                        WebSocketManager.getInstance(this@RegisterActivity).connect()
                        navigateToDashboard()
                    }

                    is NetworkResult.Error -> {
                        hideLoading()
                        Toast.makeText(
                            this@RegisterActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.firstNameError.collect { error ->
                firstNameField.error = error
            }
        }

        lifecycleScope.launch {
            viewModel.lastNameError.collect { error ->
                lastNameField.error = error
            }
        }

        lifecycleScope.launch {
            viewModel.emailError.collect { error ->
                emailField.error = error
            }
        }

        lifecycleScope.launch {
            viewModel.phoneError.collect { error ->
                phoneField.error = error
            }
        }

        lifecycleScope.launch {
            viewModel.passwordError.collect { error ->
                passwordField.error = error
            }
        }

        lifecycleScope.launch {
            viewModel.confirmPasswordError.collect { error ->
                confirmPasswordField.error = error
            }
        }
    }

    private fun showLoading() {
        registerButton.isEnabled = false
        registerButton.text = getString(R.string.registering)
    }

    private fun hideLoading() {
        registerButton.isEnabled = true
        registerButton.text = getString(R.string.register_button)
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetState()
    }
}
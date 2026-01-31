package com.example.myapplicationv10

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplicationv10.adapter.ActiveValvesAdapter
import com.example.myapplicationv10.model.Valve
import com.example.myapplicationv10.databinding.ActivityDashboardBinding
import com.example.myapplicationv10.network.NetworkResult
import com.example.myapplicationv10.repository.UserRepository
import com.example.myapplicationv10.viewmodel.DashboardViewModel
import com.example.myapplicationv10.websocket.WebSocketManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardActivity : BaseActivity() {

    private lateinit var activeValvesAdapter: ActiveValvesAdapter
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var userRepository: UserRepository

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.setPadding(16, systemBars.top + 16, 16, 16)
            insets
        }

        userRepository = UserRepository(this)

        initializeViews()
        setupProfileButton()
        setupActiveValvesRecyclerView()
        setupNavigationButtons()
        setupSwipeRefresh()
        observeViewModel()
        setupWebSocket()
        loadUserAvatar()
    }

    private fun initializeViews() {
        // Views are accessible via binding
    }

    private fun setupProfileButton() {
        binding.profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupActiveValvesRecyclerView() {
        binding.activeValvesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        activeValvesAdapter = ActiveValvesAdapter(emptyList())
        binding.activeValvesRecyclerView.adapter = activeValvesAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.green)
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshDevices()
            loadUserAvatar()
        }
    }

    private fun setupNavigationButtons() {
        binding.valveControlCard.setOnClickListener {
            val currentState = viewModel.devicesState.value
            if (currentState is NetworkResult.Success && currentState.data.isNotEmpty()) {
                val firstDevice = currentState.data.first()
                val intent = Intent(this, ValveManagementActivity::class.java)
                intent.putExtra("DEVICE_ID", firstDevice.id)
                intent.putExtra("DEVICE_NAME", firstDevice.name)
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.no_device), Toast.LENGTH_SHORT).show()
            }
        }

        binding.timingPlanCard.setOnClickListener {
            val currentState = viewModel.devicesState.value
            if (currentState is NetworkResult.Success && currentState.data.isNotEmpty()) {
                val firstDevice = currentState.data.first()
                val intent = Intent(this, TimingPlanActivity::class.java)
                intent.putExtra("DEVICE_ID", firstDevice.id)
                intent.putExtra("DEVICE_NAME", firstDevice.name)
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.no_device), Toast.LENGTH_SHORT).show()
            }
        }

        binding.historyCard.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.statisticsCard.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        binding.notificationsCard.setOnClickListener {
            Snackbar.make(binding.main, getString(R.string.notifications), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun loadUserAvatar() {
        lifecycleScope.launch {
            when (val result = userRepository.getUserProfile()) {
                is NetworkResult.Success -> {
                    val user = result.data

                    val welcomeText = if (!user.firstName.isNullOrEmpty()) {
                        "${getString(R.string.welcome)} ${user.firstName}!"
                    } else {
                        getString(R.string.welcome)
                    }
                    binding.welcome.text = welcomeText

                    if (!user.avatarUrl.isNullOrEmpty()) {
                        binding.profileIcon.load(user.avatarUrl) {
                            crossfade(true)
                            placeholder(R.drawable.account_circle)
                            error(R.drawable.account_circle)
                            transformations(CircleCropTransformation())
                        }
                    }
                }
                is NetworkResult.Error -> {
                    // Silent fail for avatar loading
                }
                else -> {}
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.devicesState.collect { result ->
                when (result) {
                    is NetworkResult.Idle -> {}
                    is NetworkResult.Loading -> {
                        binding.swipeRefreshLayout.isRefreshing = true
                    }
                    is NetworkResult.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false

                        val activeValves = result.data.flatMap { device ->
                            device.pistons
                                .filter { it.state == "ACTIVE" }
                                .map { piston ->
                                    Valve(
                                        name = "${getString(R.string.valve)} ${piston.pistonNumber}",
                                        lastChanged = piston.lastTriggered ?: ""
                                    )
                                }
                        }

                        activeValvesAdapter.updateValves(activeValves)

                        if (activeValves.isEmpty()) {
                            binding.noActiveValvesText.visibility = View.VISIBLE
                            binding.activeValvesRecyclerView.visibility = View.GONE
                        } else {
                            binding.noActiveValvesText.visibility = View.GONE
                            binding.activeValvesRecyclerView.visibility = View.VISIBLE
                        }

                        Toast.makeText(
                            this@DashboardActivity,
                            getString(R.string.devices_valves_count, result.data.size, activeValves.size),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is NetworkResult.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        Snackbar.make(binding.main, result.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isRefreshing.collect { binding.swipeRefreshLayout.isRefreshing = it }
        }
    }

    private fun setupWebSocket() {
        webSocketManager = WebSocketManager.getInstance(this)
        webSocketManager.connect()

        webSocketManager.addPistonUpdateListener { message ->
            lifecycleScope.launch(Dispatchers.Main) {
                viewModel.refreshDevices()
                Toast.makeText(
                    this@DashboardActivity,
                    getString(R.string.piston_updated_state, message.pistonNumber, message.state),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        webSocketManager.addDeviceStatusListener { message ->
            lifecycleScope.launch(Dispatchers.Main) {
                viewModel.refreshDevices()
                Toast.makeText(
                    this@DashboardActivity,
                    getString(R.string.device_status, message.status),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDevices()
        loadUserAvatar()
        if (!webSocketManager.isConnected()) webSocketManager.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
    }
}
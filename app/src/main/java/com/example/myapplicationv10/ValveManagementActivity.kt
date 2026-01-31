package com.example.myapplicationv10

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.myapplicationv10.network.NetworkResult
import com.example.myapplicationv10.utils.Constants
import com.example.myapplicationv10.utils.ValveLimitManager
import com.example.myapplicationv10.viewmodel.ValveManagementViewModel
import com.example.myapplicationv10.websocket.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ValveManagementActivity - Manage pistons with MVVM
 */
class ValveManagementActivity : BaseActivity() {

    private val viewModel: ValveManagementViewModel by viewModels()
    private lateinit var webSocketManager: WebSocketManager

    private var deviceId: String? = null
    private var deviceName: String? = null

    private val valveViews = mutableMapOf<Int, Pair<CardView, ImageView>>()
    private lateinit var valveLimitManager: ValveLimitManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_valve_management)

        deviceId = intent.getStringExtra("DEVICE_ID")
        deviceName = intent.getStringExtra("DEVICE_NAME")

        if (deviceId == null) {
            Toast.makeText(this, getString(R.string.no_device_selected), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        valveLimitManager = ValveLimitManager.getInstance(this)

        setupBackButton()
        setupValveControls()
        observeViewModel()
        setupWebSocket()

        viewModel.loadDevice(deviceId!!)
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun setupValveControls() {
        val valveLimit = valveLimitManager.getValveLimit()

        val valveContainerIds = listOf(
            R.id.valve1Container, R.id.valve2Container, R.id.valve3Container, R.id.valve4Container,
            R.id.valve5Container, R.id.valve6Container, R.id.valve7Container, R.id.valve8Container
        )

        val valveCardIds = listOf(
            R.id.valve1Card, R.id.valve2Card, R.id.valve3Card, R.id.valve4Card,
            R.id.valve5Card, R.id.valve6Card, R.id.valve7Card, R.id.valve8Card
        )

        val valveIconIds = listOf(
            R.id.valve1Icon, R.id.valve2Icon, R.id.valve3Icon, R.id.valve4Icon,
            R.id.valve5Icon, R.id.valve6Icon, R.id.valve7Icon, R.id.valve8Icon
        )

        for (i in 0 until 8) {
            val pistonNumber = i + 1
            val container = findViewById<LinearLayout>(valveContainerIds[i])
            val card = findViewById<CardView>(valveCardIds[i])
            val icon = findViewById<ImageView>(valveIconIds[i])

            if (pistonNumber > valveLimit) {
                container.visibility = View.GONE
            } else {
                container.visibility = View.VISIBLE
                valveViews[pistonNumber] = Pair(card, icon)

                card.setOnClickListener {
                    showConfirmationDialog(pistonNumber)
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.deviceState.collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val device = result.data
                        device.pistons.forEach { piston ->
                            updatePistonUI(piston.pistonNumber, piston.state)
                        }
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(
                            this@ValveManagementActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.controlState.collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val pistonNumber = result.data.pistonNumber
                        val status = if (result.data.state == Constants.STATE_ACTIVE)
                            getString(R.string.opened) else getString(R.string.closed)
                        Toast.makeText(
                            this@ValveManagementActivity,
                            "${getString(R.string.valve)} $pistonNumber $status",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is NetworkResult.Error -> {
                        Toast.makeText(
                            this@ValveManagementActivity,
                            result.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    null -> {}
                    else -> {}
                }
            }
        }
    }

    private fun setupWebSocket() {
        webSocketManager = WebSocketManager.getInstance(this)

        webSocketManager.addPistonUpdateListener { message ->
            if (message.deviceId == deviceId) {
                lifecycleScope.launch(Dispatchers.Main) {
                    updatePistonUI(message.pistonNumber, message.state)

                    Toast.makeText(
                        this@ValveManagementActivity,
                        getString(R.string.piston_updated, message.pistonNumber),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showConfirmationDialog(pistonNumber: Int) {
        val piston = viewModel.getPiston(pistonNumber)
        val currentState = piston?.state ?: Constants.STATE_INACTIVE
        val action = if (currentState == Constants.STATE_ACTIVE)
            getString(R.string.deactivate) else getString(R.string.activate)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirmation))
            .setMessage(getString(R.string.confirm_piston_action, action, pistonNumber))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.togglePiston(pistonNumber, currentState)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updatePistonUI(pistonNumber: Int, state: String) {
        val views = valveViews[pistonNumber] ?: return
        val (card, icon) = views

        if (state == Constants.STATE_ACTIVE) {
            card.setCardBackgroundColor(getColor(R.color.green))
            icon.setImageResource(R.drawable.ic_toggle_on)
        } else {
            card.setCardBackgroundColor(getColor(R.color.gray_disabled))
            icon.setImageResource(R.drawable.ic_toggle_off)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.removePistonUpdateListener {}
    }
}
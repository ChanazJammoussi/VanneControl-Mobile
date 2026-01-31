package com.example.myapplicationv10

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplicationv10.adapter.ScheduleAdapter
import com.example.myapplicationv10.databinding.ActivityTimingPlanBinding
import com.example.myapplicationv10.model.ScheduleResponse
import com.example.myapplicationv10.network.NetworkResult
import com.example.myapplicationv10.viewmodel.ScheduleViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * TimingPlanActivity - Main screen for managing scheduled valve operations
 */
class TimingPlanActivity : BaseActivity() {

    private lateinit var binding: ActivityTimingPlanBinding
    private lateinit var viewModel: ScheduleViewModel
    private lateinit var scheduleAdapter: ScheduleAdapter

    private var deviceId: String? = null
    private var deviceName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimingPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceId = intent.getStringExtra("DEVICE_ID")
        deviceName = intent.getStringExtra("DEVICE_NAME")

        setupViewModel()
        setupUI()
        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ScheduleViewModel.Factory(applicationContext)
        )[ScheduleViewModel::class.java]
    }

    private fun setupUI() {
        binding.tvTitle.text = getString(R.string.timing_plan)
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(
            onToggle = { schedule, enabled ->
                viewModel.toggleSchedule(schedule.id, enabled)
            },
            onEdit = { schedule ->
                navigateToEditSchedule(schedule)
            },
            onDelete = { schedule ->
                showDeleteConfirmation(schedule)
            }
        )

        binding.schedulesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TimingPlanActivity)
            adapter = scheduleAdapter
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAdd.setOnClickListener {
            navigateToAddSchedule()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSchedules()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.schedulesState.collect { result ->
                when (result) {
                    is NetworkResult.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    is NetworkResult.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                    }
                    is NetworkResult.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val schedules = result.data

                        if (schedules.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.schedulesRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.schedulesRecyclerView.visibility = View.VISIBLE

                            val filteredSchedules = if (deviceId != null) {
                                schedules.filter { it.deviceId == deviceId }
                            } else {
                                schedules
                            }

                            scheduleAdapter.submitList(filteredSchedules)
                        }
                    }
                    is NetworkResult.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                        Toast.makeText(
                            this@TimingPlanActivity,
                            "${getString(R.string.error)}: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.deleteState.collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        Toast.makeText(
                            this@TimingPlanActivity,
                            getString(R.string.schedule_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.resetDeleteState()
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(
                            this@TimingPlanActivity,
                            getString(R.string.failed_to_delete, result.message),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.resetDeleteState()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.updateState.collect { result ->
                when (result) {
                    is NetworkResult.Error -> {
                        Toast.makeText(
                            this@TimingPlanActivity,
                            getString(R.string.failed_to_update, result.message),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.resetUpdateState()
                        viewModel.loadSchedules()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun navigateToAddSchedule() {
        val intent = Intent(this, AddTimingActivity::class.java).apply {
            putExtra("DEVICE_ID", deviceId)
            putExtra("DEVICE_NAME", deviceName)
        }
        startActivity(intent)
    }

    private fun navigateToEditSchedule(schedule: ScheduleResponse) {
        val intent = Intent(this, AddTimingActivity::class.java).apply {
            putExtra("DEVICE_ID", deviceId ?: schedule.deviceId)
            putExtra("DEVICE_NAME", deviceName)
            putExtra("SCHEDULE_ID", schedule.id)
            putExtra("SCHEDULE_NAME", schedule.name)
            putExtra("PISTON_NUMBER", schedule.pistonNumber)
            putExtra("ACTION", schedule.action)
            putExtra("CRON_EXPRESSION", schedule.cronExpression)
            putExtra("ENABLED", schedule.enabled)
            putExtra("IS_EDIT_MODE", true)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(schedule: ScheduleResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_schedule))
            .setMessage(getString(R.string.delete_schedule_confirm))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteSchedule(schedule.id)
            }
            .show()
    }
}
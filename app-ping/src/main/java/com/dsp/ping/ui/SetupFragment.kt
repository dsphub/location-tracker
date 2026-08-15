package com.dsp.ping.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dsp.ping.R
import com.dsp.ping.databinding.FragmentSetupBinding
import com.dsp.ping.service.PingService
import com.google.android.material.snackbar.Snackbar
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Первый экран: ввод адреса сайта и запуск мониторинга.
 */
class SetupFragment : Fragment() {

    private val viewModel: PingViewModel by activityViewModel()

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        startMonitoring()
        if (!granted) showNotificationsDenied()
    }

    /** Snackbar на content-view активности переживает замену фрагмента на StatusFragment. */
    private fun showNotificationsDenied() {
        val content = requireActivity().findViewById<View>(android.R.id.content)
        Snackbar.make(content, R.string.notifications_denied, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentSetupBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStart.setOnClickListener {
            val input = binding.etHost.text?.toString().orEmpty()
            val normalized = viewModel.saveHost(input)
            if (normalized == null) {
                binding.tilHost.error = getString(R.string.host_invalid)
            } else {
                binding.tilHost.error = null
                requestNotificationsAndStart()
            }
        }
    }

    private fun requestNotificationsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        ContextCompat.startForegroundService(
            requireContext(),
            Intent(requireContext(), PingService::class.java)
                .setAction(PingService.ACTION_START)
        )
        openStatus()
    }

    private fun openStatus() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, StatusFragment.newInstance())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SetupFragment()
    }
}

package com.dsp.ping.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsp.ping.R
import com.dsp.ping.data.PingRepository.Availability
import com.dsp.ping.data.db.PingEntity
import com.dsp.ping.data.db.PingStatus
import com.dsp.ping.databinding.FragmentStatusBinding
import com.dsp.ping.notifications.AvailabilityCalculator
import com.dsp.ping.service.PingService
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * Статусный экран: текущий сайт, последний результат, доступность за 24 ч и история.
 */
class StatusFragment : Fragment() {

    private val viewModel: PingViewModel by activityViewModel()

    private var _binding: FragmentStatusBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val adapter = HistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentStatusBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        binding.tvHost.text = currentHost

        binding.btnChangeHost.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, SetupFragment.newInstance())
                .commit()
        }
        binding.btnPingNow.setOnClickListener {
            requireContext().startService(
                Intent(requireContext(), PingService::class.java)
                    .setAction(PingService.ACTION_PING_NOW)
            )
        }
        binding.btnStop.setOnClickListener {
            requireContext().startService(
                Intent(requireContext(), PingService::class.java)
                    .setAction(PingService.ACTION_CLOSE)
            )
        }

        viewModel.availability().observe(viewLifecycleOwner, ::showAvailability)
        viewModel.recentPings().observe(viewLifecycleOwner, ::showPings)
        viewModel.refresh()
    }

    private val currentHost: String
        get() = viewModel.currentHost() ?: getString(R.string.status_host_unknown)

    private fun showAvailability(availability: Availability) {
        val percent = AvailabilityCalculator.percent(availability.ok, availability.fail)
        binding.tvAvailability.text = if (percent != null) {
            getString(R.string.status_availability_format, percent)
        } else {
            getString(R.string.status_availability_nd)
        }
    }

    private fun showPings(pings: List<PingEntity>) {
        binding.tvStatus.text = pings.firstOrNull()?.let(::statusText)
            ?: getString(R.string.status_no_data)
        adapter.submitList(pings)
    }

    private fun statusText(ping: PingEntity): String = when (ping.status) {
        PingStatus.OK -> getString(R.string.status_online_format, ping.latencyMs ?: 0L)
        PingStatus.FAIL -> getString(R.string.status_error_format, ping.error.orEmpty())
        else -> getString(R.string.status_no_network)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = StatusFragment()
    }
}

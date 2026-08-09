package com.dsp.androidsample.ui.history

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsp.androidsample.R
import com.dsp.androidsample.databinding.HistoryFragmentBinding
import com.dsp.androidsample.isLocationPermissionGranted
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.i
import com.dsp.androidsample.ui.history.adapter.EventItem
import com.dsp.androidsample.ui.history.adapter.HistoryAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {
    private val viewModel: HistoryViewModel by sharedViewModel()
    private lateinit var adapter: HistoryAdapter
    private var listener: Callbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        i { "onCreate" }
        super.onCreate(savedInstanceState)
        retainInstance = true
        adapter = HistoryAdapter()
        requestPermissions()
    }

    private fun requestPermissions() {
        when {
            requireContext().isLocationPermissionGranted() -> {
                d { "permission is granted" }
                viewModel.setState("permission is granted")
            }
            else -> {
                viewModel.setState("permission is not granted")
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION
                )
            }
        }
    }

    override fun onAttach(context: Context) {
        i { "onAttach" }
        super.onAttach(context)
        if (context is Callbacks) {
            listener = context
            if (context.isLocationPermissionGranted())
                (context as Callbacks).startService()
        }
    }

    override fun onDetach() {
        i { "onDetach" }
        super.onDetach()
        listener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return HistoryFragmentBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    private var _binding: HistoryFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        i { "onViewCreated" }
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        viewModel.events.observe(viewLifecycleOwner) { events ->
            val data = events.map {
                EventItem(
                    "${it.id} ${SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.US
                    ).format(it.date)} ${it.value}"
                )
            }.toList()
            adapter.setData(data)
        }
        viewModel.location.observe(viewLifecycleOwner) {
            binding.textViewHistoryLocation.text = it
        }
        viewModel.error.observe(viewLifecycleOwner) {
            binding.textViewHistoryLocation.text = it
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.listViewHistoryEvents.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        binding.listViewHistoryEvents.adapter = this.adapter
        (binding.listViewHistoryEvents.adapter as HistoryAdapter).registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            }

            override fun onChanged() {
                binding.listViewHistoryEvents.smoothScrollToPosition(adapter.itemCount)
            }
        })
    }

    interface Callbacks {
        fun startService()
    }

    companion object {
        const val REQUEST_CODE_LOCATION = 0
        fun newInstance() = HistoryFragment()
    }
}

package com.dsp.androidsample.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.dsp.androidsample.R
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.i
import kotlinx.android.synthetic.main.history_fragment.*
import org.koin.android.viewmodel.ext.android.sharedViewModel

class HistoryFragment : Fragment() {
    companion object {
        const val REQUEST_CODE_LOCATION = 0
        fun newInstance() = HistoryFragment()
    }

    private val viewModel: HistoryViewModel by sharedViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        i { "onCreate" }
        super.onCreate(savedInstanceState)
        retainInstance = true
        requestPermissions()
    }

    private fun requestPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                    == PackageManager.PERMISSION_GRANTED -> {
                d { "permission is granted" }
                viewModel.startLocationListener()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.history_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        i { "onViewCreated" }
        super.onViewCreated(view, savedInstanceState)
        viewModel.location.observe(viewLifecycleOwner, Observer {
            textView_main_location.text = it
        })
        viewModel.error.observe(viewLifecycleOwner, Observer {
            textView_main_location.text = it
        })
    }
}
package com.dsp.androidsample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.dsp.androidsample.R
import com.dsp.androidsample.log.Logger.i
import kotlinx.android.synthetic.main.history_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel

class HistoryFragment : Fragment() {
    companion object {
        fun newInstance() = HistoryFragment()
    }

    private val viewModel: HistoryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        i { "onCreate" }
        super.onCreate(savedInstanceState)
        retainInstance = true
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
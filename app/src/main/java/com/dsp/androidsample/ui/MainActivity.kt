package com.dsp.androidsample.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dsp.androidsample.R
import com.dsp.androidsample.log.Logger
import com.dsp.androidsample.log.Logger.d
import com.dsp.androidsample.log.Logger.i
import com.dsp.androidsample.service.LocationService
import org.koin.android.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: HistoryViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        i { "onCreate" }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addFragment(R.id.frameLayout_history, HistoryFragment.newInstance())
        openBatteryOptimization()
        startService()
    }

    private fun openBatteryOptimization() {
        if (SDK_INT >= VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            d { "openBatteryOptimization ignored=${pm.isIgnoringBatteryOptimizations(packageName)}" }
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                    startActivity(this)
                }
            }
        }
    }

    private fun startService() {
        val connectionIntent = Intent(this, LocationService::class.java)
        if (SDK_INT >= VERSION_CODES.O) {
            startForegroundService(connectionIntent)
        } else {
            startService(connectionIntent)
        }
    }

    private fun addFragment(frameId: Int, fragment: Fragment) {
        supportFragmentManager.findFragmentByTag("HistoryFragment") ?: supportFragmentManager
            .beginTransaction()
            .add(frameId, fragment, "HistoryFragment")
            .commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Logger.d { "onRequestPermissionsResult" }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == HistoryFragment.REQUEST_CODE_LOCATION) {
            loop@ for (g in grantResults.withIndex()) {
                if (g.value != PackageManager.PERMISSION_GRANTED) {
                    Logger.w { "onRequestPermissionsResult: permission ${permissions[g.index]} is not granted" }
                    return
                }
            }
        }
        viewModel.startLocationListener()
    }
}

package com.dsp.ping.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.dsp.ping.R
import com.dsp.ping.service.PingService
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Точка входа с прозрачной темой: при уже настроенном хосте стартует сервис
 * и завершается без показа UI; иначе показывает Setup/Status экраны.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: PingViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            route(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        route(intent)
    }

    private fun route(intent: Intent?) {
        when {
            intent?.getBooleanExtra(PingService.EXTRA_FROM_NOTIFICATION, false) == true ->
                showStatus()

            viewModel.isHostConfigured() -> {
                startMonitoringAndFinish()
            }

            else -> showSetup()
        }
    }

    private fun showStatus() {
        supportFragmentManager.commit {
            replace(R.id.container, StatusFragment.newInstance())
        }
    }

    private fun showSetup() {
        supportFragmentManager.commit {
            replace(R.id.container, SetupFragment.newInstance())
        }
    }

    private fun startMonitoringAndFinish() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, PingService::class.java)
                .setAction(PingService.ACTION_START)
        )
        finish()
    }
}

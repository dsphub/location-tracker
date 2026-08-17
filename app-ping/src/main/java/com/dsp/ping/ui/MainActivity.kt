package com.dsp.ping.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.dsp.ping.R
import com.dsp.ping.databinding.ActivityMainBinding
import com.dsp.ping.service.PingService
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Точка входа с прозрачной темой: без настроенного хоста показывает Setup-экран;
 * иначе гарантирует запуск сервиса (если не работает) и показывает статусный экран —
 * одинаково для тапа по иконке и по нотификации.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: PingViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        if (savedInstanceState == null) {
            route()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        route()
    }

    private fun route() {
        if (!viewModel.isHostConfigured()) {
            showSetup()
            return
        }
        // Тап по иконке или нотификации: сервис должен работать,
        // даже если был остановлен (Stop) или убит системой.
        startMonitoring()
        showStatus()
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

    private fun startMonitoring() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, PingService::class.java)
                .setAction(PingService.ACTION_START)
        )
    }
}

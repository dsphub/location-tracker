package com.dsp.ping.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dsp.ping.data.PingRepository
import com.dsp.ping.data.SettingsStore
import com.dsp.ping.data.db.PingEntity
import com.dsp.ping.ping.HostNormalizer

class PingViewModel(
    private val repository: PingRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val availabilityLiveData = MutableLiveData<PingRepository.Availability>()
    private val recentPingsLiveData = MutableLiveData<List<PingEntity>>(emptyList())

    init {
        refresh()
    }

    fun isHostConfigured(): Boolean = settingsStore.getHost() != null

    fun currentHost(): String? = settingsStore.getHost()

    /**
     * Нормализует и сохраняет адрес сайта.
     *
     * @return нормализованный URL либо null при невалидном вводе.
     */
    fun saveHost(rawInput: String): String? {
        val normalized = HostNormalizer.normalize(rawInput) ?: return null
        settingsStore.setHost(normalized)
        return normalized
    }

    fun availability(): LiveData<PingRepository.Availability> = availabilityLiveData

    fun recentPings(): LiveData<List<PingEntity>> = recentPingsLiveData

    /** Повторно запрашивает данные у репозитория (колбэки приходят на main-поток). */
    fun refresh() {
        repository.requestAvailability24h { availability ->
            availabilityLiveData.postValue(availability)
        }
        val since = System.currentTimeMillis() - HISTORY_WINDOW_MS
        repository.requestRecent(since) { pings ->
            recentPingsLiveData.postValue(pings)
        }
    }

    private companion object {
        const val HISTORY_WINDOW_MS = 24L * 60L * 60L * 1000L
    }
}

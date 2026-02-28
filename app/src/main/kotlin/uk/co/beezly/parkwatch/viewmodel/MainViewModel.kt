package uk.co.beezly.parkwatch.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uk.co.beezly.parkwatch.data.AppSettings
import uk.co.beezly.parkwatch.data.ParkEvent
import uk.co.beezly.parkwatch.data.SettingsRepository
import uk.co.beezly.parkwatch.service.TimerForegroundService

sealed class TimerState {
    object Idle : TimerState()
    data class Running(val remainingMs: Long, val totalMs: Long) : TimerState()
    object OutsideHours : TimerState()
    object OutsideZone : TimerState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val parkEvents: StateFlow<List<ParkEvent>> = repo.parkEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    init {
        // Poll timer service state
        viewModelScope.launch {
            while (isActive) {
                _timerState.value = if (TimerForegroundService.isRunning) {
                    val remaining = TimerForegroundService.remainingMs
                    val total = TimerForegroundService.durationMs
                    if (remaining > 0) TimerState.Running(remaining, total) else TimerState.Idle
                } else {
                    TimerState.Idle
                }
                delay(1000)
            }
        }
    }

    fun cancelTimer() {
        val context = getApplication<Application>()
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            repo.updateSettings(newSettings)
        }
    }
}

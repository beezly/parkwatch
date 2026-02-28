package uk.co.beezly.parkwatch.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import uk.co.beezly.parkwatch.data.ParkEvent
import uk.co.beezly.parkwatch.data.SettingsRepository
import uk.co.beezly.parkwatch.service.TimerForegroundService
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

class BluetoothReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BluetoothReceiver"
    }

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        val mac = device?.address ?: return
        Log.d(TAG, "BT event: ${intent.action} device=$mac")

        val repo = SettingsRepository(context)

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                val settings = kotlinx.coroutines.flow.first(repo.settingsFlow)
                if (settings.carDeviceMac.isBlank() || settings.carDeviceMac != mac) {
                    Log.d(TAG, "Device $mac is not the configured car device, ignoring")
                    return@launch
                }

                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        Log.d(TAG, "Car disconnected, checking conditions...")
                        handleDisconnect(context, repo, settings)
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        Log.d(TAG, "Car reconnected, cancelling timer if running")
                        handleConnect(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleDisconnect(
        context: Context,
        repo: SettingsRepository,
        settings: uk.co.beezly.parkwatch.data.AppSettings
    ) {
        // Check restriction hours
        if (!isWithinRestrictionHours(settings)) {
            Log.d(TAG, "Outside restriction hours, not starting timer")
            return
        }

        // Check location if enabled
        if (settings.locationCheckEnabled) {
            val inZone = isInZone(context, settings)
            if (inZone == false) {
                Log.d(TAG, "Not in zone, not starting timer")
                return
            }
        }

        // All checks passed – start timer
        Log.d(TAG, "Starting parking timer for ${settings.timerDurationMinutes} minutes")
        val startTime = System.currentTimeMillis()
        repo.addParkEvent(ParkEvent(startTime = startTime))

        val serviceIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START
            putExtra(TimerForegroundService.EXTRA_DURATION_MINUTES, settings.timerDurationMinutes)
            putExtra(TimerForegroundService.EXTRA_START_TIME, startTime)
        }
        context.startForegroundService(serviceIntent)
    }

    private fun handleConnect(context: Context) {
        if (TimerForegroundService.isRunning) {
            val serviceIntent = Intent(context, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_CANCEL
            }
            context.startService(serviceIntent)
        }
    }

    private fun isWithinRestrictionHours(settings: uk.co.beezly.parkwatch.data.AppSettings): Boolean {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val dayOfWeek = now.dayOfWeek
        val currentTime = now.toLocalTime()

        val (startStr, endStr) = if (dayOfWeek == DayOfWeek.SUNDAY) {
            settings.sundayStartTime to settings.sundayEndTime
        } else {
            settings.weekdayStartTime to settings.weekdayEndTime
        }

        val start = parseTime(startStr) ?: return false
        val end = parseTime(endStr) ?: return false

        return currentTime.isAfter(start) && currentTime.isBefore(end)
    }

    private fun parseTime(timeStr: String): LocalTime? {
        return try {
            val parts = timeStr.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun isInZone(context: Context, settings: uk.co.beezly.parkwatch.data.AppSettings): Boolean? {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location: Location? = fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .await()

            if (location == null) {
                Log.w(TAG, "Could not get location")
                return null
            }

            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                settings.zoneLat, settings.zoneLng,
                results
            )
            val distanceMetres = results[0]
            Log.d(TAG, "Distance to zone centre: ${distanceMetres}m (radius: ${settings.zoneRadiusMetres}m)")
            distanceMetres <= settings.zoneRadiusMetres
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission denied: ${e.message}")
            null // Treat as unknown → don't block timer
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}")
            null
        }
    }
}

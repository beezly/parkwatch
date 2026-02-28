package uk.co.beezly.parkwatch.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import uk.co.beezly.parkwatch.MainActivity
import uk.co.beezly.parkwatch.R
import uk.co.beezly.parkwatch.data.ParkOutcome
import uk.co.beezly.parkwatch.data.SettingsRepository

class TimerForegroundService : Service() {

    companion object {
        const val ACTION_START = "uk.co.beezly.parkwatch.START_TIMER"
        const val ACTION_CANCEL = "uk.co.beezly.parkwatch.CANCEL_TIMER"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_START_TIME = "start_time"

        const val CHANNEL_ID_TIMER = "parkwatch_timer"
        const val CHANNEL_ID_EXPIRED = "parkwatch_expired"
        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_EXPIRED = 1002

        // Shared state accessible by ViewModel
        @Volatile var isRunning = false
        @Volatile var startTimeMs = 0L
        @Volatile var durationMs = 0L
        @Volatile var remainingMs = 0L
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private lateinit var repo: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        repo = SettingsRepository(applicationContext)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 90)
                val startTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
                startTimer(durationMinutes, startTime)
            }
            ACTION_CANCEL -> {
                cancelTimer(ParkOutcome.CANCELLED)
            }
        }
        return START_STICKY
    }

    private fun startTimer(durationMinutes: Int, startTime: Long) {
        isRunning = true
        startTimeMs = startTime
        durationMs = durationMinutes * 60 * 1000L
        remainingMs = durationMs

        startForeground(NOTIFICATION_ID_TIMER, buildTimerNotification(remainingMs))

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (remainingMs > 0) {
                delay(30_000) // Update every 30 seconds
                remainingMs = durationMs - (System.currentTimeMillis() - startTimeMs)
                if (remainingMs <= 0) break
                updateTimerNotification(remainingMs)
            }
            // Timer expired
            onTimerExpired()
        }
    }

    private fun onTimerExpired() {
        isRunning = false
        serviceScope.launch {
            repo.updateLatestEvent(ParkOutcome.EXPIRED)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID_TIMER)

        val expiredNotification = NotificationCompat.Builder(this, CHANNEL_ID_EXPIRED)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("⚠️ Move your car!")
            .setContentText("Your parking time has expired.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()

        nm.notify(NOTIFICATION_ID_EXPIRED, expiredNotification)
        stopSelf()
    }

    fun cancelTimer(outcome: ParkOutcome) {
        timerJob?.cancel()
        isRunning = false
        remainingMs = 0L
        serviceScope.launch {
            repo.updateLatestEvent(outcome)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID_TIMER)
        nm.cancel(NOTIFICATION_ID_EXPIRED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildTimerNotification(remainingMs: Long): Notification {
        val remainingText = formatRemaining(remainingMs)

        val cancelIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPi = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_TIMER)
            .setSmallIcon(R.drawable.ic_parking)
            .setContentTitle("🅿️ ParkWatch")
            .setContentText("$remainingText remaining")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppPendingIntent())
            .addAction(R.drawable.ic_cancel, "Cancel", cancelPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateTimerNotification(remainingMs: Long) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_TIMER, buildTimerNotification(remainingMs))
    }

    private fun formatRemaining(ms: Long): String {
        val totalMinutes = (ms / 1000 / 60).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_ID_TIMER,
            "Parking Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live countdown timer while parked"
            nm.createNotificationChannel(this)
        }

        NotificationChannel(
            CHANNEL_ID_EXPIRED,
            "Parking Expired",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alert when parking time is up"
            nm.createNotificationChannel(this)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

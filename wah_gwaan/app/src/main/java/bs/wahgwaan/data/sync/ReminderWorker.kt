package bs.wahgwaan.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import bs.wahgwaan.data.db.EventDao
import bs.wahgwaan.data.db.toDomain
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Daily check: any SAVED event happening tomorrow fires a reminder
 * notification that deep-links straight to the event page. Runs entirely
 * on-device against the Room cache — works offline, no accounts needed.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: EventDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return Result.success()   // user declined notifications; not an error

        val tomorrow = LocalDate.now().plusDays(1)
        val events = dao.favoritesOnDay(tomorrow.toEpochDay()).map { it.toDomain(true) }
        if (events.isEmpty()) return Result.success()

        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Event reminders",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders for events you saved"
            })

        val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
        events.forEachIndexed { index, event ->
            val tap = PendingIntent.getActivity(
                applicationContext, index,
                Intent(Intent.ACTION_VIEW, Uri.parse("wahgwaan://event/${event.id}")),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val timeBit = event.timeStart?.let { " at ${it.format(timeFmt)}" } ?: ""
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle("Tomorrow: ${event.name}")
                .setContentText("${event.venue.ifBlank { "Nassau" }}$timeBit 🎉")
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build()
            nm.notify(event.id.hashCode(), notification)
        }
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "event-reminders"
        private const val UNIQUE_NAME = "saved-event-reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

package com.example.aidocumentorganizer.data.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.aidocumentorganizer.MainActivity
import com.example.aidocumentorganizer.data.local.DocumentDatabase
import com.example.aidocumentorganizer.utils.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

const val REMINDER_WINDOW_DAYS = 30
private const val DAY_MS = 24L * 60 * 60 * 1000

object ReminderScheduler {

    private const val WORK_NAME = "expiry_reminders"

    // KEEP means calling this on every app start is safe: it will not
    // reset the schedule if it is already registered.
    fun schedule(context: Context) {

        val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(WORK_NAME)
    }
}

class ExpiryReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.success()

        if (!AppPreferences(applicationContext).remindersEnabled.first()) {
            return Result.success()
        }

        val now = System.currentTimeMillis()

        val expiring = DocumentDatabase
            .getDatabase(applicationContext)
            .documentDao()
            .getDocumentsExpiringBetween(
                userId = user.uid,
                // Expiry dates are stored at UTC midnight, so look back a
                // day to still catch "expires today".
                from = now - DAY_MS,
                to = now + REMINDER_WINDOW_DAYS * DAY_MS
            )

        if (expiring.isEmpty()) return Result.success()

        val first = expiring.first()

        val daysLeft = maxOf(
            0,
            Math.ceil(((first.expiryDate ?: now) - now).toDouble() / DAY_MS).toInt()
        )

        val message =
            if (expiring.size == 1) {
                if (daysLeft == 0) "\"${first.title}\" expires today."
                else "\"${first.title}\" expires in $daysLeft day${if (daysLeft == 1) "" else "s"}."
            } else {
                "${expiring.size} documents expire within $REMINDER_WINDOW_DAYS days, " +
                        "starting with \"${first.title}\"."
            }

        showNotification(message)

        return Result.success()
    }

    @SuppressLint("MissingPermission") // checked explicitly just below
    private fun showNotification(message: String) {

        val context = applicationContext

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = "document_expiry"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Document reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts when a saved document is about to expire"
                }
            )
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Document expiring soon")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}

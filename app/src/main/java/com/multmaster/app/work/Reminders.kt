package com.multmaster.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.multmaster.app.MultiplicationApp
import com.multmaster.app.util.TimeUtils
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object Reminders {
    private const val WORK = "daily_reminder"
    private const val CHANNEL = "reminders"

    fun schedule(context: Context, force: Boolean = true) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK,
            if (force) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MultiplicationApp
        val settings = app.container.settings.current.first()
        if (!settings.reminders) return Result.success()

        val due = app.container.mistakeRepo.activeList().count { it.dueAt <= System.currentTimeMillis() }
        if (due == 0) return Result.success()

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33) {
            return Result.success()
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel("reminders", "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.notify(
            1001,
            NotificationCompat.Builder(applicationContext, "reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Multiplication Master")
                .setContentText("$due question(s) are due for revision today.")
                .setAutoCancel(true)
                .build()
        )
        return Result.success()
    }
}
package ir.mycalendar.app.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ir.mycalendar.app.MainActivity
import ir.mycalendar.app.R
import ir.mycalendar.app.date.PersianCalendarHelper
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "persian_calendar_daily_channel"
    const val NOTIFICATION_ID = 1001
    const val REQUEST_CODE_ALARM = 2001
    const val PREFS_NAME = "my_calendar_prefs"
    const val KEY_NOTIFICATION_ENABLED = "key_notification_enabled"

    /**
     * Initializes notification channel (required for Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW // Silent, no annoying sound/vibration
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows or updates the date notification showing the current Persian date
     */
    fun showDateNotification(context: Context) {
        createNotificationChannel(context)

        val todayInfo = PersianCalendarHelper.getTodayFullDateInfo()

        // Clicking the notification launches MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${todayInfo.persianDayOfWeekName} ${PersianCalendarHelper.toPersianDigits(todayInfo.persianDay)} ${todayInfo.persianMonthName}"
        val contentText = "${todayInfo.persianFormatted}  |  ${todayInfo.gregorianFormatted} (${todayInfo.gregorianMonthNamePersian})"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$contentText"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Persistent in status drawer so user always sees the date
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission not yet granted
        }
    }

    /**
     * Cancels the notification
     */
    fun cancelDateNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Schedules an efficient alarm for the next midnight (00:00:03).
     * Wakes up ONLY when the date actually changes, keeping battery consumption at ~0%.
     */
    fun scheduleNextMidnightAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DateNotificationReceiver::class.java).apply {
            action = DateNotificationReceiver.ACTION_UPDATE_DATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val delayMillis = PersianCalendarHelper.getMillisUntilNextMidnight()
        val triggerAtMillis = System.currentTimeMillis() + delayMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cancels the scheduled midnight alarm
     */
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DateNotificationReceiver::class.java).apply {
            action = DateNotificationReceiver.ACTION_UPDATE_DATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun isNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true) // Enabled by default
    }

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()

        if (enabled) {
            showDateNotification(context)
            scheduleNextMidnightAlarm(context)
        } else {
            cancelDateNotification(context)
            cancelAlarm(context)
        }
    }
}

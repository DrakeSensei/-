package ir.mycalendar.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DateNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UPDATE_DATE = "ir.mycalendar.app.ACTION_UPDATE_DATE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        // Check if the user has date notifications enabled
        if (!NotificationHelper.isNotificationEnabled(context)) {
            return
        }

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_SET,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_UPDATE_DATE -> {
                // Update the date notification to today's date
                NotificationHelper.showDateNotification(context)

                // Schedule the next check for upcoming midnight
                NotificationHelper.scheduleNextMidnightAlarm(context)
            }
        }
    }
}

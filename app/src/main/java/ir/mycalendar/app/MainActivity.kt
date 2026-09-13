package ir.mycalendar.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import ir.mycalendar.app.notification.NotificationHelper
import ir.mycalendar.app.ui.CalendarScreen
import ir.mycalendar.app.ui.theme.MyCalendarTheme

class MainActivity : ComponentActivity() {

    // Notification permission launcher for Android 13+ (API 33+)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationHelper.showDateNotification(this)
            NotificationHelper.scheduleNextMidnightAlarm(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize notification channel and schedule next midnight rollover
        if (NotificationHelper.isNotificationEnabled(this)) {
            checkAndRequestNotificationPermission()
            NotificationHelper.showDateNotification(this)
            NotificationHelper.scheduleNextMidnightAlarm(this)
        }

        setContent {
            val systemInDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemInDark) }

            MyCalendarTheme(darkTheme = isDarkTheme) {
                // Apply RTL (Right-to-Left) layout direction for native Persian UX
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    CalendarScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onRequestNotificationPermission = { checkAndRequestNotificationPermission() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

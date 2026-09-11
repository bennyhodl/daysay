package com.benschroth.daylightmic.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.benschroth.daylightmic.R
import com.benschroth.daylightmic.ui.MainActivity

/**
 * Keeps the process in the foreground with the microphone type while a dictation is running.
 * The accessibility service already gives the process foreground capabilities while the screen
 * is on; this service makes the microphone indicator behave and covers the screen-off case.
 * Failure to start is logged and ignored: recording still runs in the accessibility process.
 */
class MicForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } catch (t: Throwable) {
            Log.w(TAG, "startForeground refused: ${t.message}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Dictation", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Dictation in progress")
            .setContentText("Press the button again to stop")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "MicFgs"
        private const val CHANNEL_ID = "dictation"
        private const val NOTIFICATION_ID = 41

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, MicForegroundService::class.java))
            } catch (t: Throwable) {
                Log.w(TAG, "foreground service not started: ${t.message}")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MicForegroundService::class.java))
        }
    }
}

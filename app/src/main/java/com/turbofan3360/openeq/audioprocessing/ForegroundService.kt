package com.turbofan3360.openeq.audioprocessing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.turbofan3360.openeq.MainActivity
import com.turbofan3360.openeq.R

private const val NOTIFICATION_ID = "0"
private const val FOREGROUND_NOTIFICATION_CHANEL_ID = "0"

// Foreground service that listens for media streams starting and then attaches equalizers to them
class EQMediaListenerService: Service() {
    override fun onBind(intent: Intent): IBinder? {
        // Required method; not needed in this service
        return null
    }

    @RequiresPermission(NOTIFICATION_SERVICE)
    fun startForeground(
    ) {
        // Calling the function that handles creating the notification
        eqNotification()

        // TODO: START CO-ROUTINE TO LISTEN FOR MEDIA STREAMS
    }

    @RequiresPermission(NOTIFICATION_SERVICE)
    private fun eqNotification() {
        // Creating a notification channel to post my notification to
        createEqNotificationChannel(
            getString(R.string.notification_channel_name),
            getString(R.string.notification_channel_info),
            FOREGROUND_NOTIFICATION_CHANEL_ID
        )

        // Creating the intent to happen when notification is tapped
        val tapIntent: PendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        // Creating the notification object for my foreground service notification
        val notification = NotificationCompat.Builder(this, NOTIFICATION_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_info))
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Shows notification on notification channel
        with (NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID.toInt(), notification.build())
        }
    }

    private fun createEqNotificationChannel(
        channelName: String,
        channelDescription: String,
        channelNo: String
    ){

        // Creates a notification channel that notifications can then be posted to
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(channelNo, channelName, importance).apply {
            description = channelDescription
        }
        // Register the channel with the system
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
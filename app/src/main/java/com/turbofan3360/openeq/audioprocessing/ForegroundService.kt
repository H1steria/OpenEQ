package com.turbofan3360.openeq.audioprocessing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.checkSelfPermission

import com.turbofan3360.openeq.MainActivity
import com.turbofan3360.openeq.R

private const val NOTIFICATION_ID = 0
private const val FOREGROUND_NOTIFICATION_CHANEL_ID = "0"

// Foreground service that listens for media streams starting and then attaches equalizers to them
class EQMediaListenerService: Service() {
    lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent): IBinder? {
        // Required method; not used in this service
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Called when starting the service
        // Calling the function that handles creating the notification
        eqNotification()

        // TODO: START CO-ROUTINE TO LISTEN FOR MEDIA STREAMS

        return START_STICKY
    }

    override fun onDestroy() {
        // Tidies up everything when stopping the foreground service
        // Deletes notification
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        // Deletes notification channel (to prevent repeating a new notification channel if service re-started)
        notificationManager.deleteNotificationChannel(FOREGROUND_NOTIFICATION_CHANEL_ID)
    }

    private fun eqNotification() {
        if (checkSelfPermission(this, NOTIFICATION_SERVICE) == PackageManager.PERMISSION_DENIED) {
            return
        }

        // Creating a notification channel to post my notification to
        createEqNotificationChannel(
            getString(R.string.notification_channel_name),
            getString(R.string.notification_channel_info),
        )

        // Creating the intent to happen when notification is tapped
        val tapIntent: PendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        // Creating the notification object for my foreground service notification
        val notification = NotificationCompat.Builder(this, NOTIFICATION_ID.toString())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_info))
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Shows notification on notification channel
        with (NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notification.build())
        }
    }

    private fun createEqNotificationChannel(
        channelName: String,
        channelDescription: String,
    ){

        // Creates a notification channel that notifications can then be posted to
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(FOREGROUND_NOTIFICATION_CHANEL_ID, channelName, importance).apply {
            description = channelDescription
        }
        // Register the channel with the system
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
package org.hyperskill.phrases

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        println("Alarm is received")
        // Build the notification (you'll need to replace this with your notification logic)
        val notification = buildNotification(context, intent.getStringExtra("phrase")!!)

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(context: Context, string: String): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Your phrase of the day")
            .setSmallIcon(android.R.mipmap.sym_def_app_icon)
            .setContentText(string)
            .setAutoCancel(true) // Remove notification after tapping on it

        // Add additional notification actions or configuration as needed
        Log.d("TAG", "ALARM tRIGGED")
        Log.d("TAG", "phrase: ${string}")

        return builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "org.hyperskill.phrases" // Replace with your unique channel ID
        private const val NOTIFICATION_ID = 393939 //Use a unique ID for each notification
    }
}

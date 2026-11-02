package com.example.nephroticsyndrome.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nephroticsyndrome.MainActivity
import com.example.nephroticsyndrome.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles FCM pushes for the Android client. Token persistence is performed only
 * after the user has an authenticated Supabase session (see token sync in app flow).
 */
class NephroFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PENDING_TOKEN, token).apply()
        // The app flow syncs this pending token to the patient-owned push_tokens table
        // after Supabase sign-in. Do not make unauthenticated database writes here.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.data["title"] ?: message.notification?.title ?: "Message from your doctor"
        val body = message.data["body"] ?: message.notification?.body ?: "You have a new notification"
        val notificationId = message.data["notification_id"]?.toIntOrNull() ?: message.messageId?.hashCode() ?: 1
        ensureChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_notification_id", message.data["notification_id"])
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Doctor notifications", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "OPD, investigation and advice notifications from your doctor" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val PREFS = "nephro_push"
        const val KEY_PENDING_TOKEN = "pending_fcm_token"
        const val CHANNEL_ID = "doctor_notifications"
    }
}

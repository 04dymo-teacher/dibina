package com.example.dibina.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service for DIBINA.
 * Receives daily habit reminders, cheer alerts, and class announcements.
 */
class DibinaMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "dibina_habits_channel"
        const val CHANNEL_NAME = "Pengingat Kebiasaan DIBINA"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Associate new FCM registration token with current user if logged in
        val user = FirebaseAuth.getInstance().currentUser ?: return
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update("fcmToken", token)
        } catch (_: Exception) {
            // Firestore not ready or network offline
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Pengingat DIBINA"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Jangan lupa catat 7 Kebiasaan Baikmu hari ini ya!"

        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat kebiasaan pagi, belajar, dan malam"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dibina_book_mark)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

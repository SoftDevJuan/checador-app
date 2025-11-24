package com.example.checadorccl.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.checadorccl.MainActivity
import com.example.checadorccl.R
import com.example.checadorccl.api.ApiClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "Nuevo token: $token")
        enviarTokenAlServidor(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Mensaje recibido")
        remoteMessage.notification?.let {
            mostrarNotificacion(it.title, it.body)
        }
    }

    private fun mostrarNotificacion(title: String?, body: String?) {
        val channelId = "canal_notificaciones_ccl"

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // --- AQUÍ EL CAMBIO CLAVE ---
        // Le decimos a la Main Activity qué abrir
        intent.putExtra("OPEN_FRAGMENT", "NOTIFICATIONS")
        // ---------------------------

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_logo) // Tu ícono blanco
            .setColor(getColor(R.color.color_primary_ccl))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones CCL",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun enviarTokenAlServidor(token: String) {
        val prefs = getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("FCM_TOKEN", token).apply()
        val userToken = prefs.getString("ACCESS_TOKEN", null)

        if (userToken != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiClient.service.registerDevice("Bearer $userToken", token)
                } catch (e: Exception) { Log.e("FCM", "Error token", e) }
            }
        }
    }
}
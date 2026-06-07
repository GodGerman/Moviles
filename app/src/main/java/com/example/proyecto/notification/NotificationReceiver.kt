package com.example.proyecto.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.proyecto.R

/**
 * Propósito: Receptor de difusión encargado de disparar las Notificaciones Locales en un momento exacto.
 * Rol en MVVM: Reemplazo del NotificationWorker para asegurar la entrega incluso si la app está cerrada.
 * Interacciones: Es "despertado" por el AlarmManager.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: context.getString(R.string.notification_default_title)
        val description = intent.getStringExtra("description") ?: context.getString(R.string.notification_default_body)

        sendNotification(context, title, description)
    }

    private fun sendNotification(context: Context, title: String, description: String) {
        val channelId = "event_notifications"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Creación del Canal de Notificaciones para Android O y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                context.getString(R.string.notification_channel_name), 
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Construcción compatible de la notificación
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Icono obligatorio
            .setContentTitle(title)
            .setContentText(description)
            // IMPORTANCE_HIGH hace que la notificación flote ("Heads-up") en pantalla
            .setPriority(NotificationCompat.PRIORITY_HIGH) 
            .setAutoCancel(true) // Desaparece automáticamente al tocarla
            .build()

        // El primer parámetro es el ID numérico. Usamos el tiempo actual para asegurar ID único.
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

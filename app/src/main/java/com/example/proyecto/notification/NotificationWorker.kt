package com.example.proyecto.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.proyecto.R

/**
 * Propósito: Worker de Android Jetpack encargado de disparar las Notificaciones Locales.
 * Rol en MVVM: Capa de Lógica de Trabajo (Background Worker). Funciona independiente a la UI, asegurando que el sistema operativo ejecute esta tarea incluso si la aplicación está cerrada.
 * Interacciones: Es programado usando `WorkManager` (principalmente desde [com.example.proyecto.ui.add.AddEventFragment]) para lanzarse a una hora calculada del futuro.
 */
class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    /**
     * Propósito: Método principal y obligatorio de todo Worker. Ejecuta el trabajo encomendado en background.
     * Parámetros: Ninguno explícito, pero extrae parámetros de `inputData` (inyectados por quien programó el Worker).
     * Retorno: Objeto [Result] (`Result.success()`) indicando que el trabajo se completó satisfactoriamente.
     * Lógica interna: 
     * 1. Extrae de `inputData` el título y descripción configurados al momento de programar el recordatorio.
     * 2. Aplica valores por defecto empleando los recursos (strings.xml) si llegan vacíos.
     * 3. Llama a la función privada `sendNotification` para renderizar el mensaje push.
     */
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: applicationContext.getString(R.string.notification_default_title)
        val description = inputData.getString("description") ?: applicationContext.getString(R.string.notification_default_body)

        // Lanzar la notificación en pantalla
        sendNotification(title, description)
        return Result.success()
    }

    /**
     * Propósito: Construye y muestra visualmente la notificación en el ecosistema Android.
     * Parámetros:
     * - title: Título en negrita de la notificación.
     * - description: Cuerpo de texto detallando el evento.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Solicita al sistema el manejador de notificaciones `NotificationManager`.
     * 2. Si la versión de Android es 8.0 (Oreo) o superior, crea forzosamente un `NotificationChannel`, el cual permite al usuario configurar alertas.
     * 3. Configura un objeto `NotificationCompat.Builder`, aplicando icono, título y texto. 
     * 4. Establece la máxima prioridad (`PRIORITY_HIGH`) para que se muestre como notificación flotante en la parte superior (Heads-up).
     * 5. Emite la notificación usando el timestamp del sistema como ID único para evitar la sobrescritura en caso de recibir múltiples notificaciones a la vez.
     */
    private fun sendNotification(title: String, description: String) {
        val channelId = "event_notifications"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Creación del Canal de Notificaciones para Android O y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                applicationContext.getString(R.string.notification_channel_name), 
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Construcción compatible de la notificación
        val notification = NotificationCompat.Builder(applicationContext, channelId)
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

package com.example.proyecto.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Propósito: Entidad que representa la tabla 'events' en la base de datos local Room.
 * Rol en MVVM: Modelo de Datos (Model). Es la estructura de datos principal que viaja desde la capa de Datos hasta la capa de Interfaz de Usuario.
 * Interacciones: Utilizada por [com.example.proyecto.data.local.EventDao] para operaciones de base de datos y por adaptadores/fragmentos de UI (ej. EventAdapter) para mostrar la información en pantalla.
 */
@Entity(tableName = "events")
data class EventEntity(
    /**
     * Identificador único del evento.
     * Al usar autoGenerate = true, Room asignará automáticamente un número secuencial (1, 2, 3...)
     * cada vez que insertemos un nuevo evento. Por defecto le pasamos 0.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    /** Categoría a la que pertenece el evento (ej. Trabajo, Personal, Escuela). */
    val categoria: String,
    
    /** Fecha en la que ocurrirá el evento (formato esperado: YYYY-MM-DD o DD/MM/YYYY). */
    val fecha: String,
    
    /** Hora en la que ocurrirá el evento (formato esperado: HH:MM). */
    val hora: String,
    
    /** Descripción detallada o notas adicionales sobre el evento. */
    val descripcion: String,
    
    /** Estado actual del evento (ej. Pendiente, Completado). */
    val estatus: String,
    
    /** Latitud de la ubicación geográfica asociada al evento. Usada en MapsActivity. */
    val ubicacion_lat: Double,
    
    /** Longitud de la ubicación geográfica asociada al evento. Usada en MapsActivity. */
    val ubicacion_lng: Double,
    
    /** Nombre de un contacto asociado a este evento, útil para integraciones de agenda. */
    val contacto_nombre: String,
    
    /** Tipo de recordatorio (en minutos antes) para programar notificaciones a través de WorkManager. */
    val recordatorio_tipo: Int
)

package com.example.proyecto.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoria: String,
    val fecha: String,
    val hora: String,
    val descripcion: String,
    val estatus: String,
    val ubicacion_lat: Double,
    val ubicacion_lng: Double,
    val contacto_nombre: String,
    val recordatorio_tipo: Int
)

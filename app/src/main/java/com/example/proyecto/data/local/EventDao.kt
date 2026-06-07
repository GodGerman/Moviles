package com.example.proyecto.data.local

import androidx.room.*
import com.example.proyecto.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Propósito: Data Access Object (DAO) para la entidad EventEntity. Define todas las operaciones SQL (CRUD) permitidas sobre la tabla de eventos.
 * Rol en MVVM: Capa de Datos (Data Source Local). Abstrae las consultas SQL en métodos de Kotlin para que el Repositorio pueda invocarlos sin conocer el lenguaje de base de datos.
 * Interacciones: Sus métodos son llamados exclusivamente por [com.example.proyecto.data.repository.EventRepository]. Interactúa con [EventEntity] para leer y escribir datos.
 */
@Dao
interface EventDao {
    
    /**
     * Propósito: Inserta un nuevo evento en la base de datos.
     * Parámetros:
     * - event: El objeto [EventEntity] que contiene los datos del nuevo evento a guardar.
     * Retorno: Un valor de tipo Long que representa el ID (PrimaryKey) autogenerado del nuevo registro.
     * Lógica interna: Ejecuta una consulta SQL INSERT de fondo de manera asíncrona (suspend function) para no bloquear el hilo principal (UI Thread).
     */
    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    /**
     * Propósito: Actualiza la información de un evento previamente guardado.
     * Parámetros:
     * - event: El objeto [EventEntity] modificado. Room usará la clave primaria (ID) para encontrar el registro original y reemplazar el resto de los campos.
     * Retorno: Ninguno.
     * Lógica interna: Ejecuta un UPDATE en la tabla 'events' buscando coincidencia por el ID del evento de forma asíncrona.
     */
    @Update
    suspend fun updateEvent(event: EventEntity)

    /**
     * Propósito: Elimina un evento específico de la base de datos.
     * Parámetros:
     * - event: El objeto [EventEntity] que se desea borrar.
     * Retorno: Ninguno.
     * Lógica interna: Ejecuta una instrucción DELETE en SQLite buscando la coincidencia por clave primaria. Se hace en un hilo secundario por ser suspend function.
     */
    @Delete
    suspend fun deleteEvent(event: EventEntity)

    /**
     * Propósito: Recupera absolutamente todos los eventos almacenados.
     * Parámetros: Ninguno.
     * Retorno: Un [Flow] que emite una lista reactiva de [EventEntity]. 
     * Lógica interna: Ejecuta `SELECT * FROM events`. Al retornar un Flow, Room mantiene un canal abierto y re-emitirá la lista actualizada automáticamente cada vez que se inserte, actualice o borre un registro en la tabla.
     */
    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    /**
     * Propósito: Recupera los eventos programados para una fecha específica.
     * Parámetros:
     * - date: Un String que representa la fecha a buscar (ej. "2026-06-06").
     * Retorno: [Flow] con la lista de eventos correspondientes a esa fecha.
     * Lógica interna: Ejecuta un SELECT filtrando por la columna 'fecha'. El flujo será reactivo a cambios.
     */
    @Query("SELECT * FROM events WHERE fecha = :date")
    fun getEventsByDate(date: String): Flow<List<EventEntity>>

    /**
     * Propósito: Obtiene eventos filtrados por categoría y/o por fecha de forma dinámica.
     * Parámetros:
     * - category: La categoría a buscar. Si es null, se ignora el filtro de categoría.
     * - date: La fecha a buscar. Si es null, se ignora el filtro de fecha.
     * Retorno: [Flow] con la lista de eventos filtrada reactivamente.
     * Lógica interna: Usa una lógica condicional SQL `(:category IS NULL OR categoria = :category)`. Si pasamos null, la primera parte es TRUE y anula el filtro, permitiendo consultas muy flexibles (ambos nulos = retorna todos los eventos).
     */
    @Query("SELECT * FROM events WHERE (:category IS NULL OR categoria = :category) AND (:date IS NULL OR fecha = :date)")
    fun getFilteredEvents(category: String?, date: String?): Flow<List<EventEntity>>

    /**
     * Propósito: Recupera los eventos que ocurran dentro de un lapso determinado de fechas.
     * Parámetros:
     * - startDate: La fecha inicial del rango.
     * - endDate: La fecha límite del rango.
     * Retorno: [Flow] con los eventos que caen dentro del intervalo, ordenados cronológicamente.
     * Lógica interna: Ejecuta un SELECT usando el operador `BETWEEN` de SQL y ordena los resultados primero por fecha ascendente y luego por hora ascendente (ORDER BY fecha ASC, hora ASC).
     */
    @Query("SELECT * FROM events WHERE fecha BETWEEN :startDate AND :endDate ORDER BY fecha ASC, hora ASC")
    fun getEventsBetweenDates(startDate: String, endDate: String): Flow<List<EventEntity>>

    /**
     * Propósito: Consulta avanzada que combina el filtrado opcional de categoría con un rango de fechas.
     * Parámetros:
     * - category: Categoría a filtrar, o null para omitir.
     * - startDate: Fecha inicial del rango, o null para no limitar el inicio.
     * - endDate: Fecha final del rango, o null para no limitar el fin.
     * Retorno: [Flow] con los eventos resultantes.
     * Lógica interna: Combina `IS NULL OR ...` con evaluaciones de `>=` y `<=`. De este modo, el motor SQLite procesa de manera segura y eficiente la solicitud dinámica. Se incluye ordenación por fecha y hora.
     */
    @Query("SELECT * FROM events WHERE (:category IS NULL OR categoria = :category) AND (:startDate IS NULL OR fecha >= :startDate) AND (:endDate IS NULL OR fecha <= :endDate) ORDER BY fecha ASC, hora ASC")
    fun getFilteredEventsAdvanced(category: String?, startDate: String?, endDate: String?): Flow<List<EventEntity>>

    /**
     * Propósito: Extrae un listado exclusivo de las fechas que tienen al menos un evento programado.
     * Parámetros: Ninguno.
     * Retorno: [Flow] emitiendo una lista de Strings (las fechas únicas).
     * Lógica interna: Usa `SELECT DISTINCT fecha` para evitar fechas duplicadas en la respuesta. Esto es muy útil y liviano para renderizar marcadores en una UI de calendario sin cargar todos los objetos EventEntity en memoria.
     */
    @Query("SELECT DISTINCT fecha FROM events")
    fun getAllDatesWithEvents(): Flow<List<String>>
}

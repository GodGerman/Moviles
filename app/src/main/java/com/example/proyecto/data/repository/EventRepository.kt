package com.example.proyecto.data.repository

import com.example.proyecto.data.local.EventDao
import com.example.proyecto.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Propósito: Repositorio para la gestión de Eventos. Actúa como la "única fuente de verdad" de los datos.
 * Rol en MVVM: Capa de Repositorio (Data Layer). Su rol es abstraer el origen de los datos (en este caso el DAO) para que el ViewModel no deba preocuparse por cómo ni de dónde se obtienen los eventos.
 * Interacciones: Es instanciado y utilizado por [com.example.proyecto.viewmodel.EventViewModel]. Internamente interactúa con [EventDao].
 */
class EventRepository(private val eventDao: EventDao) {
    
    /**
     * Variable reactiva que contiene el flujo constante de todos los eventos.
     * El ViewModel observará este Flow para actualizar la UI en tiempo real.
     */
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    /**
     * Propósito: Inserta un nuevo evento en la base de datos.
     * Parámetros:
     * - event: El [EventEntity] a insertar.
     * Retorno: Long que representa el ID del evento insertado.
     * Lógica interna: Delega la operación al [EventDao]. Se ejecuta de forma asíncrona (suspend).
     */
    suspend fun insert(event: EventEntity): Long {
        return eventDao.insertEvent(event)
    }

    /**
     * Propósito: Actualiza un evento existente.
     * Parámetros:
     * - event: El [EventEntity] con los datos modificados.
     * Retorno: Ninguno.
     * Lógica interna: Delega el trabajo asíncrono de actualización a [EventDao].
     */
    suspend fun update(event: EventEntity) {
        eventDao.updateEvent(event)
    }

    /**
     * Propósito: Elimina un evento de la base de datos.
     * Parámetros:
     * - event: El [EventEntity] a borrar.
     * Retorno: Ninguno.
     * Lógica interna: Delega el trabajo asíncrono de borrado a [EventDao].
     */
    suspend fun delete(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    /**
     * Propósito: Obtiene los eventos de una fecha en específico.
     * Parámetros:
     * - date: La fecha solicitada (String).
     * Retorno: [Flow] con la lista reactiva de los eventos de esa fecha.
     * Lógica interna: Llama al método respectivo del DAO. No es suspendida porque retorna un Flow, el cual es frío hasta que se observe.
     */
    fun getEventsByDate(date: String): Flow<List<EventEntity>> {
        return eventDao.getEventsByDate(date)
    }

    /**
     * Propósito: Filtra los eventos por categoría y/o fecha de forma flexible.
     * Parámetros:
     * - category: La categoría a buscar (o null para ignorarla).
     * - date: La fecha a buscar (o null para ignorarla).
     * Retorno: [Flow] con la lista reactiva de resultados del filtro.
     * Lógica interna: Llama al DAO pasando ambos parámetros.
     */
    fun getFilteredEvents(category: String?, date: String?): Flow<List<EventEntity>> {
        return eventDao.getFilteredEvents(category, date)
    }

    /**
     * Propósito: Obtiene todos los eventos en un rango específico de fechas.
     * Parámetros:
     * - startDate: Fecha inicial (inclusive).
     * - endDate: Fecha final (inclusive).
     * Retorno: [Flow] con los eventos que ocurran dentro del periodo indicado.
     * Lógica interna: Llama al DAO con el rango temporal definido.
     */
    fun getEventsBetweenDates(startDate: String, endDate: String): Flow<List<EventEntity>> {
        return eventDao.getEventsBetweenDates(startDate, endDate)
    }

    /**
     * Propósito: Búsqueda avanzada combinando categoría y rango de fechas.
     * Parámetros:
     * - category: La categoría buscada (o null para ignorar).
     * - startDate: Fecha inicial del rango (o null).
     * - endDate: Fecha final del rango (o null).
     * Retorno: [Flow] reactivo con los eventos resultantes que cumplen los criterios.
     * Lógica interna: Llama al método avanzado del DAO.
     */
    fun getFilteredEventsAdvanced(category: String?, startDate: String?, endDate: String?): Flow<List<EventEntity>> {
        return eventDao.getFilteredEventsAdvanced(category, startDate, endDate)
    }

    /**
     * Propósito: Recupera una lista de las fechas que tienen eventos programados (sin traer toda la información de cada evento).
     * Parámetros: Ninguno.
     * Retorno: [Flow] reactivo que emite la lista de fechas (Strings).
     * Lógica interna: Delega al DAO para obtener la consulta DISTINCT de fechas.
     */
    fun getAllDatesWithEvents(): Flow<List<String>> {
        return eventDao.getAllDatesWithEvents()
    }
}

package com.example.proyecto.data.repository

import com.example.proyecto.data.local.EventDao
import com.example.proyecto.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    suspend fun insert(event: EventEntity): Long {
        return eventDao.insertEvent(event)
    }

    suspend fun update(event: EventEntity) {
        eventDao.updateEvent(event)
    }

    suspend fun delete(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    fun getEventsByDate(date: String): Flow<List<EventEntity>> {
        return eventDao.getEventsByDate(date)
    }

    fun getFilteredEvents(category: String?, date: String?): Flow<List<EventEntity>> {
        return eventDao.getFilteredEvents(category, date)
    }

    fun getEventsBetweenDates(startDate: String, endDate: String): Flow<List<EventEntity>> {
        return eventDao.getEventsBetweenDates(startDate, endDate)
    }

    fun getFilteredEventsAdvanced(category: String?, startDate: String?, endDate: String?): Flow<List<EventEntity>> {
        return eventDao.getFilteredEventsAdvanced(category, startDate, endDate)
    }

    fun getAllDatesWithEvents(): Flow<List<String>> {
        return eventDao.getAllDatesWithEvents()
    }
}

package com.example.proyecto.data.repository

import com.example.proyecto.data.local.EventDao
import com.example.proyecto.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    suspend fun insert(event: EventEntity) {
        eventDao.insertEvent(event)
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
}

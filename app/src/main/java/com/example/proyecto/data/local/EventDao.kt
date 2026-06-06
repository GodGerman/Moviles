package com.example.proyecto.data.local

import androidx.room.*
import com.example.proyecto.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE fecha = :date")
    fun getEventsByDate(date: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE (:category IS NULL OR categoria = :category) AND (:date IS NULL OR fecha = :date)")
    fun getFilteredEvents(category: String?, date: String?): Flow<List<EventEntity>>

    @Query("SELECT DISTINCT fecha FROM events")
    fun getAllDatesWithEvents(): Flow<List<String>>
}

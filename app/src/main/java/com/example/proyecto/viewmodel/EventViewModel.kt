package com.example.proyecto.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.proyecto.R
import com.example.proyecto.data.local.AppDatabase
import com.example.proyecto.data.model.EventEntity
import com.example.proyecto.data.repository.EventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EventRepository
    val allEvents: LiveData<List<EventEntity>>
    val datesWithEvents: LiveData<List<String>>

    init {
        val eventDao = AppDatabase.getDatabase(application).eventDao()
        repository = EventRepository(eventDao)
        allEvents = repository.allEvents.asLiveData()
        datesWithEvents = repository.getAllDatesWithEvents().asLiveData()
        
        migrateLegacyDates()
    }

    private fun migrateLegacyDates() = viewModelScope.launch {
        try {
            repository.allEvents.first().forEach { event ->
                if (event.fecha.contains("/")) {
                    try {
                        val parts = event.fecha.split("/")
                        if (parts.size == 3) {
                            val newDate = String.format("%04d-%02d-%02d", parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                            repository.update(event.copy(fecha = newDate))
                        }
                    } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {}
    }

    fun insert(event: EventEntity) = viewModelScope.launch {
        repository.insert(event)
    }

    suspend fun insertAndReturnId(event: EventEntity): Long {
        return repository.insert(event)
    }

    fun update(event: EventEntity) = viewModelScope.launch {
        repository.update(event)
    }

    fun delete(event: EventEntity) = viewModelScope.launch {
        repository.delete(event)
    }

    fun getFilteredEvents(category: String?, date: String?): LiveData<List<EventEntity>> {
        val allCategoriesStr = getApplication<Application>().getString(R.string.all_categories)
        val cat = if (category == allCategoriesStr || category == null) null else category
        return repository.getFilteredEvents(cat, date).asLiveData()
    }

    fun getUpcomingEvents(): LiveData<List<EventEntity>> {
        val calendar = Calendar.getInstance()
        val startStr = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        calendar.add(Calendar.DAY_OF_MONTH, 4)
        val endStr = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        return repository.getEventsBetweenDates(startStr, endStr).asLiveData()
    }

    fun getFilteredEventsAdvanced(category: String?, startDate: String?, endDate: String?): LiveData<List<EventEntity>> {
        val allCategoriesStr = getApplication<Application>().getString(R.string.all_categories)
        val cat = if (category == allCategoriesStr || category == null) null else category
        return repository.getFilteredEventsAdvanced(cat, startDate, endDate).asLiveData()
    }

    fun getEventsByDate(date: String): LiveData<List<EventEntity>> {
        return repository.getEventsByDate(date).asLiveData()
    }
}


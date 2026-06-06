package com.example.proyecto.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.proyecto.data.local.AppDatabase
import com.example.proyecto.data.model.EventEntity
import com.example.proyecto.data.repository.EventRepository
import kotlinx.coroutines.launch

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EventRepository
    val allEvents: LiveData<List<EventEntity>>

    init {
        val eventDao = AppDatabase.getDatabase(application).eventDao()
        repository = EventRepository(eventDao)
        allEvents = repository.allEvents.asLiveData()
    }

    fun insert(event: EventEntity) = viewModelScope.launch {
        repository.insert(event)
    }

    fun update(event: EventEntity) = viewModelScope.launch {
        repository.update(event)
    }

    fun delete(event: EventEntity) = viewModelScope.launch {
        repository.delete(event)
    }

    fun getFilteredEvents(category: String?, date: String?): LiveData<List<EventEntity>> {
        val cat = if (category == "Todas las categorías" || category == null) null else category
        return repository.getFilteredEvents(cat, date).asLiveData()
    }

    fun getEventsByDate(date: String): LiveData<List<EventEntity>> {
        return repository.getEventsByDate(date).asLiveData()
    }
}

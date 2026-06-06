package com.example.proyecto.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.proyecto.R
import com.example.proyecto.data.local.AppDatabase
import com.example.proyecto.data.model.EventEntity
import com.example.proyecto.data.repository.EventRepository
import kotlinx.coroutines.launch

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EventRepository
    val allEvents: LiveData<List<EventEntity>>
    val datesWithEvents: LiveData<List<String>>

    init {
        val eventDao = AppDatabase.getDatabase(application).eventDao()
        repository = EventRepository(eventDao)
        allEvents = repository.allEvents.asLiveData()
        datesWithEvents = repository.getAllDatesWithEvents().asLiveData()
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
        val allCategoriesStr = getApplication<Application>().getString(R.string.all_categories)
        val cat = if (category == allCategoriesStr || category == null) null else category
        return repository.getFilteredEvents(cat, date).asLiveData()
    }

    fun getEventsByDate(date: String): LiveData<List<EventEntity>> {
        return repository.getEventsByDate(date).asLiveData()
    }
}


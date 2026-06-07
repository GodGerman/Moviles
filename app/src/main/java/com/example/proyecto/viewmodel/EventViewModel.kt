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

/**
 * Propósito: ViewModel principal para gestionar los datos de la interfaz de usuario (UI).
 * Rol en MVVM: Capa de Lógica de Presentación (ViewModel). Sobrevive a cambios de configuración (como rotar la pantalla) y provee a la UI de datos frescos. Mantiene el estado de la pantalla.
 * Interacciones: Actúa como puente entre la Capa de Datos ([EventRepository]) y la UI (Fragmentos y Activities). Expone los flujos de datos a través de LiveData.
 */
class EventViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: EventRepository
    
    /** 
     * LiveData reactivo que contiene todos los eventos. La UI lo observa para redibujar listas de forma automática.
     */
    val allEvents: LiveData<List<EventEntity>>
    
    /** 
     * LiveData con las fechas distintas para el calendario.
     */
    val datesWithEvents: LiveData<List<String>>

    init {
        // Inicializamos la base de datos y el repositorio al construir el ViewModel
        val eventDao = AppDatabase.getDatabase(application).eventDao()
        repository = EventRepository(eventDao)
        
        // Convertimos los Flow (corrientes de datos frías) a LiveData (reactivos conscientes del ciclo de vida).
        // asLiveData() maneja automáticamente el ciclo de vida, evitando actualizaciones cuando la app está en segundo plano.
        allEvents = repository.allEvents.asLiveData()
        datesWithEvents = repository.getAllDatesWithEvents().asLiveData()
        
        // Ejecuta migración preventiva por si la base de datos viene de una versión antigua
        migrateLegacyDates()
    }

    /**
     * Propósito: Función interna que arregla el formato de fecha antiguo (DD/MM/YYYY) al nuevo formato ordenable (YYYY-MM-DD).
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna: 
     * 1. Inicia una corrutina en `viewModelScope` (se cancela sola si el ViewModel muere).
     * 2. Recolecta la primera emisión (`first()`) del Flow del repositorio.
     * 3. Itera sobre los eventos; si detecta el separador "/", divide el texto y reordena las piezas de día, mes, año.
     * 4. Llama a la actualización del repositorio con el nuevo formato estandarizado.
     */
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

    /**
     * Propósito: Inserta un evento en la base de datos a través del Repositorio.
     * Parámetros: 
     * - event: El objeto [EventEntity] a guardar.
     * Retorno: Ninguno.
     * Lógica interna: Ejecuta el método suspendido de inserción dentro de `viewModelScope.launch` para no bloquear la interfaz gráfica.
     */
    fun insert(event: EventEntity) = viewModelScope.launch {
        repository.insert(event)
    }

    /**
     * Propósito: Inserta un evento y devuelve su ID generado, pausando la ejecución de la corrutina que la llama.
     * Parámetros:
     * - event: El objeto [EventEntity] a guardar.
     * Retorno: Valor Long representando el ID primario.
     * Lógica interna: Delega directamente al Repositorio. Es suspendida (suspend function) porque quien la llama necesita conocer el ID inmediatamente para programar notificaciones.
     */
    suspend fun insertAndReturnId(event: EventEntity): Long {
        return repository.insert(event)
    }

    /**
     * Propósito: Actualiza la información de un evento ya existente.
     * Parámetros:
     * - event: Evento modificado por el usuario.
     * Retorno: Ninguno.
     * Lógica interna: Ejecuta en el `viewModelScope` la operación delegada al Repositorio.
     */
    fun update(event: EventEntity) = viewModelScope.launch {
        repository.update(event)
    }

    /**
     * Propósito: Elimina un evento de la base de datos.
     * Parámetros:
     * - event: Evento que se va a borrar.
     * Retorno: Ninguno.
     * Lógica interna: Ejecuta en el `viewModelScope` la operación de borrado delegada al Repositorio.
     */
    fun delete(event: EventEntity) = viewModelScope.launch {
        repository.delete(event)
    }

    /**
     * Propósito: Filtra eventos por una categoría o una fecha en específica.
     * Parámetros:
     * - category: String con el nombre de la categoría (ej. "Trabajo").
     * - date: String con la fecha (ej. "2026-06-06").
     * Retorno: Un [LiveData] con la lista resultante.
     * Lógica interna: Obtiene la cadena representativa de "Todas" las categorías. Si la categoría coincide con "Todas" o es nula, envía un `null` al repositorio para que el DAO ignore dicho filtro.
     */
    fun getFilteredEvents(category: String?, date: String?): LiveData<List<EventEntity>> {
        val allCategoriesStr = getApplication<Application>().getString(R.string.all_categories)
        val cat = if (category == allCategoriesStr || category == null) null else category
        return repository.getFilteredEvents(cat, date).asLiveData()
    }

    /**
     * Propósito: Obtiene los eventos programados para los próximos 4 días contando desde hoy.
     * Parámetros: Ninguno.
     * Retorno: [LiveData] con los próximos eventos.
     * Lógica interna: 
     * 1. Instancia `Calendar` para obtener la fecha y mes de hoy.
     * 2. Modifica el calendario sumando 4 días.
     * 3. Formatea ambos límites temporales y retorna la consulta `getEventsBetweenDates` convertida a LiveData.
     */
    fun getUpcomingEvents(): LiveData<List<EventEntity>> {
        val calendar = Calendar.getInstance()
        val startStr = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        calendar.add(Calendar.DAY_OF_MONTH, 4)
        val endStr = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        return repository.getEventsBetweenDates(startStr, endStr).asLiveData()
    }

    /**
     * Propósito: Permite filtrar eventos detalladamente por categoría en un rango de fechas.
     * Parámetros:
     * - category: String con la categoría a buscar.
     * - startDate: Fecha inicial de búsqueda.
     * - endDate: Fecha final límite.
     * Retorno: Un [LiveData] reactivo.
     * Lógica interna: Anula el valor de `category` si es "Todas" e invoca el método avanzado del Repositorio.
     */
    fun getFilteredEventsAdvanced(category: String?, startDate: String?, endDate: String?): LiveData<List<EventEntity>> {
        val allCategoriesStr = getApplication<Application>().getString(R.string.all_categories)
        val cat = if (category == allCategoriesStr || category == null) null else category
        return repository.getFilteredEventsAdvanced(cat, startDate, endDate).asLiveData()
    }

    /**
     * Propósito: Recupera exclusivamente eventos de un día en particular.
     * Parámetros:
     * - date: La fecha solicitada (String).
     * Retorno: [LiveData] con la respuesta.
     * Lógica interna: Conversión estándar de Flow a LiveData proveniente del Repositorio.
     */
    fun getEventsByDate(date: String): LiveData<List<EventEntity>> {
        return repository.getEventsByDate(date).asLiveData()
    }
}

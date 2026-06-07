package com.example.proyecto.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.ui.home.EventAdapter
import com.example.proyecto.viewmodel.EventViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Propósito: Fragmento que renderiza un Calendario interactivo modularizado hecho a medida.
 * Rol en MVVM: Capa de Presentación (UI Layer). El componente `CalendarView` pre-construido de Android es notoriamente opaco y difícil de personalizar; en su lugar, esta clase calcula matemáticamente una matriz de días que es orquestada y pintada por un `GridLayoutManager`. Recopila datos cruzados usando al `EventViewModel`.
 * Interacciones: Se sirve del [EventViewModel]. Construye dos listas: Una matriz usando [CalendarAdapter] y una subsecuente lista para resultados renderizada por [EventAdapter]. Puede desplegar el [com.example.proyecto.ui.consult.EventDetailsBottomSheet].
 */
class CalendarFragment : Fragment() {

    private val eventViewModel: EventViewModel by activityViewModels()
    
    // Adaptador listado inferior de tareas resultantes para el día seleccionado
    private lateinit var eventAdapter: EventAdapter
    
    // Adaptador tipo Grid que aloja los 35/42 bloques de los días del mes
    private lateinit var calendarAdapter: CalendarAdapter

    // Pivote maestro; memoria RAM que almacena en qué mes y año tiene el usuario posicionado su visor
    private var currentCalendar: Calendar = Calendar.getInstance()
    
    // Rastreador del día numérico (1-31) que el usuario presionó (-1 == deseleccionado).
    private var selectedDay: Int = -1
    
    // Colección estructural (Hash Set sin orden secuencial) diseñada para dar respuestas booleanas veloces en O(1) de si un string de fecha se haya aquí.
    private var datesWithEventsSet: Set<String> = emptySet()

    private lateinit var tvMonthYear: TextView

    /**
     * Propósito: Inicialización primaria y uniones lógicas de Vistas-Controladores.
     * Parámetros:
     * - inflater: Herramienta XML.
     * - container: Nodo base.
     * - savedInstanceState: Caché de la app.
     * Retorno: Objeto de Vista [View].
     * Lógica interna:
     * 1. Enlaza variables gráficas y dos adaptadores distintos al mismo Layout.
     * 2. El `rvCalendarGrid` divide el listado obligatoriamente en 7 columnas inflexibles emulando el DOM. l-m-m-j-v-s-d.
     * 3. Mapea la lógica de botones Mes Atrás y Mes Adelante para alterar la matemática de la fecha central e instruir borrados temporales.
     * 4. Lanza una escucha global en LiveData (`datesWithEvents`) desde Room. Su único propósito es procesar los strings a estándar ISO y re-empaquetarlos como Set súper veloz, para que, instantes después, mande a repintar y aparezcan/desaparezcan puntitos.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_calendar, container, false)

        tvMonthYear = root.findViewById(R.id.tv_month_year)
        val btnPrev: ImageButton = root.findViewById(R.id.btn_prev_month)
        val btnNext: ImageButton = root.findViewById(R.id.btn_next_month)
        val rvCalendarGrid: RecyclerView = root.findViewById(R.id.rv_calendar_grid)
        val rvCalendarEvents: RecyclerView = root.findViewById(R.id.rv_calendar_events)

        // Callback invocado a través del CalendarAdapter cuando el usuario hace Tap a una de las celdas
        calendarAdapter = CalendarAdapter { dayOfMonth ->
            selectedDay = dayOfMonth
            // El usuario tocó algo; reescribir toda la cuadrícula en color y lanzar la query SQL en fondo
            updateCalendarGrid()
            loadEventsForSelectedDay()
        }
        rvCalendarGrid.adapter = calendarAdapter
        // Dividir la lista matemática en 7 columnas equivalentes a L-M-M-J-V-S-D.
        rvCalendarGrid.layoutManager = GridLayoutManager(context, 7)

        eventAdapter = EventAdapter(
            onItemClick = { event ->
                val bottomSheet = com.example.proyecto.ui.consult.EventDetailsBottomSheet.newInstance(event)
                bottomSheet.show(childFragmentManager, "EventDetailsBottomSheet")
            }
        )
        rvCalendarEvents.adapter = eventAdapter
        rvCalendarEvents.layoutManager = LinearLayoutManager(context)

        // Botón "Previo"
        btnPrev.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            selectedDay = -1 // Limpiar rastros fantasmas al brincar al mes pasado
            updateCalendarGrid()
            eventAdapter.submitList(emptyList()) // Esconder info obsoleta
        }

        // Botón "Siguiente"
        btnNext.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            selectedDay = -1
            updateCalendarGrid()
            eventAdapter.submitList(emptyList())
        }

        // Default: Apuntar autómata a la fecha del día corriente.
        selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        // Obtenemos una vez sola (Single Source of Truth) todas las fechas de la DB.
        // A medida que muta, repinta puntos visuales, transformando una Lista lenta en un HashSet de acceso instantáneo
        eventViewModel.datesWithEvents.observe(viewLifecycleOwner) { dates ->
            datesWithEventsSet = dates.mapNotNull { dateString ->
                try {
                    if (dateString.contains("-")) {
                        dateString 
                    } else {
                        // Por si se migra código heredado con DD/MM/YYYY
                        val parts = dateString.split("/")
                        if (parts.size == 3) {
                            String.format("%04d-%02d-%02d", parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                        } else null
                    }
                } catch (e: Exception) { null }
            }.toSet()
            updateCalendarGrid()
        }

        // Llamada de inicialización en crudo
        loadEventsForSelectedDay()

        return root
    }

    /**
     * Propósito: Algoritmo central para calcular dinámicamente y reconstruir la matriz de 35/42 días.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Extracción de Mes y Año base.
     * 2. Actualización de Titulo "Label" de cabecera.
     * 3. Busca el "día de la semana (L, M, M)" en que recae el número "1" de ese mes con `get(Calendar.DAY_OF_WEEK)`.
     * 4. Inyecta casillas "fantasma" (`dayOfMonth = 0`) para rellenar los hoyos previos al primer día de forma ordenada.
     * 5. Itera e inyecta la numeración sólida usando bucles nativos (ej: `1..31`), resolviendo matemáticamente si un ciclo específico debe iluminarse porque es el presente o cruzarlo contra el HashSet para inyectar `hasEvents`.
     * 6. Dispara la compilación mandando la lista terminada mediante `calendarAdapter.submitList(days)`.
     */
    private fun updateCalendarGrid() {
        val today = Calendar.getInstance()
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)

        // Traducción de Localidad del Mes: "Abril 2026"
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
        tvMonthYear.text = sdf.format(currentCalendar.time).replaceFirstChar { it.uppercase() }

        // Localizar offset de desfase inicial del mes
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Ej: Domingo es 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH) // Límite real contemplando Años Bisiestos

        val days = mutableListOf<CalendarDay>()

        // PASO 1: Calcular e indexar casillas muertas a la izquierda para cuadrar tabla.
        val emptyDays = firstDayOfWeek - 1
        for (i in 0 until emptyDays) {
            days.add(CalendarDay(dayOfMonth = 0, isToday = false, isSelected = false, hasEvents = false))
        }

        // PASO 2: Agregar iterativamente del 1 al N.
        for (day in 1..daysInMonth) {
            val isToday = (day == today.get(Calendar.DAY_OF_MONTH)
                    && month == today.get(Calendar.MONTH)
                    && year == today.get(Calendar.YEAR))
            
            val isSelected = day == selectedDay

            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
            val hasEvents = datesWithEventsSet.contains(dateStr)

            days.add(CalendarDay(dayOfMonth = day, isToday = isToday, isSelected = isSelected, hasEvents = hasEvents))
        }

        calendarAdapter.submitList(days)
    }

    /**
     * Propósito: Desencadenar la instrucción a la Base de Datos para recabar el extracto detallado de los elementos pautados para una determinada casilla.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna: Formatea los punteros locales hacia ISO SQL; requiere asincrónicamente `getEventsByDate(dateStr)` al Dao mediador y canaliza transparentemente los fragmentos recibidos a la lista inferior `eventAdapter`.
     */
    private fun loadEventsForSelectedDay() {
        if (selectedDay <= 0) return 
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)
        val dateStr = String.format("%04d-%02d-%02d", year, month + 1, selectedDay)

        eventViewModel.getEventsByDate(dateStr).observe(viewLifecycleOwner) { events ->
            eventAdapter.submitList(events)
        }
    }
}

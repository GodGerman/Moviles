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

class CalendarFragment : Fragment() {

    private val eventViewModel: EventViewModel by activityViewModels()
    private lateinit var eventAdapter: EventAdapter
    private lateinit var calendarAdapter: CalendarAdapter

    private var currentCalendar: Calendar = Calendar.getInstance()
    private var selectedDay: Int = -1
    private var datesWithEventsSet: Set<String> = emptySet()

    private lateinit var tvMonthYear: TextView

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

        // Configurar adapter del grid del calendario
        calendarAdapter = CalendarAdapter { dayOfMonth ->
            selectedDay = dayOfMonth
            updateCalendarGrid()
            loadEventsForSelectedDay()
        }
        rvCalendarGrid.adapter = calendarAdapter
        rvCalendarGrid.layoutManager = GridLayoutManager(context, 7)

        // Configurar adapter de la lista de eventos
        eventAdapter = EventAdapter(
            onItemClick = { event ->
                val bottomSheet = com.example.proyecto.ui.consult.EventDetailsBottomSheet.newInstance(event)
                bottomSheet.show(childFragmentManager, "EventDetailsBottomSheet")
            }
        )
        rvCalendarEvents.adapter = eventAdapter
        rvCalendarEvents.layoutManager = LinearLayoutManager(context)

        // Navegación entre meses
        btnPrev.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            selectedDay = -1
            updateCalendarGrid()
            eventAdapter.submitList(emptyList())
        }

        btnNext.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            selectedDay = -1
            updateCalendarGrid()
            eventAdapter.submitList(emptyList())
        }

        // Seleccionar el día de hoy al inicio
        selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        // Observar fechas con eventos para actualizar las marcas del calendario
        eventViewModel.datesWithEvents.observe(viewLifecycleOwner) { dates ->
            datesWithEventsSet = dates.toSet()
            updateCalendarGrid()
        }

        // Cargar eventos del día de hoy
        loadEventsForSelectedDay()

        return root
    }

    private fun updateCalendarGrid() {
        val today = Calendar.getInstance()
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)

        // Actualizar título del mes
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
        tvMonthYear.text = sdf.format(currentCalendar.time).replaceFirstChar { it.uppercase() }

        // Calcular primer día del mes y total de días
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Dom, 2=Lun, ...
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val days = mutableListOf<CalendarDay>()

        // Agregar celdas vacías antes del primer día
        val emptyDays = firstDayOfWeek - 1 // Calendar.SUNDAY = 1
        for (i in 0 until emptyDays) {
            days.add(CalendarDay(dayOfMonth = 0, isToday = false, isSelected = false, hasEvents = false))
        }

        // Agregar los días del mes
        for (day in 1..daysInMonth) {
            val isToday = (day == today.get(Calendar.DAY_OF_MONTH)
                    && month == today.get(Calendar.MONTH)
                    && year == today.get(Calendar.YEAR))
            val isSelected = day == selectedDay

            // Construir la fecha con el mismo formato que se guarda en Room: "d/M/yyyy"
            val dateStr = "$day/${month + 1}/$year"
            val hasEvents = datesWithEventsSet.contains(dateStr)

            days.add(CalendarDay(dayOfMonth = day, isToday = isToday, isSelected = isSelected, hasEvents = hasEvents))
        }

        calendarAdapter.submitList(days)
    }

    private fun loadEventsForSelectedDay() {
        if (selectedDay <= 0) return
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)
        val dateStr = "$selectedDay/${month + 1}/$year"

        eventViewModel.getEventsByDate(dateStr).observe(viewLifecycleOwner) { events ->
            eventAdapter.submitList(events)
        }
    }
}

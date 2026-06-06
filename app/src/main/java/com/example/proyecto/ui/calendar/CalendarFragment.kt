package com.example.proyecto.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.ui.home.EventAdapter
import com.example.proyecto.viewmodel.EventViewModel

class CalendarFragment : Fragment() {

    private val eventViewModel: EventViewModel by activityViewModels()
    private lateinit var adapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_calendar, container, false)

        val calendarView: CalendarView = root.findViewById(R.id.calendar_view)
        val recyclerView: RecyclerView = root.findViewById(R.id.rv_calendar_events)
        
        adapter = EventAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            loadEventsForDate(selectedDate)
        }

        // Cargar eventos para el día de hoy al inicio
        val today = java.util.Calendar.getInstance()
        val todayStr = "${today.get(java.util.Calendar.DAY_OF_MONTH)}/${today.get(java.util.Calendar.MONTH) + 1}/${today.get(java.util.Calendar.YEAR)}"
        loadEventsForDate(todayStr)

        return root
    }

    private fun loadEventsForDate(date: String) {
        eventViewModel.getEventsByDate(date).observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
        }
    }
}

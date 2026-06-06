package com.example.proyecto.ui.calendar

data class CalendarDay(
    val dayOfMonth: Int,       // 0 = celda vacía (padding del mes)
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasEvents: Boolean
)

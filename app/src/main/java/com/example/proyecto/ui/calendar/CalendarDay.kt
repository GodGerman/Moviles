package com.example.proyecto.ui.calendar

/**
 * Propósito: Clase de datos pura (Data Class) que actúa como modelo individual para representar una casilla en el calendario.
 * Rol en MVVM: Modelo de Vista UI. Su única función es guardar en memoria los estados visuales transitorios (booleans) que el `CalendarAdapter` procesará para determinar el color, borde, o visibilidad de indicadores en una celda.
 * Interacciones: Generado masivamente por [CalendarFragment] para enviarse a [CalendarAdapter].
 */
data class CalendarDay(
    /** 
     * El número del día del mes (1 a 31). Un valor numérico de 0 se interpreta como una celda vacía/invisible, usada para rellenar los espacios en blanco antes de que empiece el mes.
     */
    val dayOfMonth: Int,
    
    /** 
     * Bandera verdadera si esta celda corresponde matemáticamente con precisión al día de hoy del reloj del sistema. 
     */
    val isToday: Boolean,
    
    /** 
     * Bandera verdadera si el usuario tiene tocada y remarcada activamente esta celda en este momento en pantalla. 
     */
    val isSelected: Boolean,
    
    /** 
     * Bandera verdadera si la base de datos (Room) reportó asíncronamente que existe al menos 1 tarea configurada para ocurrir en la fecha de esta celda, lo que disparará el dibujado de un pequeño punto indicador debajo del número.
     */
    val hasEvents: Boolean
)

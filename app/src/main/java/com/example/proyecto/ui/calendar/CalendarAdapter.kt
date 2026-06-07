package com.example.proyecto.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R

/**
 * Propósito: Adaptador estructural que dibuja de forma granular CADA UNA de las 35/42 cuadrículas pequeñas que conforman el Mes en curso.
 * Rol en MVVM: Capa de Presentación (UI Layer). Funciona como un orquestador de componentes visuales; transforma iterativamente los modelos estáticos [CalendarDay] en `ViewHolders` interactivos y con color de fondo dinámico.
 * Interacciones: Desplegado y controlado por [com.example.proyecto.ui.calendar.CalendarFragment]. Mapea e infla el archivo de diseño `item_calendar_day.xml`.
 */
class CalendarAdapter(
    // Función anónima o Lambda que actúa como puente para notificar al Fragmento Padre que un número de día en específico fue tocado por el usuario
    private val onDayClick: (Int) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    // Caché local que mantiene las celdas del mes provistas por el Fragment.
    private var days: List<CalendarDay> = emptyList()

    /**
     * Propósito: Recibe la nueva lista de días y ordena a Android que repinte completamente la cuadrícula del calendario.
     * Parámetros:
     * - newDays: Listado de datos [CalendarDay] con la matemática del mes.
     * Retorno: Ninguno.
     * Lógica interna: Sobreescribe la caché. Se utiliza obligatoriamente `notifyDataSetChanged()` de golpe debido a que en aplicaciones de calendario, la transición inter-mensual requiere que el 100% de las celdas se desdibujen o recoloquen a la vez.
     */
    fun submitList(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    /**
     * Propósito: Materializa el cascarón vacío del contenedor de días bajo demanda.
     * Parámetros:
     * - parent: ViewGroup.
     * - viewType: Int.
     * Retorno: [DayViewHolder] listo.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    /**
     * Propósito: Enlaza (Bind) el dato numérico a la tarjeta individual.
     */
    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    /**
     * Retorna el número estricto de elementos cartográficos generados.
     */
    override fun getItemCount(): Int = days.size

    /**
     * Clase interna que retiene referenciadas permanentemente las etiquetas de vista (Evita findViewByID cíclicos y masivos).
     */
    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
        
        // Un pequeño punto coloreado incrustado que aparece debajo del número para indicar tareas
        private val viewIndicator: View = itemView.findViewById(R.id.view_event_indicator)

        /**
         * Propósito: Setea las propiedades estéticas y de control de un día.
         * Parámetros:
         * - day: La instancia con booleanos [CalendarDay].
         * Retorno: Ninguno.
         * Lógica interna:
         * 1. Parsea el número 0 como una "celda invisible" quitando listeners para mantener el formato matricial vacío.
         * 2. Asigna el texto numérico.
         * 3. Establece jerarquía de diseño (Background y Stroke) aplicando reglas de color de Material Design con condicional `when`: Si es seleccionado, es hoy, o es normal.
         * 4. Altera la propiedad `visibility` del indicador `viewIndicator` dependiendo si la DB advirtió `hasEvents`.
         * 5. Adjunta el callback `onDayClick` sobre `itemView`.
         */
        fun bind(day: CalendarDay) {
            val context = itemView.context

            // Lógica para celdas invisibles/vacías (padding matemático antes del 1er día del mes)
            if (day.dayOfMonth == 0) {
                tvDayNumber.text = ""
                tvDayNumber.background = null
                viewIndicator.visibility = View.INVISIBLE
                itemView.setOnClickListener(null)
                // Evitamos sonido o reacción física (ripple) de feedback al usuario al tocar los espacios de relleno
                itemView.isClickable = false 
                return
            }

            // Inyectar el número impreso
            tvDayNumber.text = day.dayOfMonth.toString()

            // Asignación de Estilos Dinámicos basados en la jerarquía visual de prioridad
            when {
                // Prioridad 1: Si el usuario la tocó, se pinta completamente un círculo sólido
                day.isSelected -> {
                    tvDayNumber.setBackgroundResource(R.drawable.selected_day_bg)
                    tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                // Prioridad 2: Si es el día de hoy exacto, se le pone solamente un borde (stroke) coloreado
                day.isToday -> {
                    tvDayNumber.setBackgroundResource(R.drawable.today_day_bg)
                    tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.calendar_today_stroke))
                }
                // Caso Base general: Un día ordinario sin selección, texto oscuro sobre transparente
                else -> {
                    tvDayNumber.background = null
                    tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
            }

            // Maneja visibilidad del punto inferior
            viewIndicator.visibility = if (day.hasEvents) View.VISIBLE else View.INVISIBLE

            // Propagar clics al fragmento controlador para que solicite datos
            itemView.setOnClickListener {
                onDayClick(day.dayOfMonth)
            }
        }
    }
}

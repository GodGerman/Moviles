package com.example.proyecto.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R

class CalendarAdapter(
    private val onDayClick: (Int) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun submitList(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
        private val viewIndicator: View = itemView.findViewById(R.id.view_event_indicator)

        fun bind(day: CalendarDay) {
            val context = itemView.context

            if (day.dayOfMonth == 0) {
                // Celda vacía (padding antes del primer día del mes)
                tvDayNumber.text = ""
                tvDayNumber.background = null
                viewIndicator.visibility = View.INVISIBLE
                itemView.setOnClickListener(null)
                itemView.isClickable = false
                return
            }

            tvDayNumber.text = day.dayOfMonth.toString()

            // Fondo según estado
            when {
                day.isSelected -> {
                    tvDayNumber.setBackgroundResource(R.drawable.selected_day_bg)
                    tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
                }
                day.isToday -> {
                    tvDayNumber.setBackgroundResource(R.drawable.today_day_bg)
                    tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.calendar_today_stroke))
                }
                else -> {
                    tvDayNumber.background = null
                    tvDayNumber.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
            }

            // Indicador de eventos
            viewIndicator.visibility = if (day.hasEvents) View.VISIBLE else View.INVISIBLE

            // Click listener
            itemView.setOnClickListener {
                onDayClick(day.dayOfMonth)
            }
        }
    }
}

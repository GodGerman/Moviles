package com.example.proyecto.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.data.model.EventEntity

class EventAdapter : ListAdapter<EventEntity, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_datetime)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_location_contact)

        fun bind(event: EventEntity) {
            tvCategory.text = event.categoria
            tvStatus.text = event.estatus
            tvDateTime.text = "${event.fecha} | ${event.hora}"
            tvDescription.text = event.descripcion
            tvInfo.text = "Ubicación: ${event.ubicacion_lat},${event.ubicacion_lng} | Persona: ${event.contacto_nombre}"
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<EventEntity>() {
        override fun areItemsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean = oldItem == newItem
    }
}

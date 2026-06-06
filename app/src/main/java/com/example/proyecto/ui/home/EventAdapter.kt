package com.example.proyecto.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.data.model.EventEntity

class EventAdapter(
    private val onItemClick: ((EventEntity) -> Unit)? = null,
    private val onEditClick: ((EventEntity) -> Unit)? = null
) : ListAdapter<EventEntity, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
        
        if (onItemClick != null) {
            holder.itemView.setOnClickListener {
                onItemClick.invoke(event)
            }
        }
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_datetime)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_location_contact)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit_event)

        fun bind(event: EventEntity) {
            val context = itemView.context
            tvCategory.text = event.categoria
            tvStatus.text = event.estatus
            tvDateTime.text = "${event.fecha} | ${event.hora}"
            tvDescription.text = event.descripcion
            tvInfo.text = context.getString(
                R.string.item_info_format,
                event.ubicacion_lat,
                event.ubicacion_lng,
                event.contacto_nombre
            )

            // Color del badge de estatus según valor
            val statusColors = context.resources.getStringArray(R.array.status_array)
            val bgColor = when (event.estatus) {
                statusColors[0] -> R.color.status_pending    // Pendiente
                statusColors[1] -> R.color.status_done       // Realizado
                statusColors[2] -> R.color.status_postponed  // Aplazado
                else -> R.color.status_pending
            }
            val drawable = tvStatus.background?.mutate()
            drawable?.setTint(ContextCompat.getColor(context, bgColor))
            tvStatus.background = drawable

            // Mostrar botón de editar solo si se proporcionó callback
            if (onEditClick != null) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener {
                    onEditClick.invoke(event)
                }
            } else {
                btnEdit.visibility = View.GONE
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<EventEntity>() {
        override fun areItemsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean = oldItem == newItem
    }
}

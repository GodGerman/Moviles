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

/**
 * Propósito: Adaptador dinámico para listas de Eventos (RecyclerView).
 * Rol en MVVM: Capa de Presentación (UI Layer). Actúa como intermediario o traductor entre las colecciones reactivas inyectadas (`List<EventEntity>`) y el motor de dibujo `RecyclerView`. Utiliza `ListAdapter` y `DiffUtil` para ofrecer una interfaz altamente responsiva con animaciones atómicas a nivel elemento sin repintados completos de pantalla.
 * Interacciones: Se incrusta directamente en componentes como [com.example.proyecto.ui.home.HomeFragment] y [com.example.proyecto.ui.consult.ConsultEventsFragment]. Mapea el XML `item_event.xml`.
 */
class EventAdapter(
    // Función lambda que se ejecutará si el usuario toca la tarjeta completa (para ver detalles)
    private val onItemClick: ((EventEntity) -> Unit)? = null,
    // Función lambda opcional que se ejecutará si el usuario toca el botón de "Editar"
    private val onEditClick: ((EventEntity) -> Unit)? = null
) : ListAdapter<EventEntity, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    /**
     * Propósito: Materializa un nuevo molde genérico de interfaz cada que el sistema requiere mostrar en el área visible una celda nueva que no existía.
     * Parámetros:
     * - parent: Vista Padre contenedora.
     * - viewType: Categorización de diseño (Opcional en ListAdapter simples).
     * Retorno: Objeto encapsulado [EventViewHolder].
     * Lógica interna: Utiliza el `LayoutInflater` acoplado al Context local para descomprimir un `item_event` e incrustarlo en su propio controlador (ViewHolder).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    /**
     * Propósito: Liga ("Bind") un objeto estricto [EventEntity] hacia un cascarón visual en blanco del [EventViewHolder] durante ciclos rápidos de scroll.
     * Parámetros:
     * - holder: ViewHolder listo de la jerarquía.
     * - position: Posición numérica matemática del elemento a pintar dentro del Arreglo base.
     * Retorno: Ninguno.
     * Lógica interna: Extrae el evento en particular a través de `getItem(position)`, se lo traspasa a un bind personalizado interno del holder, y condiciona lógicas de clicks anónimas si es que el constructor inicial las dotó en su instanciación.
     */
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
        
        if (onItemClick != null) {
            holder.itemView.setOnClickListener {
                onItemClick.invoke(event)
            }
        }
    }

    /**
     * Propósito: Clase interior para mantener ("Sostener/Cachear") referencias rígidas a los hijos internos de la vista general; evitando múltiples e ineficientes lecturas `findViewById()`.
     */
    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_datetime)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvInfo: TextView = itemView.findViewById(R.id.tv_location_contact)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit_event)

        /**
         * Propósito: Imprime o sustituye el texto, estado y los fondos visuales con los valores en bruto del [EventEntity] suministrado.
         * Parámetros: 
         * - event: Componente individual con los datos pre-cargados a inyectar en XML.
         * Retorno: Ninguno.
         * Lógica interna: Aplica formato directo a cadenas, maneja variables condicionales para determinar el background del botón de Estatus e incluso esconde el botón de edición ("View.GONE") si el fragmento no autoriza su presencia en esa vista.
         */
        fun bind(event: EventEntity) {
            val context = itemView.context
            tvCategory.text = event.categoria
            tvStatus.text = event.estatus
            
            var displayDate = event.fecha
            try {
                // Formateamos la fecha ISO universal para mostrarla de forma localmente legible (DD/MM/YYYY)
                val parts = event.fecha.split("-")
                if (parts.size == 3) {
                    displayDate = String.format("%02d/%02d/%04d", parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                }
            } catch (e: Exception) {}
            
            tvDateTime.text = "$displayDate | ${event.hora}"
            tvDescription.text = event.descripcion
            tvInfo.text = context.getString(
                R.string.item_info_format,
                event.ubicacion_lat,
                event.ubicacion_lng,
                event.contacto_nombre
            )

            val statusColors = context.resources.getStringArray(R.array.status_array)
            val bgColor = when (event.estatus) {
                statusColors[0] -> R.color.status_pending    // Pendiente (Naranja)
                statusColors[1] -> R.color.status_done       // Realizado (Verde)
                statusColors[2] -> R.color.status_postponed  // Aplazado (Gris/Rojo)
                else -> R.color.status_pending
            }
            
            // Mutamos (clonamos) el Drawable de forma obligatoria en tiempo real para no corromper color a nivel sistema (System-wide background leak).
            val drawable = tvStatus.background?.mutate()
            drawable?.setTint(ContextCompat.getColor(context, bgColor))
            tvStatus.background = drawable

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

    /**
     * Propósito: Delegado automático `DiffUtil` de [ListAdapter] responsable de calcular disparidades (Deltas) entre listas asíncronas para proveer animaciones ricas, redefiniendo comparadores absolutos y superficiales.
     */
    class EventDiffCallback : DiffUtil.ItemCallback<EventEntity>() {
        override fun areItemsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean = oldItem == newItem
    }
}

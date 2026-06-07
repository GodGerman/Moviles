package com.example.proyecto.ui.consult

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.proyecto.R
import com.example.proyecto.data.model.EventEntity
import com.example.proyecto.viewmodel.EventViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.textfield.TextInputEditText
import java.util.*

/**
 * Propósito: Diálogo modal (DialogFragment) para Editar o Eliminar un evento existente.
 * Rol en MVVM: Capa de Presentación (UI Layer). Un DialogFragment se renderiza "flotando" por encima de la pantalla actual. Contiene la lógica para precargar el formulario con datos existentes y despachar la actualización o borrado a través de `EventViewModel`.
 * Interacciones: Se vincula estrechamente a [EventViewModel]. Invoca actividades externas como [com.example.proyecto.ui.map.MapsActivity] o el selector de contactos del sistema operativo. Cancela alarmas residuales en `WorkManager` al eliminar.
 */
class EditEventDialogFragment : DialogFragment(), OnMapReadyCallback {

    private val eventViewModel: EventViewModel by activityViewModels()

    // Datos temporales del evento a editar alojados en memoria
    private var eventId: Int = 0
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var lat = 0.0
    private var lng = 0.0
    private var selectedContact = ""

    // Vista de previsualización del mapa incrustada
    private var mapView: MapView? = null
    
    /**
     * Propósito: Launcher moderno (`registerForActivityResult`) encargado de solicitar la recolección de nuevas coordenadas espaciales.
     * Lógica interna: Espera el código `RESULT_OK` de la Actividad de Mapas. Sobrescribe la latitud y longitud, actualiza las etiquetas de UI de solo lectura y, de manera asíncrona, vuelve a renderizar el mapa incrustado `mapView?.getMapAsync(this)` para reflejar visualmente el salto geográfico de inmediato.
     */
    private val mapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            lng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            view?.findViewById<TextView>(R.id.et_edit_lat)?.text = lat.toString()
            view?.findViewById<TextView>(R.id.et_edit_lng)?.text = lng.toString()
            
            // Recargamos asíncronamente el pequeño mapa para que refleje la nueva ubicación
            mapView?.getMapAsync(this)
        }
    }

    /**
     * Propósito: Launcher para solicitar el acceso a la agenda nativa del dispositivo.
     * Lógica interna: Ejecuta un intent para buscar contactos. Al recibir `RESULT_OK`, abre un Cursor apuntando al URI retornado mediante el `ContentResolver` del sistema, extrayendo y almacenando el `DISPLAY_NAME` en la variable interna y refrescando la UI con el nuevo nombre.
     */
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data ?: return@registerForActivityResult
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            context?.contentResolver?.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    selectedContact = cursor.getString(0) ?: getString(R.string.default_contact)
                    view?.findViewById<TextView>(R.id.et_edit_contact)?.text = selectedContact
                }
            }
        }
    }

    /**
     * Companion object provee un método estático tipo Factory `newInstance` seguro para inicializar este DialogFragment inyectándole argumentos predefinidos por Bundle.
     */
    companion object {
        private const val ARG_EVENT_ID = "event_id"
        private const val ARG_CATEGORIA = "categoria"
        private const val ARG_FECHA = "fecha"
        private const val ARG_HORA = "hora"
        private const val ARG_DESCRIPCION = "descripcion"
        private const val ARG_ESTATUS = "estatus"
        private const val ARG_LAT = "lat"
        private const val ARG_LNG = "lng"
        private const val ARG_CONTACTO = "contacto"
        private const val ARG_RECORDATORIO = "recordatorio"

        fun newInstance(event: EventEntity): EditEventDialogFragment {
            val fragment = EditEventDialogFragment()
            val args = Bundle().apply {
                putInt(ARG_EVENT_ID, event.id)
                putString(ARG_CATEGORIA, event.categoria)
                putString(ARG_FECHA, event.fecha)
                putString(ARG_HORA, event.hora)
                putString(ARG_DESCRIPCION, event.descripcion)
                putString(ARG_ESTATUS, event.estatus)
                putDouble(ARG_LAT, event.ubicacion_lat)
                putDouble(ARG_LNG, event.ubicacion_lng)
                putString(ARG_CONTACTO, event.contacto_nombre)
                putInt(ARG_RECORDATORIO, event.recordatorio_tipo)
            }
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * Propósito: Modifica el comportamiento predeterminado de Android para redimensionar el diálogo en pantalla.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna: Forzamos a que la ventana de este cuadro de diálogo ocupe el 90% del ancho real de la pantalla en lugar de encapsularse estrictamente, usando los pixeles proporcionales para mejorar la legibilidad del mapa interno.
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Propósito: Materializa la interfaz interactiva, extrayendo e instalando la información suministrada previamente a través del Bundle para rellenar los componentes del formulario.
     * Parámetros:
     * - inflater: Mecanismo constructor XML.
     * - container: Contenedor subyacente de jerarquía.
     * - savedInstanceState: Estado preservable en rotaciones.
     * Retorno: Raíz de vista [View].
     * Lógica interna:
     * 1. Extrae cada propiedad inyectada.
     * 2. Localiza cada widget visual y le atribuye su valor correspondiente, incluyendo la búsqueda manual de índices en los Arreglos (`categories.indexOf`) para situar los Spinners en la selección original.
     * 3. Configura los listeners clásicos de Fecha y Hora precargando el calendario con los datos originales parseados para su edición contextual natural.
     * 4. Asigna los `mapLauncher` y `contactPickerLauncher` a sus botones interactivos.
     * 5. Fuerza la inyección asincrónica inicial del `MapView` interno pasando por `onCreate`.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dialog_edit_event, container, false)

        val args = requireArguments()
        eventId = args.getInt(ARG_EVENT_ID)
        selectedDate = args.getString(ARG_FECHA, "")
        selectedTime = args.getString(ARG_HORA, "")
        val categoria = args.getString(ARG_CATEGORIA, "")
        val descripcion = args.getString(ARG_DESCRIPCION, "")
        val estatus = args.getString(ARG_ESTATUS, "")
        lat = args.getDouble(ARG_LAT, 0.0)
        lng = args.getDouble(ARG_LNG, 0.0)
        selectedContact = args.getString(ARG_CONTACTO, "")
        val recordatorio = args.getInt(ARG_RECORDATORIO, 0)

        val spinnerCategory: Spinner = root.findViewById(R.id.spinner_edit_category)
        val spinnerStatus: Spinner = root.findViewById(R.id.spinner_edit_status)
        val btnDate: Button = root.findViewById(R.id.btn_edit_date)
        val btnTime: Button = root.findViewById(R.id.btn_edit_time)
        val etDescription: TextInputEditText = root.findViewById(R.id.et_edit_description)
        val tvContact: TextView = root.findViewById(R.id.et_edit_contact)
        val tvLat: TextView = root.findViewById(R.id.et_edit_lat)
        val tvLng: TextView = root.findViewById(R.id.et_edit_lng)
        val spinnerReminder: Spinner = root.findViewById(R.id.spinner_edit_reminder)
        
        val btnActionLocation: Button = root.findViewById(R.id.btn_action_edit_location)
        val btnActionContact: Button = root.findViewById(R.id.btn_action_edit_contact)
        mapView = root.findViewById(R.id.map_preview_edit)

        val categories = resources.getStringArray(R.array.categories_array)
        val categoryIndex = categories.indexOf(categoria)
        if (categoryIndex >= 0) spinnerCategory.setSelection(categoryIndex)

        val statuses = resources.getStringArray(R.array.status_array)
        val statusIndex = statuses.indexOf(estatus)
        if (statusIndex >= 0) spinnerStatus.setSelection(statusIndex)

        try {
            val parts = selectedDate.split("-")
            if(parts.size == 3) {
                btnDate.text = String.format("%02d/%02d/%04d", parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            } else {
                btnDate.text = selectedDate
            }
        } catch(e: Exception) {
            btnDate.text = selectedDate
        }
        btnTime.text = selectedTime
        etDescription.setText(descripcion)
        tvContact.text = selectedContact
        tvLat.text = lat.toString()
        tvLng.text = lng.toString()
        spinnerReminder.setSelection(recordatorio)

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            try {
                val parts = selectedDate.split("-")
                calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (_: Exception) { }

            DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                val uiDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                btnDate.text = uiDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            try {
                val parts = selectedTime.split(":")
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                calendar.set(Calendar.MINUTE, parts[1].toInt())
            } catch (_: Exception) { }

            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                btnTime.text = selectedTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        btnActionLocation.setOnClickListener {
            val intent = Intent(requireContext(), com.example.proyecto.ui.map.MapsActivity::class.java)
            mapLauncher.launch(intent)
        }

        btnActionContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        }

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        return root
    }

    /**
     * Propósito: Aquí, después de que la Vista base se creó, inyectamos botones de acción (Cancelar, Borrar, Guardar) dinámicamente usando código Kotlin en lugar de XML.
     * Parámetros:
     * - view: Vista pre-construida base.
     * - savedInstanceState: Estado.
     * Retorno: Ninguno.
     * Lógica interna: Manipula programáticamente un `LinearLayout` incrustando botones generados al vuelo para demostrar flexibilidad de diseño sin depender meramente de XML, asignándoles acciones destructivas (`deleteEvent`) y constructivas (`saveEvent`).
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollView = view as? android.widget.ScrollView ?: return
        val container = scrollView.getChildAt(0) as? android.widget.LinearLayout ?: return

        val buttonLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
            }
        }

        val btnCancel = com.google.android.material.button.MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.btn_cancel)
            insetTop = 0
            insetBottom = 0
            textSize = 12f
            maxLines = 1
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = 4
            }
            // 'dismiss()' destruye y cierra este DialogFragment de manera segura
            setOnClickListener { dismiss() }
        }

        val btnDelete = com.google.android.material.button.MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = getString(R.string.btn_delete)
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.md_error))
            setStrokeColorResource(R.color.md_error)
            insetTop = 0
            insetBottom = 0
            textSize = 12f
            maxLines = 1
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = 4
                marginStart = 4
            }
            setOnClickListener { deleteEvent() }
        }

        val btnSave = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = getString(R.string.btn_save_changes)
            insetTop = 0
            insetBottom = 0
            textSize = 12f
            maxLines = 1
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = 4
            }
            setOnClickListener { saveEvent(view) }
        }

        buttonLayout.addView(btnCancel)
        buttonLayout.addView(btnDelete)
        buttonLayout.addView(btnSave)
        container.addView(buttonLayout)
    }
    
    /**
     * Propósito: Reacción automática al llamado asíncrono de finalización de mapa embebido.
     * Parámetros:
     * - googleMap: Componente mapa a gobernar.
     * Retorno: Ninguno.
     * Lógica interna: Elimina íconos de toolbar basura (`isMapToolbarEnabled = false`), limpia la instancia, dibuja un marcador y focaliza el centro con un zoom predefinido.
     */
    override fun onMapReady(googleMap: com.google.android.gms.maps.GoogleMap) {
        googleMap.uiSettings.isMapToolbarEnabled = false
        val position = LatLng(lat, lng)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(position))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
    }

    // --- Ciclo de Vida del MapView Incrustado ---
    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    /**
     * Propósito: Toma los nuevos valores re-llenados del formulario y sobreescribe un registro existente usando UPDATE.
     * Parámetros:
     * - root: Vista inicial referenciada para recolectar información actual de los campos interactivos.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Extrae Spinners y EditTexts para ensamblar una clase [EventEntity] clónica.
     * 2. CRÍTICO: Debe asignar exactamente el mismo ID interno (`id = eventId`) a la entidad de paso; de lo contrario, Room producirá un INSERT en lugar de un UPDATE, causando elementos duplicados en la lista del usuario.
     * 3. Ejecuta `eventViewModel.update`, reporta éxito visual al usuario y procede a `dismiss()` para desaparecer.
     */
    private fun saveEvent(root: View) {
        val spinnerCategory: Spinner = root.findViewById(R.id.spinner_edit_category)
        val spinnerStatus: Spinner = root.findViewById(R.id.spinner_edit_status)
        val etDescription: TextInputEditText = root.findViewById(R.id.et_edit_description)
        val spinnerReminder: Spinner = root.findViewById(R.id.spinner_edit_reminder)

        val description = etDescription.text.toString()
        if (description.isBlank() || selectedDate.isBlank() || selectedTime.isBlank()) {
            Toast.makeText(context, R.string.toast_fill_required_fields, Toast.LENGTH_SHORT).show()
            return
        }

        val updatedEvent = EventEntity(
            id = eventId,
            categoria = spinnerCategory.selectedItem.toString(),
            fecha = selectedDate,
            hora = selectedTime,
            descripcion = description,
            estatus = spinnerStatus.selectedItem.toString(),
            ubicacion_lat = lat,
            ubicacion_lng = lng,
            contacto_nombre = selectedContact.ifBlank { getString(R.string.default_contact) },
            recordatorio_tipo = spinnerReminder.selectedItemPosition
        )

        eventViewModel.update(updatedEvent)
        Toast.makeText(context, R.string.toast_event_updated, Toast.LENGTH_SHORT).show()
        dismiss()
    }

    /**
     * Propósito: Muestra un diálogo de confirmación estricta antes de eliminar permanentemente el registro de manera irrevocable.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Crea un `EventEntity` cascarón con datos basura excepto el ID, que es el único rastreador que necesita Room para hacer su trabajo.
     * 2. Despliega un componente nativo de Android `AlertDialog.Builder`.
     * 3. CRÍTICO: Si el usuario aprueba, no sólo borra del SQLite local, sino que cancela asertivamente (`cancelUniqueWork`) cualquier tarea inyectada remanente sobre este ID específico pendiente dentro del sistema de `WorkManager`, evitando así que alarmas fantasmas despabilen al usuario de un evento inexistente en su historial. Luego ejecuta `dismiss()`.
     */
    private fun deleteEvent() {
        val eventToDelete = EventEntity(
            id = eventId,
            categoria = "",
            fecha = "",
            hora = "",
            descripcion = "",
            estatus = "",
            ubicacion_lat = 0.0,
            ubicacion_lng = 0.0,
            contacto_nombre = "",
            recordatorio_tipo = 0
        )
        // Dialog de confirmación de Android nativo de protección (AlertDialog)
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_delete_event)
            .setMessage(R.string.msg_confirm_delete)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                eventViewModel.delete(eventToDelete)
                
                // CRÍTICO: Prevenimos notificaciones 'fantasma' si el usuario elimina
                // una tarea que aún tenía pendiente emitir su aviso planificado en segundo plano.
                androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("event_${eventId}")
                
                Toast.makeText(context, R.string.toast_event_deleted, Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}

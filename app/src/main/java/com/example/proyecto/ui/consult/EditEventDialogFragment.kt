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

class EditEventDialogFragment : DialogFragment(), OnMapReadyCallback {

    private val eventViewModel: EventViewModel by activityViewModels()

    private var eventId: Int = 0
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    
    private var lat = 0.0
    private var lng = 0.0
    private var selectedContact = ""

    private var mapView: MapView? = null
    
    // Launchers
    private val mapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            lng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            view?.findViewById<TextView>(R.id.et_edit_lat)?.text = lat.toString()
            view?.findViewById<TextView>(R.id.et_edit_lng)?.text = lng.toString()
            
            // Actualizar el mapa
            mapView?.getMapAsync(this)
        }
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data ?: return@registerForActivityResult
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            context?.contentResolver?.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    selectedContact = cursor.getString(0)
                    view?.findViewById<TextView>(R.id.et_edit_contact)?.text = selectedContact
                }
            }
        }
    }

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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

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

        btnDate.text = selectedDate
        btnTime.text = selectedTime
        etDescription.setText(descripcion)
        tvContact.text = selectedContact
        tvLat.text = lat.toString()
        tvLng.text = lng.toString()
        spinnerReminder.setSelection(recordatorio)

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            try {
                val parts = selectedDate.split("/")
                calendar.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            } catch (_: Exception) { }

            DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDate = "$day/${month + 1}/$year"
                btnDate.text = selectedDate
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
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = 8
            }
            setOnClickListener { dismiss() }
        }

        val btnSave = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = getString(R.string.btn_save_changes)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = 8
            }
            setOnClickListener { saveEvent(view) }
        }

        buttonLayout.addView(btnCancel)
        buttonLayout.addView(btnSave)
        container.addView(buttonLayout)
    }
    
    override fun onMapReady(googleMap: com.google.android.gms.maps.GoogleMap) {
        googleMap.uiSettings.isMapToolbarEnabled = false
        val position = LatLng(lat, lng)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(position))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
    }

    // --- MapView Lifecycle ---
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
}

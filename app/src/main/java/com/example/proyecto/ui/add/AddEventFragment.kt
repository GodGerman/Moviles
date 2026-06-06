package com.example.proyecto.ui.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.proyecto.R
import com.example.proyecto.data.model.EventEntity
import com.example.proyecto.notification.NotificationWorker
import com.example.proyecto.viewmodel.EventViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AddEventFragment : Fragment() {

    private val eventViewModel: EventViewModel by activityViewModels()
    
    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedContact = ""
    private var lat = 0.0
    private var lng = 0.0

    private val mapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            lng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            view?.findViewById<TextView>(R.id.tv_selected_location)?.text =
                getString(R.string.item_location_format, lat, lng)
        }
    }

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val contactUri = result.data?.data ?: return@registerForActivityResult
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            context?.contentResolver?.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    selectedContact = cursor.getString(0)
                    view?.findViewById<TextView>(R.id.tv_selected_contact)?.text = selectedContact
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_add_event, container, false)

        val btnDate: Button = root.findViewById(R.id.btn_date)
        val btnTime: Button = root.findViewById(R.id.btn_time)
        val btnContact: Button = root.findViewById(R.id.btn_contact)
        val btnSave: Button = root.findViewById(R.id.btn_save)
        
        val spinnerCategory: Spinner = root.findViewById(R.id.spinner_category)
        val spinnerStatus: Spinner = root.findViewById(R.id.spinner_status)
        val spinnerReminder: Spinner = root.findViewById(R.id.spinner_reminder)
        val etDescription: EditText = root.findViewById(R.id.et_description)

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDate = "$day/${month + 1}/$year"
                btnDate.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                btnTime.text = selectedTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        btnContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        }

        root.findViewById<Button>(R.id.btn_location).setOnClickListener {
            val intent = Intent(requireContext(), com.example.proyecto.ui.map.MapsActivity::class.java)
            mapLauncher.launch(intent)
        }

        btnSave.setOnClickListener {
            val description = etDescription.text.toString()
            if (description.isBlank() || selectedDate.isBlank() || selectedTime.isBlank()) {
                Toast.makeText(context, R.string.toast_fill_required_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val event = EventEntity(
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

            eventViewModel.insert(event)
            scheduleNotification(event)
            Toast.makeText(context, R.string.toast_event_saved, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        return root
    }

    private fun scheduleNotification(event: EventEntity) {
        if (event.recordatorio_tipo == 0) return // Sin recordatorio

        val eventDateTimeStr = "${event.fecha} ${event.hora}"
        val sdf = SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault())
        val eventDate = sdf.parse(eventDateTimeStr) ?: return
        val eventTimeMillis = eventDate.time
        val currentTimeMillis = System.currentTimeMillis()

        var delay = eventTimeMillis - currentTimeMillis

        // Ajustar delay según tipo de recordatorio
        when (event.recordatorio_tipo) {
            2 -> delay -= 10 * 60 * 1000 // 10 min antes
            3 -> delay -= 24 * 60 * 60 * 1000 // 1 día antes
        }

        if (delay > 0) {
            val data = Data.Builder()
                .putString("title", getString(R.string.notification_reminder_prefix, event.categoria))
                .putString("description", event.descripcion)
                .build()

            val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(requireContext()).enqueue(notificationWork)
        }
    }
}

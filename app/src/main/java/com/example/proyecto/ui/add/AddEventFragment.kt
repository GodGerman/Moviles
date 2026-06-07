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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.work.ExistingWorkPolicy

/**
 * Propósito: Fragmento (Pantalla) responsable de la creación de nuevos eventos/tareas. Contiene el formulario principal interactivo.
 * Rol en MVVM: Capa de Presentación (UI Layer). Captura la entrada del usuario y delega la responsabilidad de guardado al ViewModel. No accede a la base de datos directamente.
 * Interacciones: Se enlaza fuertemente con [EventViewModel] para despachar la inserción. Interactúa con [com.example.proyecto.ui.map.MapsActivity] mediante un intent para adquirir coordenadas geográficas. Programa tareas en segundo plano a través de [NotificationWorker].
 */
class AddEventFragment : Fragment() {

    // Al usar 'activityViewModels()', nos aseguramos de usar la MISMA instancia del ViewModel
    // que la MainActivity y los demás fragmentos. Esto permite que si este fragmento inserta un dato, 
    // la lista principal lo vea inmediatamente porque comparten el mismo origen de datos subyacente.
    private val eventViewModel: EventViewModel by activityViewModels()
    
    // Variables de estado temporal para ir guardando las selecciones del usuario en el formulario
    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedContact = ""
    private var lat = 0.0
    private var lng = 0.0

    /**
     * Propósito: `registerForActivityResult` es la API moderna de Android para lanzar actividades secundarias y escuchar su respuesta, reemplazando a `startActivityForResult`.
     * Lógica interna: Este launcher específico abre nuestro 'MapsActivity' e intercepta un `RESULT_OK`. Luego lee los extras del intent devuelto ("lat" y "lng") y los almacena en el estado temporal de la clase, actualizando también la etiqueta de la interfaz de usuario.
     */
    private val mapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Extraer las coordenadas del Intent de respuesta, o colocar 0.0 si falla por alguna razón
            lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            lng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            
            // Actualiza la TextView en la pantalla para informar visualmente al usuario del éxito de su selección
            view?.findViewById<TextView>(R.id.tv_selected_location)?.text =
                getString(R.string.item_location_format, lat, lng)
        }
    }

    /**
     * Propósito: Launcher que solicita al sistema operativo abrir la aplicación de Contactos nativa del teléfono para que el usuario seleccione una persona, y posteriormente lee su nombre.
     * Lógica interna: Recibe el URI del contacto seleccionado. Crea una "proyección" (columnas de DB que queremos consultar). Utiliza un `ContentResolver` para realizar la lectura de base de datos inter-aplicaciones y recupera el nombre a mostrar (DISPLAY_NAME).
     */
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Obtenemos el localizador (URI) del contacto seleccionado desde la agenda nativa
            val contactUri = result.data?.data ?: return@registerForActivityResult
            
            // "projection" indica a Android qué columnas de su base de datos de contactos queremos extraer
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            
            // contentResolver.query hace una consulta segura a los datos del teléfono
            context?.contentResolver?.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // Si encontramos un resultado, guardamos el nombre. Si está vacío, guardamos un default.
                    selectedContact = cursor.getString(0) ?: getString(R.string.default_contact)
                    view?.findViewById<TextView>(R.id.tv_selected_contact)?.text = selectedContact
                }
            }
        }
    }

    /**
     * Propósito: Método del ciclo de vida donde se "infla" (convierte de XML a objetos Kotlin) la interfaz gráfica del fragmento y se asocian sus listeners.
     * Parámetros:
     * - inflater: Utilidad para inflar layouts.
     * - container: Contenedor padre.
     * - savedInstanceState: Estado de recreación de la vista.
     * Retorno: La raíz del [View] inflado.
     * Lógica interna: 
     * 1. Infla `fragment_add_event.xml`.
     * 2. Enlaza los botones y spinners de la UI a variables locales.
     * 3. Configura `DatePickerDialog` y `TimePickerDialog` nativos para la selección amigable de fechas y horas.
     * 4. Asigna los `launchers` previamente declarados a sus botones respectivos.
     * 5. Al presionar "Guardar", empaqueta las selecciones en un [EventEntity] y delega la labor al ViewModel mediante una corrutina enlazada al ciclo de vida del fragment (`viewLifecycleOwner.lifecycleScope.launch`). Tras el éxito, programa notificaciones y navega hacia la pantalla anterior.
     */
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

        // Diálogo estandarizado para seleccionar Fecha
        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                // Formateamos en ISO (YYYY-MM-DD) la variable de estado interno para que la DB pueda ordenar correctamente
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                // Formato amigable para exhibirlo en el botón de la Interfaz (DD/MM/YYYY)
                val uiDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                btnDate.text = uiDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Diálogo estandarizado para seleccionar Hora
        btnTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                btnTime.text = selectedTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show() // true = modo 24 horas
        }

        // Al hacer clic, lanza nuestro 'contactPickerLauncher' declarando un intent implícito a la libreta de direcciones
        btnContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        }

        // Al hacer clic, lanza de manera explícita nuestra propia Activity (MapsActivity)
        root.findViewById<Button>(R.id.btn_location).setOnClickListener {
            val intent = Intent(requireContext(), com.example.proyecto.ui.map.MapsActivity::class.java)
            mapLauncher.launch(intent)
        }

        // Lógica de validación principal y flujo de guardado
        btnSave.setOnClickListener {
            val description = etDescription.text.toString()
            
            // Evitar inserciones corruptas exigiendo los campos vitales
            if (description.isBlank() || selectedDate.isBlank() || selectedTime.isBlank()) {
                Toast.makeText(context, R.string.toast_fill_required_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mapeo (Binding manual) de los datos de la UI al modelo de datos de la DB
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

            // 'lifecycleScope.launch' encapsula la llamada asíncrona (suspendida) al ViewModel 
            // asegurándose que si el usuario cierra el fragmento antes de terminar, la corrutina se aborte limpiamente.
            viewLifecycleOwner.lifecycleScope.launch {
                val newId = eventViewModel.insertAndReturnId(event)
                
                // Programamos la alarma/recordatorio en segundo plano con el ID definitivo en la base de datos.
                scheduleNotification(event.copy(id = newId.toInt()))
                
                // DEMO: Lanzar una notificación inmediata para probar que el sistema funciona
                val demoData = Data.Builder()
                    .putString("title", "¡Nuevo recordatorio creado!")
                    .putString("description", "Se ha guardado: ${event.descripcion}")
                    .build()
                val demoWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInputData(demoData)
                    .build()
                WorkManager.getInstance(requireContext()).enqueue(demoWork)
                
                Toast.makeText(context, R.string.toast_event_saved, Toast.LENGTH_SHORT).show()
                
                // Navegamos hacia atrás simulando el botón de retroceso (Back), sacando el fragment actual de la pila.
                findNavController().navigateUp()
            }
        }

        return root
    }

    /**
     * Propósito: Configura y programa una tarea asíncrona usando 'AlarmManager' para que despierte al dispositivo y lance una notificación local en un futuro exacto.
     * Parámetros:
     * - event: Objeto [EventEntity] correspondiente al evento recién guardado. Requerimos que ya posea un ID real.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Detiene la ejecución si el usuario configuró "Sin recordatorio" (recordatorio_tipo == 0).
     * 2. Parsea la fecha y hora seleccionadas fusionándolas.
     * 3. Ajusta el timestamp de activación descontando minutos u horas en función a la opción seleccionada.
     * 4. Utiliza `AlarmManager.setExactAndAllowWhileIdle` que fuerza al SO a despertar del modo Doze en el milisegundo exacto deseado.
     * 5. Delega la responsabilidad de mostrar la interfaz al BroadcastReceiver `NotificationReceiver`.
     */
    private fun scheduleNotification(event: EventEntity) {
        if (event.recordatorio_tipo == 0) return // Si eligió "Sin recordatorio", salimos tempranamente.

        // Concatenamos las cadenas elegidas y las transformamos a una clase manejable temporal
        val eventDateTimeStr = "${event.fecha} ${event.hora}"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val eventDate = sdf.parse(eventDateTimeStr) ?: return
        
        var triggerAtMillis = eventDate.time

        // Retrasamos el "reloj despertador" dependiendo del setting seleccionado
        when (event.recordatorio_tipo) {
            2 -> triggerAtMillis -= 10 * 60 * 1000 // Adelantarlo por 10 minutos
            3 -> triggerAtMillis -= 24 * 60 * 60 * 1000 // Adelantarlo por un día entero
        }

        // Validamos lógicamente que la alarma no esté configurada para sonar en el pasado
        if (triggerAtMillis > System.currentTimeMillis()) {
            val alarmManager = requireContext().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(requireContext(), com.example.proyecto.notification.NotificationReceiver::class.java).apply {
                putExtra("title", getString(R.string.notification_reminder_prefix, event.categoria))
                putExtra("description", event.descripcion)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND) // Le pide a Android que corra este Broadcast con prioridad alta
            }
            
            // PendingIntent encapsula el Broadcast que ejecutará nuestra alarma en el futuro.
            // FLAG_UPDATE_CURRENT actualiza una alarma existente del mismo ID.
            // FLAG_IMMUTABLE es un requerimiento estricto de seguridad desde Android 12+.
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                requireContext(),
                event.id, // El ID único del evento en la BD sirve para poder cancelar/actualizar la alarma individualmente
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // En dispositivos chinos (Xiaomi, Huawei) el sistema mata los PendingIntents si la app es cerrada, 
            // a menos que usemos setAlarmClock, el cual tiene la prioridad máxima de todo el sistema operativo.
            val showIntent = Intent(requireContext(), com.example.proyecto.MainActivity::class.java)
            val showPendingIntent = android.app.PendingIntent.getActivity(
                requireContext(),
                event.id,
                showIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            try {
                val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(
                    triggerAtMillis,
                    showPendingIntent
                )
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } catch (e: SecurityException) {
                // A partir de Android 14, los usuarios pueden revocar manualmente el permiso de Alarmas Exactas
                e.printStackTrace()
            }
        }
    }
}

package com.example.proyecto.ui.consult

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.ui.home.EventAdapter
import com.example.proyecto.viewmodel.EventViewModel
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*
import java.text.SimpleDateFormat

/**
 * Propósito: Fragmento encargado de Consultar y Filtrar (query) los eventos de la base de datos de manera dinámica y avanzada.
 * Rol en MVVM: Capa de Presentación (UI Layer). Funciona como un visor reactivo en el que el usuario configura restricciones y la UI pasa las variables al [EventViewModel].
 * Interacciones: Delega las llamadas de filtrado interactuando a través del `EventViewModel`. Lanza, como sub-vistas, al [EventDetailsBottomSheet] (hoja modal para consultar datos a fondo) y al [EditEventDialogFragment] (para hacer modificaciones puntuales). Renderiza resultados con [EventAdapter].
 */
class ConsultEventsFragment : Fragment() {

    private val eventViewModel: EventViewModel by activityViewModels()
    private lateinit var adapter: EventAdapter
    
    // Variables de estado que sostienen los filtros actuales en memoria antes de mandarlos al ViewModel
    private var filterCategory: String? = null
    private var filterStartDate: String? = null
    private var filterEndDate: String? = null
    
    // Mantiene rastreo de qué "botón segmentado" se presionó (Día, Mes, Año, Rango)
    private var currentMode: Int = R.id.btn_mode_day

    /**
     * Propósito: Construir la jerarquía visual de componentes de consulta (filtros, recyclerview, botones modales).
     * Parámetros:
     * - inflater: Objeto para expandir layout XML.
     * - container: ViewGroup contenedor subyacente.
     * - savedInstanceState: Caché de variables guardadas.
     * Retorno: Objeto [View] listo.
     * Lógica interna:
     * 1. Configura el `RecyclerView` usando un `LinearLayoutManager` y nuestro `EventAdapter` personalizado, pasando dos lambdas (clics y edición) que abrirán diálogos.
     * 2. Configura el Spinner rellenando el catálogo de categorías que se extrae del XML e inyectando un "Todas las categorías" extra.
     * 3. Configura el `MaterialButtonToggleGroup`. Al cambiar de tipo de búsqueda, borra las fechas previas seleccionadas.
     * 4. Al hacer click en "Seleccionar Fecha", calcula fechas internamente (Día=exacto, Mes=1 al 30/31, Año=1 de Ene al 31 de Dic) e inicia selectores MD3 dependiendo del modo.
     * 5. Al presionar "Borrar Filtros" restablece los nulls y pide actualizar la vista completa.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_consult_events, container, false)

        val recyclerView: RecyclerView = root.findViewById(R.id.rv_consult_results)

        // Crear adaptador inyectándole comportamiento en forma de Funciones Anónimas (Lambdas)
        adapter = EventAdapter(
            onItemClick = { event ->
                // Abre una "BottomSheet" (Modal inferior deslizable) para ver detalles del evento
                val bottomSheet = EventDetailsBottomSheet.newInstance(event)
                bottomSheet.show(childFragmentManager, "EventDetailsBottomSheet")
            },
            onEditClick = { event ->
                // Abre el diálogo superpuesto para Editar
                val dialog = EditEventDialogFragment.newInstance(event)
                dialog.show(childFragmentManager, "EditEventDialog")
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val spinnerCategory: Spinner = root.findViewById(R.id.spinner_filter_category)
        val btnDate: Button = root.findViewById(R.id.btn_filter_date)
        val btnClear: Button = root.findViewById(R.id.btn_clear_filters)

        // Dinámicamente tomamos el arreglo del XML y le agregamos la opción comodín "Todas las categorías" al inicio (índice 0)
        val categories = resources.getStringArray(R.array.categories_array).toMutableList()
        categories.add(0, getString(R.string.all_categories))
        
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        // Se usa setOnItemSelectedListener para disparar búsquedas automáticas cada que cambia el spinner
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterCategory = categories[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Grupo de botones tipo "Segmented Controls" de Material Design
        val toggleMode: MaterialButtonToggleGroup = root.findViewById(R.id.toggle_date_filter_mode)
        toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentMode = checkedId
                // Limpiamos fechas anteriores porque el formato lógico/matemático cambia según el modo seleccionado
                filterStartDate = null
                filterEndDate = null
                btnDate.text = getString(R.string.btn_select_date)
                applyFilters()
            }
        }

        // Lógica súper interactiva: dependiendo del 'Modo' de búsqueda activo,
        // este botón abrirá un selector distinto y formateará la respuesta diferentemente.
        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            when (currentMode) {
                // El Modo Rango usa el "MaterialDatePicker" avanzado de Google
                // que renderiza un calendario enorme donde se hace tap a dos fechas distintas (swipe to select range)
                R.id.btn_mode_range -> {
                    val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(getString(R.string.select_date_range))
                        .build()
                        
                    dateRangePicker.addOnPositiveButtonClickListener { selection ->
                        // La selección devuelve un par de "milisegundos" desde epoch, los convertimos a Calendar.
                        val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection.first }
                        val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection.second }
                        
                        // Guardando formato ordenable ISO SQL (YYYY-MM-DD) para las variables temporales
                        filterStartDate = String.format("%04d-%02d-%02d", startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH) + 1, startCal.get(Calendar.DAY_OF_MONTH))
                        filterEndDate = String.format("%04d-%02d-%02d", endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH) + 1, endCal.get(Calendar.DAY_OF_MONTH))
                        
                        // Formato cosmético para reescribir el botón UI (DD/MM/YYYY - DD/MM/YYYY)
                        btnDate.text = "${String.format("%02d/%02d/%04d", startCal.get(Calendar.DAY_OF_MONTH), startCal.get(Calendar.MONTH) + 1, startCal.get(Calendar.YEAR))} - ${String.format("%02d/%02d/%04d", endCal.get(Calendar.DAY_OF_MONTH), endCal.get(Calendar.MONTH) + 1, endCal.get(Calendar.YEAR))}"
                        
                        applyFilters()
                    }
                    dateRangePicker.show(childFragmentManager, "DateRangePicker")
                }
                
                // Todos los demás modos (Día/Mes/Año) usan un DatePickerDialog simple,
                // usando matemáticas puras de Calendar para deducir el resto.
                else -> {
                    DatePickerDialog(requireContext(), { _, year, month, day ->
                        val selectedDateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                        val uiDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                        
                        when (currentMode) {
                            R.id.btn_mode_day -> {
                                // Si es un día, ambos límites son iguales.
                                filterStartDate = selectedDateStr
                                filterEndDate = selectedDateStr
                                btnDate.text = uiDate
                            }
                            R.id.btn_mode_month -> {
                                // Para abarcar el mes entero, calculamos desde el Día 1 hasta el método estricto 'getActualMaximum' de ese mes/año bisiesto particular (ej. 28, 30 o 31).
                                val startOfMonth = String.format("%04d-%02d-01", year, month + 1)
                                val maxDay = Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
                                val endOfMonth = String.format("%04d-%02d-%02d", year, month + 1, maxDay)
                                
                                filterStartDate = startOfMonth
                                filterEndDate = endOfMonth
                                
                                // Extrae el nombre del mes de sistema local para mostrarlo en el botón.
                                val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().apply { set(year, month, 1) }.time)
                                btnDate.text = monthName.replaceFirstChar { it.uppercase() }
                            }
                            R.id.btn_mode_year -> {
                                // Hardcodeamos del 1 de Enero al 31 de Diciembre directamente en el string.
                                filterStartDate = String.format("%04d-01-01", year)
                                filterEndDate = String.format("%04d-12-31", year)
                                btnDate.text = year.toString()
                            }
                        }
                        applyFilters()
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                }
            }
        }

        // Restablece valores
        btnClear.setOnClickListener {
            filterCategory = getString(R.string.all_categories)
            filterStartDate = null
            filterEndDate = null
            spinnerCategory.setSelection(0) // Regresa al índice 0
            btnDate.text = getString(R.string.btn_select_date)
            applyFilters()
        }

        applyFilters() // Llamada inicial en la que todo está en null, provocando que se rendericen TODOS los eventos existentes.

        return root
    }

    /**
     * Propósito: Enviar el paquete de filtros actuales y observar el flujo de respuestas del repositorio.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna: Ejecuta un método de búsqueda avanzada del ViewModel. El `observe` escuchará asincrónicamente hasta que Room termine de ejecutar su SELECT; una vez listos los [events], la función se los empaqueta a la clase ListAdapter `adapter.submitList`, la cual renderiza y anima las diferencias (DiffUtil).
     */
    private fun applyFilters() {
        eventViewModel.getFilteredEventsAdvanced(filterCategory, filterStartDate, filterEndDate).observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
        }
    }
}

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

class ConsultEventsFragment : Fragment() {

    private val eventViewModel: EventViewModel by activityViewModels()
    private lateinit var adapter: EventAdapter
    
    private var filterCategory: String? = null
    private var filterStartDate: String? = null
    private var filterEndDate: String? = null
    private var currentMode: Int = R.id.btn_mode_day

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_consult_events, container, false)

        val recyclerView: RecyclerView = root.findViewById(R.id.rv_consult_results)

        // Crear adapter con callbacks de clics
        adapter = EventAdapter(
            onItemClick = { event ->
                val bottomSheet = EventDetailsBottomSheet.newInstance(event)
                bottomSheet.show(childFragmentManager, "EventDetailsBottomSheet")
            },
            onEditClick = { event ->
                val dialog = EditEventDialogFragment.newInstance(event)
                dialog.show(childFragmentManager, "EditEventDialog")
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val spinnerCategory: Spinner = root.findViewById(R.id.spinner_filter_category)
        val btnDate: Button = root.findViewById(R.id.btn_filter_date)
        val btnClear: Button = root.findViewById(R.id.btn_clear_filters)

        // Setup Spinner with "All" option
        val categories = resources.getStringArray(R.array.categories_array).toMutableList()
        categories.add(0, getString(R.string.all_categories))
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = spinnerAdapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterCategory = categories[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val toggleMode: MaterialButtonToggleGroup = root.findViewById(R.id.toggle_date_filter_mode)
        toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentMode = checkedId
                filterStartDate = null
                filterEndDate = null
                btnDate.text = getString(R.string.btn_select_date)
                applyFilters()
            }
        }

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            when (currentMode) {
                R.id.btn_mode_range -> {
                    val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(getString(R.string.select_date_range))
                        .build()
                    dateRangePicker.addOnPositiveButtonClickListener { selection ->
                        val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection.first }
                        val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection.second }
                        filterStartDate = String.format("%04d-%02d-%02d", startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH) + 1, startCal.get(Calendar.DAY_OF_MONTH))
                        filterEndDate = String.format("%04d-%02d-%02d", endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH) + 1, endCal.get(Calendar.DAY_OF_MONTH))
                        btnDate.text = "${String.format("%02d/%02d/%04d", startCal.get(Calendar.DAY_OF_MONTH), startCal.get(Calendar.MONTH) + 1, startCal.get(Calendar.YEAR))} - ${String.format("%02d/%02d/%04d", endCal.get(Calendar.DAY_OF_MONTH), endCal.get(Calendar.MONTH) + 1, endCal.get(Calendar.YEAR))}"
                        applyFilters()
                    }
                    dateRangePicker.show(childFragmentManager, "DateRangePicker")
                }
                else -> {
                    DatePickerDialog(requireContext(), { _, year, month, day ->
                        val selectedDateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
                        val uiDate = String.format("%02d/%02d/%04d", day, month + 1, year)
                        when (currentMode) {
                            R.id.btn_mode_day -> {
                                filterStartDate = selectedDateStr
                                filterEndDate = selectedDateStr
                                btnDate.text = uiDate
                            }
                            R.id.btn_mode_month -> {
                                val startOfMonth = String.format("%04d-%02d-01", year, month + 1)
                                val maxDay = Calendar.getInstance().apply { set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
                                val endOfMonth = String.format("%04d-%02d-%02d", year, month + 1, maxDay)
                                filterStartDate = startOfMonth
                                filterEndDate = endOfMonth
                                val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().apply { set(year, month, 1) }.time)
                                btnDate.text = monthName.replaceFirstChar { it.uppercase() }
                            }
                            R.id.btn_mode_year -> {
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

        btnClear.setOnClickListener {
            filterCategory = getString(R.string.all_categories)
            filterStartDate = null
            filterEndDate = null
            spinnerCategory.setSelection(0)
            btnDate.text = getString(R.string.btn_select_date)
            applyFilters()
        }

        applyFilters()

        return root
    }

    private fun applyFilters() {
        eventViewModel.getFilteredEventsAdvanced(filterCategory, filterStartDate, filterEndDate).observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
        }
    }
}

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
import java.util.*

class ConsultEventsFragment : Fragment() {

    private val eventViewModel: EventViewModel by activityViewModels()
    private lateinit var adapter: EventAdapter
    
    private var filterCategory: String? = null
    private var filterDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_consult_events, container, false)

        val recyclerView: RecyclerView = root.findViewById(R.id.rv_consult_results)
        adapter = EventAdapter()
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

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                filterDate = "$day/${month + 1}/$year"
                btnDate.text = filterDate
                applyFilters()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnClear.setOnClickListener {
            filterCategory = getString(R.string.all_categories)
            filterDate = null
            spinnerCategory.setSelection(0)
            btnDate.text = getString(R.string.btn_select_date)
            applyFilters()
        }

        applyFilters()

        return root
    }

    private fun applyFilters() {
        eventViewModel.getFilteredEvents(filterCategory, filterDate).observe(viewLifecycleOwner) { events ->
            adapter.submitList(events)
        }
    }
}

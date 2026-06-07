package com.example.proyecto.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.R
import com.example.proyecto.viewmodel.EventViewModel

class HomeFragment : Fragment() {

    private val eventViewModel: EventViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        val recyclerView: RecyclerView = root.findViewById(R.id.rv_events)
        val adapter = EventAdapter(
            onItemClick = { event ->
                val bottomSheet = com.example.proyecto.ui.consult.EventDetailsBottomSheet.newInstance(event)
                bottomSheet.show(childFragmentManager, "EventDetailsBottomSheet")
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        eventViewModel.getUpcomingEvents().observe(viewLifecycleOwner) { events ->
            events?.let { adapter.submitList(it) }
        }

        return root
    }
}

package com.example.proyecto.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proyecto.R

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflado de vista tradicional, infalible y sin dependencias de ViewBinding
        return inflater.inflate(R.layout.fragment_about, container, false)
    }
}

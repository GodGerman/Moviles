package com.example.proyecto.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = TextView(context).apply {
            text = "Fragmento Acerca de"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
        }
        return view
    }
}

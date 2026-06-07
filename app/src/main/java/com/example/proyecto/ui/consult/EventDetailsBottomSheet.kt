package com.example.proyecto.ui.consult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.proyecto.R
import com.example.proyecto.data.model.EventEntity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EventDetailsBottomSheet : BottomSheetDialogFragment(), OnMapReadyCallback {

    private var lat = 0.0
    private var lng = 0.0
    private var mapView: MapView? = null

    companion object {
        private const val ARG_CATEGORIA = "categoria"
        private const val ARG_FECHA = "fecha"
        private const val ARG_HORA = "hora"
        private const val ARG_DESCRIPCION = "descripcion"
        private const val ARG_ESTATUS = "estatus"
        private const val ARG_LAT = "lat"
        private const val ARG_LNG = "lng"
        private const val ARG_CONTACTO = "contacto"

        fun newInstance(event: EventEntity): EventDetailsBottomSheet {
            val fragment = EventDetailsBottomSheet()
            val args = Bundle().apply {
                putString(ARG_CATEGORIA, event.categoria)
                putString(ARG_FECHA, event.fecha)
                putString(ARG_HORA, event.hora)
                putString(ARG_DESCRIPCION, event.descripcion)
                putString(ARG_ESTATUS, event.estatus)
                putDouble(ARG_LAT, event.ubicacion_lat)
                putDouble(ARG_LNG, event.ubicacion_lng)
                putString(ARG_CONTACTO, event.contacto_nombre)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.bottom_sheet_event_details, container, false)

        val args = requireArguments()
        val categoria = args.getString(ARG_CATEGORIA, "")
        val fecha = args.getString(ARG_FECHA, "")
        val hora = args.getString(ARG_HORA, "")
        val descripcion = args.getString(ARG_DESCRIPCION, "")
        val estatus = args.getString(ARG_ESTATUS, "")
        lat = args.getDouble(ARG_LAT, 0.0)
        lng = args.getDouble(ARG_LNG, 0.0)
        val contacto = args.getString(ARG_CONTACTO, "")

        root.findViewById<TextView>(R.id.tv_details_category).text = categoria
        var displayDate = fecha
        try {
            val parts = fecha.split("-")
            if (parts.size == 3) {
                displayDate = String.format("%02d/%02d/%04d", parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
        } catch (e: Exception) {}
        root.findViewById<TextView>(R.id.tv_details_datetime).text = "$displayDate | $hora"
        root.findViewById<TextView>(R.id.tv_details_description).text = descripcion
        root.findViewById<TextView>(R.id.tv_details_contact).text = contacto
        root.findViewById<TextView>(R.id.tv_details_location).text = getString(R.string.item_location_format, lat, lng)

        val tvStatus = root.findViewById<TextView>(R.id.tv_details_status)
        tvStatus.text = estatus

        val statusColors = resources.getStringArray(R.array.status_array)
        val bgColor = when (estatus) {
            statusColors[0] -> R.color.status_pending
            statusColors[1] -> R.color.status_done
            statusColors[2] -> R.color.status_postponed
            else -> R.color.status_pending
        }
        val drawable = tvStatus.background?.mutate()
        drawable?.setTint(ContextCompat.getColor(requireContext(), bgColor))
        tvStatus.background = drawable

        mapView = root.findViewById(R.id.map_preview_details)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        return root
    }

    override fun onMapReady(googleMap: com.google.android.gms.maps.GoogleMap) {
        googleMap.uiSettings.isMapToolbarEnabled = false
        val position = LatLng(lat, lng)
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
}

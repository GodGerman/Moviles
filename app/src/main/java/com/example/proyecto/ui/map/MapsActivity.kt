package com.example.proyecto.ui.map

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.proyecto.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Propósito: Actividad dedicada a mostrar un mapa interactivo usando el SDK de Google Maps.
 * Rol en MVVM: Capa de Presentación (UI Layer). Actúa como una pantalla utilitaria auxiliar.
 * Interacciones: Es lanzada exclusivamente por [com.example.proyecto.ui.add.AddEventFragment] o fragmentos de edición para seleccionar una coordenada (Latitud, Longitud). Al terminar, envía los datos de regreso mediante un [Intent] con el resultado.
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    // Variable para controlar el mapa cuando esté cargado en memoria
    private lateinit var mMap: GoogleMap
    
    // Coordenada actual elegida por el usuario
    private var selectedLatLng: LatLng? = null
    
    // Cliente de servicios de Google Play para determinar la posición GPS/WiFi del dispositivo
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /**
     * API moderna de Android (`registerForActivityResult`) que sustituye a `onRequestPermissionsResult`.
     * Este objeto se encarga de mostrar la petición visual de ubicación (precisa o aproximada) y gestionar la respuesta asíncrona del usuario.
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            enableMyLocationAndCenter()
        } else {
            Toast.makeText(this, "Permiso denegado. Mostrando vista global.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Propósito: Inicializar la Actividad del Mapa, instanciando los servicios de GPS y solicitando la carga asíncrona de Google Maps.
     * Parámetros:
     * - savedInstanceState: El estado preservado de la actividad.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Configura la vista XML `activity_maps`.
     * 2. Instancia `FusedLocationProviderClient` de los servicios de Google Play.
     * 3. Extrae el `SupportMapFragment` del layout y ejecuta `getMapAsync(this)` para iniciar la descarga visual del mapa en background.
     * 4. Asigna un click listener al botón "Confirmar" para que, al tocarlo, empaquete las coordenadas en un `Intent` y retorne `Activity.RESULT_OK` a quien lo haya invocado.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Inicializamos el proveedor de ubicación fusionada (Google Play Services)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Cargamos el mapa en segundo plano. Cuando termine, llamará automáticamente a 'onMapReady'.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Botón para confirmar la selección y devolver la latitud/longitud
        findViewById<Button>(R.id.btn_confirm_location).setOnClickListener {
            selectedLatLng?.let {
                // Creamos un Intent vacío y le "pegamos" las coordenadas extra (Extras)
                val resultIntent = Intent().apply {
                    putExtra("lat", it.latitude)
                    putExtra("lng", it.longitude)
                }
                // Avisamos a Android que todo salió bien y devolvemos la información a la Activity/Fragment solicitante
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Cierra MapsActivity
            } ?: run {
                Toast.makeText(this, "Por favor, selecciona una ubicación tocando el mapa.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Propósito: Callback o punto de entrada que se ejecuta automáticamente cuando el mapa terminó de renderizarse por completo.
     * Parámetros:
     * - googleMap: Instancia nativa de [GoogleMap] que permite controlar las cámaras, polígonos y pines del mapa.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Guarda la referencia en la variable global `mMap`.
     * 2. Posiciona la cámara en una ubicación por defecto (Centro de México) con un zoom generalizado.
     * 3. Manda llamar a `checkLocationPermissions()` para intentar mover la cámara a donde está físicamente el usuario.
     * 4. Asigna un `setOnMapClickListener` que, ante cada toque en la pantalla, borra pines viejos (`clear`), coloca uno nuevo (`addMarker`) y actualiza la variable `selectedLatLng`.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // Coordenada global por defecto (ej. Ciudad de México)
        val defaultLocation = LatLng(19.4326, -99.1332)
        // moveCamera teletransporta la visión sin animación a un nivel de zoom lejano (5f, nivel país)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))
        
        checkLocationPermissions()

        // Cada vez que el usuario tapea el mapa...
        mMap.setOnMapClickListener { latLng ->
            mMap.clear() // Quita marcas previas para mantener solo UNA marca a la vez
            mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación Seleccionada"))
            selectedLatLng = latLng
        }
    }

    /**
     * Propósito: Validar si la app posee permisos de localización concedidos previamente o solicitarlos al sistema en tiempo real.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna: Revisa en `ContextCompat` si tiene otorgados los permisos de FINE LOCATION (precisa) o COARSE LOCATION (aproximada). Si es afirmativo, procede a centrar el mapa invocando `enableMyLocationAndCenter`. En caso contrario, lanza la petición visual del sistema usando el launcher `locationPermissionRequest`.
     */
    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocationAndCenter()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    /**
     * Propósito: Activa la visualización de "Mi Ubicación" (Punto Azul estandarizado) en el mapa y desplaza la cámara hacia dicha coordenada.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Habilita el flag nativo `mMap.isMyLocationEnabled = true`.
     * 2. Solicita al `fusedLocationClient` la última ubicación calculada conocida por el dispositivo móvil.
     * 3. Tras obtener éxito mediante un `addOnSuccessListener`, si la ubicación reportada no es nula, realiza una animación cinematográfica de cámara (`animateCamera`) hacia el nuevo [LatLng] aplicando un zoom detallado a nivel calles (15f).
     */
    private fun enableMyLocationAndCenter() {
        try {
            mMap.isMyLocationEnabled = true
            
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

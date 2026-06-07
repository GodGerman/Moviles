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

/**
 * Propósito: Fragmento de la Pantalla Principal (Home/Dashboard). Actúa como un resumen rápido de tareas entrantes para el usuario recién iniciada la sesión.
 * Rol en MVVM: Capa de Presentación (UI Layer). Implementa observables dinámicos directos a datos calculados temporalmente (vistos por `getUpcomingEvents()`) en el ViewModel maestro.
 * Interacciones: Solicita a [EventViewModel] datos reducidos de eventos. Utiliza la estructura modular de reciclaje visual [EventAdapter] e incrusta el BottomSheet [com.example.proyecto.ui.consult.EventDetailsBottomSheet] al tap-to-interact en la lista.
 */
class HomeFragment : Fragment() {

    // ViewModel compartido de forma universal con la Actividad madre.
    private val eventViewModel: EventViewModel by activityViewModels()

    /**
     * Propósito: Construcción de la visualización estructural y la vinculación de elementos de capa Reactiva (`LiveData`) a la vista activa.
     * Parámetros:
     * - inflater: Mecanismo de expansión XML.
     * - container: Contenedor padre de Android.
     * - savedInstanceState: Caché persistida.
     * Retorno: Objeto instanciado visible de tipo [View].
     * Lógica interna: 
     * 1. Infla el diseño `fragment_home` a memoria.
     * 2. Instancia un puente visual list-based (`EventAdapter`) proveyéndole la lógica tipo callback lambda necesaria para despachar un diálogo miniatura flotante BottomSheet de vista técnica al interactuar con un list-item individual.
     * 3. Configura el listado base acoplándolo a su propio adaptador y configurando sus directrices y flujos como `LinearLayoutManager`.
     * 4. Lanza de inmediato una función `observe` enlazada a `getUpcomingEvents()`. Este observador automáticamente notificará al adaptador inyectando los datos devueltos siempre que hayan mutaciones nativas de Room y este componente figure activo en el Foreground.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        // El RecyclerView es la forma moderna en Android para procesar y renderizar "infinitas" celdas reutilizables para ahorrar memoria base.
        val recyclerView: RecyclerView = root.findViewById(R.id.rv_events)
        
        val adapter = EventAdapter(
            onItemClick = { event ->
                val bottomSheet = com.example.proyecto.ui.consult.EventDetailsBottomSheet.newInstance(event)
                bottomSheet.show(childFragmentManager, "EventDetailsBottomSheet")
            }
        )
        
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Nos suscribimos a los datos del ViewModel usando el 'viewLifecycleOwner'
        // Esto previene que una lista cambie, intente repintar la UI pero el fragmento ya no exista, evitando Crashes (Fugas).
        eventViewModel.getUpcomingEvents().observe(viewLifecycleOwner) { events ->
            // Inyectamos el nuevo arreglo. Si la UI nota un diferencial lo repintará suavemente animándolo en vez de recargarlo forzosamente.
            events?.let { adapter.submitList(it) }
        }

        return root
    }
}

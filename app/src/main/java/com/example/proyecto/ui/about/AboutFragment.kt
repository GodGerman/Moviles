package com.example.proyecto.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.proyecto.R

/**
 * Propósito: Fragmento que renderiza la pantalla de "Acerca de" (About).
 * Rol en MVVM: Capa de Presentación (UI Layer). Su única función es mostrar una vista estática con la información de los desarrolladores o la versión de la aplicación.
 * Interacciones: No interactúa con ViewModels ni bases de datos. Solamente es orquestado por el componente de Navegación de [com.example.proyecto.MainActivity].
 */
class AboutFragment : Fragment() {

    /**
     * Propósito: Método del ciclo de vida del fragmento responsable de inicializar la interfaz gráfica.
     * Parámetros:
     * - inflater: Herramienta para convertir ("inflar") un archivo XML en una jerarquía de objetos [View] en memoria.
     * - container: El contenedor [ViewGroup] padre que alojará la vista del fragmento (el NavHostFragment).
     * - savedInstanceState: Datos de estado previos (si existen).
     * Retorno: Un objeto [View] que representa la raíz de la interfaz gráfica de este fragmento.
     * Lógica interna: Invoca `inflater.inflate` pasando la referencia al archivo de diseño `fragment_about.xml`. El parámetro `false` indica que no lo adhiera al padre inmediatamente, ya que el sistema de navegación se encargará de ello.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }
}

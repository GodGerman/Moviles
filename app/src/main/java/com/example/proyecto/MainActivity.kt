package com.example.proyecto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

/**
 * Propósito: Actividad Principal (Activity) que sirve como contenedor de toda la interfaz gráfica.
 * Rol en MVVM: Capa de Presentación (UI Layer). Esta aplicación sigue el modelo de "Single Activity Architecture" (Arquitectura de una sola Actividad). Aloja el `NavHostFragment` que a su vez renderiza y reemplaza los distintos fragmentos bajo demanda.
 * Interacciones: Interactúa con el componente de Navegación de Jetpack (NavController) para gestionar las transiciones hacia fragmentos como [com.example.proyecto.ui.home.HomeFragment], [com.example.proyecto.ui.add.AddEventFragment], etc.
 */
class MainActivity : AppCompatActivity() {

    // Objeto de configuración para coordinar cómo se comportan la barra superior (Toolbar) y el menú lateral desplegable (Drawer).
    private lateinit var appBarConfiguration: AppBarConfiguration

    /**
     * Propósito: Método de ciclo de vida invocado cuando se crea la Actividad por primera vez.
     * Parámetros:
     * - savedInstanceState: Objeto [Bundle] que puede contener estados guardados previamente en caso de que la actividad haya sido destruida por el sistema y recreada.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Infla el diseño XML principal (`activity_main`).
     * 2. Configura la [Toolbar] personalizada como la ActionBar de la aplicación.
     * 3. Inicializa las referencias visuales (`DrawerLayout`, `NavigationView`, `BottomNavigationView`).
     * 4. Recupera el `NavController` desde el `NavHostFragment` para delegar la navegación.
     * 5. Define los destinos de "nivel superior" (los que muestran menú hamburguesa en vez de flecha atrás) usando `AppBarConfiguration`.
     * 6. Vincula la barra superior y los menús inferior y lateral al NavController, logrando que un toque en un menú abra el fragmento automáticamente.
     * 7. Ejecuta la solicitud de permisos dinámicos (notificaciones en Android 13+).
     * 8. Añade listeners manuales para manejar el caso particular del botón "Salir".
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar la barra superior personalizada (Material Design)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Inicializar componentes visuales clave (Menú lateral y Menú inferior)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view) 
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav) 

        // El NavController es el componente encargado de orquestar los saltos entre Fragments.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Configuramos qué pantallas son de "inicio" para no mostrar flecha de retroceso.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_add_event, R.id.nav_consult_events,
                R.id.nav_calendar, R.id.nav_backup, R.id.nav_restore, R.id.nav_about
            ), drawerLayout
        )

        // Automágia de Jetpack: conecta la IU con las reglas de navegación definidas en navigation.xml
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        bottomNav.setupWithNavController(navController)

        // Solicitud en tiempo de ejecución de permisos necesarios (Notificaciones)
        requestNotificationPermission()

        // Manejo especial para la opción Salir del menú lateral
        navView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.nav_exit) {
                finish() // Cierra la Actividad principal, finalizando la app
                true
            } else {
                // Si no es salir, dejamos que la utilería de Jetpack maneje el salto de pantalla
                val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(menuItem, navController)
                if (handled) drawerLayout.closeDrawers() // Cerramos el panel deslizable tras una selección exitosa
                handled
            }
        }

        // Manejo de clicks especial para el menú inferior
        bottomNav.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.nav_exit) {
                finish()
                true
            } else {
                val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(menuItem, navController)
                handled
            }
        }
    }

    /**
     * Propósito: Solicita permisos de notificación de forma dinámica al usuario si es necesario.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Comprueba si el dispositivo corre bajo Android 13 (Tiramisu) o superior.
     * 2. Si es así, revisa si el usuario ya otorgó el permiso `POST_NOTIFICATIONS`.
     * 3. Si no ha sido concedido, lanza un diálogo nativo (`ActivityCompat.requestPermissions`) pidiendo la autorización para poder mostrar alertas de recordatorios.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    /**
     * Propósito: Maneja el evento de presionar la flecha de retroceso ("Up") en la barra superior.
     * Parámetros: Ninguno.
     * Retorno: Boolean indicando `true` si el retroceso fue manejado exitosamente.
     * Lógica interna: 
     * Delega completamente la acción al `NavController`. Este componente decidirá, con base a su pila de retroceso (BackStack) y la `appBarConfiguration`, si debe sacar el fragmento actual o si debe abrir el menú lateral (hamburguesa).
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}

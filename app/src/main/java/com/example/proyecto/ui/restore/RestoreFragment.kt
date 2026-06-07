package com.example.proyecto.ui.restore

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.proyecto.R
import com.example.proyecto.data.repository.DriveServiceHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * Propósito: Fragmento responsable de Recuperar (Restaurar) la base de datos desde la nube.
 * Rol en MVVM: Capa de Presentación (UI Layer). Controla la autenticación, delega la descarga a DriveServiceHelper y maneja el comportamiento crítico de reiniciar la aplicación post-restauración.
 * Interacciones: Interactúa fuertemente con la clase helper [DriveServiceHelper]. Manda a cerrar y destruir la Actividad Padre [com.example.proyecto.MainActivity] tras un reemplazo de base de datos exitoso para que Room no trabaje con archivos corruptos.
 */
class RestoreFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var driveServiceHelper: DriveServiceHelper? = null

    private lateinit var btnAuth: Button
    private lateinit var btnRestore: Button
    private lateinit var progressBar: ProgressBar

    /**
     * Propósito: `registerForActivityResult` que asiste al fragmento interceptando el cierre de la ventana de login de Google.
     * Lógica interna: Revisa el `resultCode`. Si fue `RESULT_OK`, extrae la información de la cuenta provista en el intent y la retransmite al método encargado de configurar el cliente `Drive`.
     */
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .addOnSuccessListener { account ->
                    setupDriveClient(account)
                }
                .addOnFailureListener {
                    Toast.makeText(context, R.string.toast_auth_failed, Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Propósito: Desinflar la estructura visual en memoria y asociar lógicas base.
     * Parámetros:
     * - inflater: Inflador nativo.
     * - container: Contenedor subyacente.
     * - savedInstanceState: Caché de variables pasadas.
     * Retorno: Un [View] listo para renderizarse en pantalla.
     * Lógica interna: Infla el XML `fragment_restore`, recupera referencias visuales, establece las reglas (`Scope`) del `GoogleSignInOptions` a únicamente leer archivos de la app, y configura listeners de clicks para los botones principales.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_restore, container, false)
        
        btnAuth = root.findViewById(R.id.btn_authenticate_restore)
        btnRestore = root.findViewById(R.id.btn_start_restore)
        progressBar = root.findViewById(R.id.progress_restore)

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), signInOptions)

        btnAuth.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        btnRestore.setOnClickListener {
            performRestore()
        }

        return root
    }

    /**
     * Propósito: Método de ciclo de vida que busca aplicar "Auto-login".
     * Parámetros: 
     * - view: Vista.
     * - savedInstanceState: Estado.
     * Retorno: Ninguno.
     * Lógica interna: Consulta si ya existía una cuenta autenticada silenciosamente a través de `GoogleSignIn.getLastSignedInAccount`. Si el token sigue vigente, autoconfigura el cliente y habilita la funcionalidad sin pedir clicks extra.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            setupDriveClient(account)
        }
    }

    /**
     * Propósito: Inicializa el servicio de Google Drive usando el token de la cuenta autorizada.
     * Parámetros:
     * - account: Objeto `GoogleSignInAccount` validado.
     * Retorno: Ninguno.
     * Lógica interna: Emplea la cuenta autorizada para crear una credencial `GoogleAccountCredential`, arranca un `Drive.Builder`, actualiza la instancia `DriveServiceHelper` y desbloquea el botón de restauración mostrándole al usuario el correo conectado en el botón superior.
     */
    private fun setupDriveClient(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            requireContext(), listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        val googleDriveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(getString(R.string.app_name)).build()

        driveServiceHelper = DriveServiceHelper(googleDriveService)
        btnAuth.text = account.email
        btnRestore.isEnabled = true
    }

    /**
     * Propósito: Proceso crítico de restauración en segundo plano que reemplaza el archivo SQLite.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Despliega la animación de carga y desactiva el botón de restauración.
     * 2. Llama dentro de una corrutina a `downloadDatabaseFile()`.
     * 3. Si la respuesta es exitosa (`true`), esto significa que el archivo "event_database" físico ha sido sobrescrito brutalmente por debajo. Room, al mantener una conexión tipo Singleton al archivo viejo en memoria RAM, causará que la aplicación haga "Crash" si intentamos leerla.
     * 4. Para evitar corrupciones, mostramos un Toast largo, llamamos a `finish()` sobre el Activity contenedor y ejecutamos `exitProcess(0)` para matar por completo y de forma segura el proceso nativo de Android. Esto forza al usuario a reabrir la app, cargando los nuevos datos de forma limpia.
     */
    private fun performRestore() {
        progressBar.visibility = View.VISIBLE
        btnRestore.isEnabled = false 
        
        viewLifecycleOwner.lifecycleScope.launch {
            val success = driveServiceHelper?.downloadDatabaseFile(requireContext(), "event_database") ?: false
            progressBar.visibility = View.GONE
            
            if (success) {
                // NOTA CRÍTICA:
                // Dado que Room mantiene una conexión Singleton a la base de datos antigua
                // y acabamos de reemplazar el archivo físico por debajo, necesitamos
                // matar la app por completo para que SQLite recargue los punteros.
                Toast.makeText(context, R.string.toast_restore_success, Toast.LENGTH_LONG).show()
                requireActivity().finish() // Cierra la Activity Principal de inmediato
                exitProcess(0) // Mata el proceso en memoria (PID)
            } else {
                // Si hubo un error de red o no existe backup en Drive, se libera el botón
                btnRestore.isEnabled = true
                Toast.makeText(context, R.string.toast_restore_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

package com.example.proyecto.ui.backup

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

/**
 * Propósito: Fragmento responsable de realizar la Copia de Seguridad (Backup) en la nube.
 * Rol en MVVM: Capa de Presentación (UI Layer). Controla la interfaz de usuario para el inicio de sesión OAuth 2.0 y coordina la llamada al Helper de subida.
 * Interacciones: Instancia directamente y se comunica con [DriveServiceHelper] para realizar operaciones de red. También se auxilia de la API de Autenticación de Google (`GoogleSignIn`).
 */
class BackupFragment : Fragment() {

    // Cliente que maneja el cuadro de diálogo de "¿Con qué cuenta de Google quieres iniciar sesión?"
    private lateinit var googleSignInClient: GoogleSignInClient
    
    // Clase Helper que abstrae las peticiones HTTP crudas a la API de Drive
    private var driveServiceHelper: DriveServiceHelper? = null

    private lateinit var btnAuth: Button
    private lateinit var btnBackup: Button
    private lateinit var progressBar: ProgressBar

    /**
     * Propósito: `registerForActivityResult` usado para capturar la cuenta seleccionada en el launcher de Google Sign-In.
     * Lógica interna: Espera el `RESULT_OK` de la ventana nativa de selección de cuentas de Google. Extrae exitosamente la `GoogleSignInAccount` y delega al método `setupDriveClient`. En caso de fallo, muestra un Toast.
     */
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Si el login fue exitoso, extraemos la cuenta seleccionada del intent de respuesta
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
     * Propósito: Crea la vista principal del fragmento y configura los listeners de botones y el cliente de Google Sign-In.
     * Parámetros:
     * - inflater: Inflador de layouts XML.
     * - container: Contenedor padre proporcionado por el fragment_host.
     * - savedInstanceState: Estado preservado previo.
     * Retorno: Raíz de la vista [View].
     * Lógica interna: 
     * 1. Infla `fragment_backup`.
     * 2. Configura los scopes de `GoogleSignInOptions` para solicitar permiso exclusivo de escritura/lectura en los archivos generados por la app (`DRIVE_FILE`).
     * 3. Configura los eventos `setOnClickListener` de los botones para lanzar el `googleSignInLauncher` y `performBackup`.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_backup, container, false)
        
        btnAuth = root.findViewById(R.id.btn_authenticate)
        btnBackup = root.findViewById(R.id.btn_start_backup)
        progressBar = root.findViewById(R.id.progress_backup)

        // DRIVE_FILE significa: "Solo queremos ver y modificar archivos creados por esta misma app".
        // Es el nivel de permiso (Scope) más seguro disponible.
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), signInOptions)

        btnAuth.setOnClickListener {
            // Abre la pantalla nativa de Android flotante de "Elige una cuenta de Google"
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        btnBackup.setOnClickListener {
            performBackup()
        }

        return root
    }

    /**
     * Propósito: Metodo de ciclo de vida llamado una vez que la vista XML se haya renderizado.
     * Parámetros: 
     * - view: Vista principal creada.
     * - savedInstanceState: Estado preservado.
     * Retorno: Ninguno.
     * Lógica interna: Revisa si existe una sesión iniciada previamente (`getLastSignedInAccount`) y auto-autentica (Auto-Login) la cuenta para evitar molestar al usuario con la pantalla de acceso nuevamente.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Auto-Login
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            setupDriveClient(account)
        }
    }

    /**
     * Propósito: Construye el cliente HTTP subyacente (`Drive`) utilizando la cuenta Google del usuario.
     * Parámetros:
     * - account: Objeto `GoogleSignInAccount` proveniente del launcher o del auto-login.
     * Retorno: Ninguno.
     * Lógica interna: 
     * 1. Genera un `GoogleAccountCredential` inyectando el OAuth y el Scope `DRIVE_FILE`.
     * 2. Instancia `Drive.Builder` pasándole el motor de red y de JSON.
     * 3. Crea la instancia de `DriveServiceHelper` e inicializa el botón de subida.
     */
    private fun setupDriveClient(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            requireContext(), listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        
        val googleDriveService = Drive.Builder(
            NetHttpTransport(), // Motor de red nativo (HTTP)
            GsonFactory(),      // Parseador JSON
            credential
        ).setApplicationName(getString(R.string.app_name)).build()

        driveServiceHelper = DriveServiceHelper(googleDriveService)
        
        // Refleja en pantalla con qué correo se logueó
        btnAuth.text = account.email
        // Habilita el botón de Backup que estaba bloqueado inicialmente
        btnBackup.isEnabled = true
    }

    /**
     * Propósito: Ejecuta visualmente el proceso de subir la base de datos (SQLite) a Google Drive coordinando la UI con la corrutina de fondo.
     * Parámetros: Ninguno.
     * Retorno: Ninguno.
     * Lógica interna:
     * 1. Muestra la barra de progreso indeterminada.
     * 2. Bloquea el botón de backup para impedir dobles clicks o spam.
     * 3. Inicia `lifecycleScope.launch` donde invoca `uploadDatabaseFile` delegando al Helper.
     * 4. Al finalizar la suspensión asíncrona, oculta la barra, rehabilita el botón y muestra un Toast confirmando el éxito o el fallo en función del ID devuelto.
     */
    private fun performBackup() {
        progressBar.visibility = View.VISIBLE
        btnBackup.isEnabled = false // Evitar múltiples clics
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Se envía el nombre exacto de la BD definida en la clase AppDatabase de Room: "event_database"
            val fileId = driveServiceHelper?.uploadDatabaseFile(requireContext(), "event_database")
            
            progressBar.visibility = View.GONE
            btnBackup.isEnabled = true
            
            if (fileId != null) {
                Toast.makeText(context, R.string.toast_backup_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.toast_backup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

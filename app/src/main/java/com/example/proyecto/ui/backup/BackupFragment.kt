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

class BackupFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var driveServiceHelper: DriveServiceHelper? = null

    private lateinit var btnAuth: Button
    private lateinit var btnBackup: Button
    private lateinit var progressBar: ProgressBar

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_backup, container, false)
        
        btnAuth = root.findViewById(R.id.btn_authenticate)
        btnBackup = root.findViewById(R.id.btn_start_backup)
        progressBar = root.findViewById(R.id.progress_backup)

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), signInOptions)

        btnAuth.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        btnBackup.setOnClickListener {
            performBackup()
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Mantener la sesión iniciada automáticamente
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            setupDriveClient(account)
        }
    }

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
        btnBackup.isEnabled = true
    }

    private fun performBackup() {
        progressBar.visibility = View.VISIBLE
        btnBackup.isEnabled = false
        
        viewLifecycleOwner.lifecycleScope.launch {
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

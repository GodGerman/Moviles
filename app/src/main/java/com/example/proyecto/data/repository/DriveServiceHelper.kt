package com.example.proyecto.data.repository

import android.content.Context
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import com.example.proyecto.data.local.AppDatabase
import java.io.File

class DriveServiceHelper(private val driveService: Drive) {

    suspend fun uploadDatabaseFile(context: Context, databaseName: String): String? = withContext(Dispatchers.IO) {
        try {
            // CRÍTICO: Forzar Checkpoint sin cerrar la DB para no desconectar los Flow de la UI
            val appDatabase = AppDatabase.getDatabase(context)
            appDatabase.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath(databaseName)
            if (!dbFile.exists()) return@withContext null

            // 1. Check if the file already exists in Drive to update it
            val fileList: FileList = driveService.files().list()
                .setSpaces("drive")
                .setQ("name='$databaseName' and trashed=false")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id

            // 2. Prepare metadata and content
            val fileMetadata = DriveFile().apply {
                name = databaseName
            }
            val mediaContent = FileContent("application/x-sqlite3", dbFile)

            // 3. Update or Create
            val file = if (existingFileId != null) {
                driveService.files().update(existingFileId, null, mediaContent).execute()
            } else {
                driveService.files().create(fileMetadata, mediaContent).execute()
            }
            return@withContext file.id
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun downloadDatabaseFile(context: Context, databaseName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Search for the file in Drive
            val fileList: FileList = driveService.files().list()
                .setSpaces("drive")
                .setQ("name='$databaseName' and trashed=false")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id ?: return@withContext false

            // CRÍTICO: Cerrar la base de datos local para liberar locks
            AppDatabase.getDatabase(context).close()

            // 2. Download to local database path
            val dbFile = context.getDatabasePath(databaseName)
            val outputStream = FileOutputStream(dbFile)
            
            driveService.files().get(existingFileId).executeMediaAndDownloadTo(outputStream)
            outputStream.flush()
            outputStream.close()
            
            // CRÍTICO: Eliminar memoria temporal (WAL/SHM) para que Room no la sobreescriba en el reinicio
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            
            return@withContext true
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext false
        }
    }
}

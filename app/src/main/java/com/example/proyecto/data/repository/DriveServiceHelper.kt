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

/**
 * Propósito: Clase auxiliar para gestionar las operaciones de Google Drive (Backup y Restauración).
 * Rol en MVVM: Capa de Datos (Data Layer). Proporciona una interfaz para interactuar con la API de Google Drive, encargándose de subir y descargar el archivo físico de la base de datos local.
 * Interacciones: Es instanciada y utilizada principalmente por [com.example.proyecto.ui.backup.BackupFragment] y [com.example.proyecto.ui.restore.RestoreFragment].
 */
class DriveServiceHelper(private val driveService: Drive) {

    /**
     * Propósito: Sube el archivo local de la base de datos a Google Drive.
     * Parámetros:
     * - context: El Contexto de la aplicación, necesario para acceder a la base de datos Room.
     * - databaseName: El nombre físico del archivo de base de datos a respaldar.
     * Retorno: Un String con el ID del archivo creado o actualizado en Drive, o null si falló el proceso.
     * Lógica interna:
     * 1. Usa `withContext(Dispatchers.IO)` para ejecutar la operación en un hilo secundario y no bloquear la UI.
     * 2. Fuerza un Checkpoint (wal_checkpoint) en la base de datos local para que Room consolide la información en memoria temporal hacia el archivo principal SQLite.
     * 3. Verifica si el archivo ya existe en Drive utilizando una consulta (Q="name='...' and trashed=false").
     * 4. Si existe, actualiza su contenido; si no, crea uno nuevo.
     */
    suspend fun uploadDatabaseFile(context: Context, databaseName: String): String? = withContext(Dispatchers.IO) {
        try {
            val appDatabase = AppDatabase.getDatabase(context)
            // Ya no se requiere PRAGMA wal_checkpoint porque configuramos Room para usar JournalMode.TRUNCATE en lugar de WAL.
            // Por lo tanto, event_database siempre contendrá los datos más recientes al instante.

            val dbFile = context.getDatabasePath(databaseName)
            if (!dbFile.exists()) return@withContext null

            // Buscamos en Drive si el archivo ya existe
            val fileList: FileList = driveService.files().list()
                .setSpaces("drive")
                .setQ("name='$databaseName' and trashed=false")
                .execute()

            // Eliminar todos los respaldos anteriores para forzar una sobreescritura (overwrite) limpia
            for (existingFile in fileList.files) {
                try {
                    driveService.files().delete(existingFile.id).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Preparamos los metadatos (nombre del archivo) y el contenido físico (FileContent).
            val fileMetadata = DriveFile().apply {
                name = databaseName
            }
            val mediaContent = FileContent("application/x-sqlite3", dbFile)

            // Creamos un archivo completamente nuevo
            val file = driveService.files().create(fileMetadata, mediaContent).execute()
            return@withContext file.id
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Propósito: Descarga el archivo de respaldo de Google Drive y reemplaza la base de datos local.
     * Parámetros:
     * - context: El Contexto de la aplicación.
     * - databaseName: El nombre del archivo a descargar y reemplazar.
     * Retorno: `true` si la descarga y reemplazo fue exitosa, `false` en caso contrario.
     * Lógica interna:
     * 1. Usa `Dispatchers.IO` por ser una operación intensiva de red y disco.
     * 2. Busca el archivo correspondiente en Google Drive. Si no existe, retorna falso temprano.
     * 3. Cierra la base de datos local actual para liberar los bloqueos (locks) de SQLite.
     * 4. Descarga el archivo de Drive y lo escribe directamente sobre el archivo local de la base de datos.
     * 5. Elimina los archivos temporales `-wal` y `-shm` locales para evitar corrupción de datos al reiniciar la conexión.
     */
    suspend fun downloadDatabaseFile(context: Context, databaseName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Buscamos el archivo en Drive.
            val fileList: FileList = driveService.files().list()
                .setSpaces("drive")
                .setQ("name='$databaseName' and trashed=false")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id ?: return@withContext false

            // CRÍTICO: Cerrar la base de datos local para liberar locks (bloqueos).
            // Si intentamos sobreescribir el archivo mientras Room lo está usando, causaremos un crash.
            AppDatabase.getDatabase(context).close()

            // Descargamos el archivo directamente en la ruta de bases de datos de Android.
            val dbFile = context.getDatabasePath(databaseName)
            val outputStream = FileOutputStream(dbFile)
            
            driveService.files().get(existingFileId).executeMediaAndDownloadTo(outputStream)
            outputStream.flush()
            outputStream.close()
            
            // CRÍTICO: Eliminar memoria temporal (WAL/SHM) para que Room no la sobreescriba en el reinicio.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            
            return@withContext true
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext false
        }
    }
}

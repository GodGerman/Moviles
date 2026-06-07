package com.example.proyecto.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.proyecto.data.model.EventEntity

/**
 * Propósito: Clase principal de la base de datos usando Room. Actúa como el punto de acceso principal a la base de datos SQLite subyacente.
 * Rol en MVVM: Capa de Datos (Data Source). Proporciona la infraestructura de persistencia local que el Repository consumirá.
 * Interacciones: Es utilizada por [com.example.proyecto.data.repository.EventRepository] para obtener el DAO. También es consultada en [com.example.proyecto.data.repository.DriveServiceHelper] para realizar copias de seguridad físicas de la base de datos.
 */
@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Propósito: Proporciona la instancia del DAO asociado a los eventos.
     * Parámetros: Ninguno.
     * Retorno: Instancia de [EventDao] configurada por Room.
     * Lógica interna: Room genera automáticamente el código de implementación de esta función durante la compilación, conectando las consultas SQL con la base de datos.
     */
    abstract fun eventDao(): EventDao

    /**
     * Companion object (objeto estático en Kotlin) para aplicar el patrón Singleton.
     * Garantizamos que solo exista UNA única instancia de la base de datos en toda la aplicación,
     * evitando problemas de concurrencia y fugas de memoria.
     */
    companion object {
        // @Volatile asegura que el valor de INSTANCE siempre esté actualizado
        // y sea visible de inmediato para todos los hilos de ejecución (threads).
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Propósito: Obtiene la instancia única de la base de datos, creándola si no existe.
         * Parámetros:
         * - context: El contexto de la aplicación, necesario para acceder al sistema de archivos y crear la base de datos.
         * Retorno: La instancia Singleton de [AppDatabase].
         * Lógica interna:
         * 1. Revisa si `INSTANCE` ya tiene un valor. Si es así, lo devuelve (retorno temprano).
         * 2. Si es nula, ingresa a un bloque `synchronized` para asegurar que múltiples hilos no intenten crear la base de datos simultáneamente.
         * 3. Construye la base de datos usando `Room.databaseBuilder`, asignando la clase `AppDatabase` y el nombre físico del archivo `"event_database"`.
         * 4. Asigna la nueva instancia a la variable `INSTANCE` y la retorna.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // 'synchronized' evita que dos hilos intenten crear la base de datos al mismo tiempo.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, // La clase de nuestra base de datos
                    "event_database"         // El nombre físico del archivo de la base de datos
                )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // Apagar modo WAL para que el archivo .db siempre esté completo para los respaldos
                .build()
                
                // Guardamos la instancia recién creada en nuestra variable para futuros usos.
                INSTANCE = instance
                instance
            }
        }
    }
}

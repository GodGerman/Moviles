# 🚀 Organizador de Tareas

**Organizador de Tareas** es una aplicación móvil nativa para Android diseñada para gestionar, planificar y sincronizar actividades diarias de manera eficiente. Más allá de ser una herramienta organizativa.

**Desarrolladores:**
* César Fernando Aguilar Bautista
* Laura Michelle Dorantes Andrade
* Sofía Denisse Nájera García

---

## 🧠 Arquitectura de Software

### MVVM (Model-View-ViewModel)
Este proyecto implementa el patrón de arquitectura **MVVM**. En el desarrollo moderno de software, mezclar la lógica de la interfaz de usuario con la lógica de negocio o de acceso a datos resulta en código espagueti, difícil de mantener y probar.

*   **Model (Modelo):** Representa la capa de datos (bases de datos, APIs). Es responsable de gestionar la lógica de negocio y proveer la información limpia a las capas superiores.
*   **View (Vista):** Representa la interfaz gráfica (Actividades y Fragmentos en Android). Su única responsabilidad es observar los cambios en los datos y actualizar la pantalla, capturando las interacciones del usuario sin realizar cálculos complejos.
*   **ViewModel:** Actúa como el puente o intermediario. Expone flujos de datos (como `LiveData` o `StateFlow`) que la Vista observa. Sobrevive a los cambios de configuración del dispositivo (como girar la pantalla), evitando que los datos se pierdan o se vuelvan a cargar desde la base de datos innecesariamente.

El uso estricto de MVVM asegura que nuestra aplicación sea escalable, modular, fácilmente testeable y que cumpla con el principio de responsabilidad única (SOLID).

---

## 🛠️ Tecnologías Utilizadas (Sección Didáctica)
### 1. Kotlin
*   **¿Qué es?** Es el lenguaje de programación moderno, multiparadigma, oficial y fuertemente recomendado por Google para el desarrollo en Android.
*   **¿Por qué se usó?** A diferencia de Java, Kotlin es **seguro contra nulos (Null Safety)** desde su diseño. Esto reduce drásticamente los errores en tiempo de ejecución (como el temido `NullPointerException` que hace crashear las apps). Además, su sintaxis concisa y el uso de *Coroutines* facilita enormemente la programación asíncrona sin bloquear el hilo principal de la interfaz de usuario.

### 2. Room Database (SQLite)
*   **¿Qué es?** Room es una biblioteca de persistencia de datos de Google que proporciona una capa de abstracción sobre SQLite (el motor de base de datos relacional integrado en los teléfonos Android). Funciona de manera similar a un ORM (Object-Relational Mapping).
*   **¿Por qué se usó?** Escribir consultas SQL crudas en texto plano es propenso a errores tipográficos y es difícil de mantener. Room nos protege de esto verificando las consultas SQL en *tiempo de compilación*.
    *   **Entidades (Entities):** Usamos clases de datos (Data Classes) anotadas para representar automáticamente tablas en la base de datos.
    *   **DAOs (Data Access Objects):** Son interfaces donde definimos los métodos para interactuar con la base de datos (Insertar, Actualizar, Borrar, Leer), aislando completamente la lógica de acceso a datos del resto de la aplicación.

### 3. ViewBinding
*   **¿Qué es?** Es una característica de Android Jetpack que genera automáticamente una clase de enlace (binding class) para cada archivo de diseño XML de la interfaz gráfica.
*   **¿Por qué se usó?** Es el reemplazo moderno y seguro del antiguo y problemático método `findViewById`. Mientras que `findViewById` podía devolver nulos si un ID no existía en el layout actual, o causar excepciones de tipo por casteos inseguros, ViewBinding garantiza **seguridad de tipos** y **seguridad de nulos**, evitando colapsos inesperados en la UI. Las vistas se convierten en propiedades directas del objeto *binding*.

### 4. Material Design 3
*   **¿Qué es?** Es la versión más reciente del sistema de diseño creado por Google, que proporciona directrices, componentes visuales prefabricados y herramientas para crear interfaces de usuario hermosas y funcionales.
*   **¿Por qué se usó?** Para garantizar una UI/UX (Interfaz y Experiencia de Usuario) moderna, accesible y coherente con el ecosistema Android actual. Material 3 nos permitió implementar elementos como botones flotantes, tarjetas y barras de navegación estandarizadas y estéticamente agradables sin reinventar la rueda.

### 5. Google Maps SDK & FusedLocationProviderClient
*   **¿Qué es?** El SDK de Maps permite incrustar funcionalidad de mapas de Google directamente en la app. El *FusedLocationProviderClient* es la API de ubicación más moderna de Google Play Services, optimizada para gestionar el uso de batería y ofrecer alta precisión.
*   **¿Por qué se usó?** Para permitir a los usuarios asociar una ubicación geográfica específica a sus tareas. El FusedLocationProviderClient captura las coordenadas actuales de forma rápida y eficiente. Para la visualización en listas, se implementó el **"Lite Mode"** de Google Maps, el cual renderiza una imagen estática (bitmap) del mapa en lugar de un motor de renderizado interactivo completo. Esto ahorra masivamente memoria RAM y recursos del procesador cuando solo se necesita una vista previa.

### 6. Google Drive API (OAuth 2.0)
*   **¿Qué es?** La interfaz de programación que permite a nuestra aplicación comunicarse de forma segura con el almacenamiento en la nube de Google Drive de cada usuario.
*   **¿Por qué se usó?** Para habilitar la funcionalidad crítica de respaldo y restauración en la nube (Cloud Sync).
    *   **Flujo OAuth 2.0:** Es el protocolo estándar de la industria para autorización delegada. Permite que el usuario otorgue permisos a nuestra app para leer/escribir en su Drive sin que nuestra app jamás conozca su contraseña. El usuario se autentica de forma segura en una pantalla de Google, y Google nos devuelve un *token de acceso temporal* que usamos para firmar nuestras peticiones.
    *   **Respaldo SQLite:** Para el respaldo, la aplicación empaqueta los archivos físicos que componen la base de datos Room (`.db`, `.db-wal` [Write-Ahead Log] y `.db-shm`) y los sube a un espacio oculto y seguro en Drive llamado `appDataFolder`, garantizando que los datos no se pierdan si el usuario cambia o pierde su dispositivo.

### 7. WorkManager / AlarmManager (Notificaciones)
*   **¿Qué son?** Son las APIs oficiales de Android para programar y garantizar la ejecución de tareas en segundo plano (Background Tasks).
*   **¿Por qué se usaron?** El sistema operativo Android es extremadamente estricto al matar procesos en segundo plano para ahorrar batería de forma agresiva (Doze Mode).
    *   **AlarmManager** se utilizó para programar recordatorios exactos en el tiempo (por ejemplo, "Lanzar una notificación hoy a las 15:00 hrs"). Despierta el dispositivo en el momento exacto requerido.
    *   **WorkManager** se empleó para tareas diferidas que deben ejecutarse obligatoriamente de forma asíncrona, incluso si la app se cierra o el dispositivo se reinicia (ideal para lanzar rutinas de limpieza o sincronizaciones secundarias respetando las restricciones del sistema).

---

## 📱 Módulos y Funcionalidades

El proyecto "Organizador de Tareas" se compone de los siguientes módulos principales de cara al usuario:

*   **Gestión de Eventos (CRUD completo):** Interfaz para la creación, lectura detallada, actualización y eliminación de tareas.
*   **Filtrado y Clasificación:** Capacidad de organizar, buscar y filtrar tareas por fecha, estado de completado y categoría asignada.
*   **Calendario Interactivo:** Vista mensual y semanal que permite una planificación visual rápida de la agenda del mes.
*   **Geolocalización:** Asignación visual de ubicaciones a las tareas, con mapas ligeros integrados en la interfaz.
*   **Sincronización en la Nube:** Sistema de respaldo manual hacia la cuenta personal de Google Drive del usuario.
*   **Sistema de Alertas Locales:** Notificaciones push programadas para recordar al usuario sobre eventos próximos importantes.

---

## ⚙️ Guía de Instalación y Configuración

### 1. Clonar el repositorio
Abre tu terminal y ejecuta:
```bash
git clone https://github.com/USUARIO/organizador-de-tareas.git
cd organizador-de-tareas
```
*(Posteriormente, abre la carpeta raíz del proyecto utilizando **Android Studio**).*

### 2. Configuración de la API Key de Google Maps
Para que los módulos de geolocalización y renderizado de mapas funcionen y no muestren una pantalla en blanco:
1. Dirígete a la [Google Cloud Console](https://console.cloud.google.com/).
2. Crea un proyecto y habilita la API **Maps SDK for Android**.
3. Genera una credencial de tipo *API Key*.
4. En la raíz de tu proyecto local en Android Studio, busca o crea un archivo llamado `local.properties`.
5. Añade la siguiente línea con tu clave (este archivo es ignorado por Git por seguridad):
   ```properties
   MAPS_API_KEY=TU_API_KEY_AQUI
   ```

### 3. Configuración de Google Drive y SHA-1
El módulo de sincronización en la nube utiliza el flujo de autenticación OAuth 2.0. Si intentas correr la app en tu emulador o dispositivo físico sin realizar este paso, **la autenticación fallará silenciosamente** o será rechazada por Google. Esto se debe a que Google verifica criptográficamente que la app esté siendo compilada por un desarrollador autorizado.

1. **Obtener la huella SHA-1 de Debug de tu máquina local:**
   * En Android Studio, abre el panel lateral de **Gradle** (usualmente a la derecha).
   * Navega a `TU_PROYECTO -> Tasks -> android -> signingReport` y haz doble clic.
   * Abre la pestaña de *Run* en la parte inferior y copia la huella que dice `SHA1`.
2. **Registrar la huella en Google Cloud:**
   * Regresa a tu proyecto en Google Cloud Console.
   * Ve a **APIs y Servicios -> Credenciales**.
   * Crea una nueva credencial de tipo **ID de cliente de OAuth**.
   * Selecciona **Android** como tipo de aplicación.
   * Ingresa el **Nombre del paquete** exacto del proyecto (encuéntralo en `build.gradle` a nivel de módulo o en el `AndroidManifest.xml`).
   * Pega la huella **SHA-1** que copiaste en el paso anterior y guarda.
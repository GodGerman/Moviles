# App Organizador de Tareas

## 1. Descripción del Proyecto
[cite_start]Desarrollo de una aplicación móvil nativa orientada a la gestión y control de tareas (organizador)[cite: 3]. [cite_start]El proyecto se desarrollará en Android Studio utilizando Kotlin[cite: 3]. [cite_start]La persistencia de datos se gestionará de forma local mediante SQLite[cite: 152].

---

## 2. Fase 1: Configuración Inicial del Proyecto

### 2.1. Creación del Proyecto en Android Studio
1. Configurar el lenguaje a **Kotlin** y el SDK mínimo compatible (recomendado API 24+).

### 2.2. Configuración de Dependencias (build.gradle)
Es necesario integrar las siguientes librerías para cumplir con los requerimientos:
* [cite_start]**Room Database:** Para abstraer y facilitar el manejo de SQLite[cite: 152].
* [cite_start]**Navigation Component:** Para gestionar los fragmentos del menú hamburguesa y la Bottom Navigation Bar[cite: 12].
* [cite_start]**Google Play Services (Maps & Location):** Para seleccionar y guardar la ubicación de los eventos[cite: 38].
* [cite_start]**API de Contactos de Android:** Para leer los contactos del dispositivo[cite: 37].
* [cite_start]**Google Drive API / Dropbox SDK:** Para el módulo de respaldo y restauración en la nube[cite: 142, 143].
* [cite_start]**WorkManager / AlarmManager:** Para programar las notificaciones locales[cite: 148].

---

## 3. Fase 2: Arquitectura de la Base de Datos (SQLite)

[cite_start]La aplicación guardará la información de forma local usando SQLite[cite: 152]. Se recomienda implementar la arquitectura MVVM (Model-View-ViewModel) junto con Room.

### 3.1. Entidad Principal (`EventEntity`)
[cite_start]Crear una tabla que almacene los siguientes atributos[cite: 36, 39]:
* `id` (Primary Key, Autoincremental)
* [cite_start]`categoria` (String/Enum): Cita, Junta, Entrega de proyecto, Examen, Otros[cite: 14, 15, 16, 17, 18, 19].
* [cite_start]`fecha` (String/Long): Formato dd/mm/aaaa[cite: 26, 39].
* [cite_start]`hora` (String): Formato HH:mm[cite: 27, 39].
* [cite_start]`descripcion` (String)[cite: 28, 39].
* [cite_start]`estatus` (String): Pendiente, Realizado, Aplazado[cite: 39].
* [cite_start]`ubicacion_lat` y `ubicacion_lng` (Double): Coordenadas del mapa[cite: 38].
* [cite_start]`contacto_uri` o `contacto_nombre` (String): Nombre seleccionado del celular[cite: 37].
* [cite_start]`recordatorio_tipo` (Int): 0 (Sin recordatorio), 1 (A la hora), 2 (10 min antes), 3 (1 día antes)[cite: 40].

### 3.2. DAO (Data Access Object)
Definir las consultas SQLite necesarias:
* `insertEvent(event)`
* `updateEvent(event)`
* `deleteEvent(event)`
* [cite_start]Consultas con filtros para el módulo de búsqueda: por fecha exacta, por rango, por mes, por año y por categoría[cite: 43, 44, 45, 46, 47].

---

## 4. Fase 3: Diseño de Interfaz y Navegación (UI/UX)

### 4.1. Navigation Drawer (Menú Lateral)
[cite_start]Implementar el menú desplegable lateral [cite: 4] con texto e íconos para las siguientes rutas:
1. [cite_start]Añadir Eventos [cite: 5]
2. [cite_start]Consultar y modificación de Eventos [cite: 6]
3. [cite_start]Mostrar Calendario [cite: 7]
4. [cite_start]Realizar Respaldo en Dropbox o google drive [cite: 8]
5. [cite_start]Restaurar datos de Dropbox o Google drive [cite: 9]
6. [cite_start]Acerca de.. [cite: 10]
7. [cite_start]Salir [cite: 11]

### 4.2. Bottom Navigation Bar
[cite_start]Agregar una barra inferior global con texto e íconos para las opciones principales: Inicio, Consultar y Salir[cite: 12].

---

## 5. Fase 4: Desarrollo de Módulos (Paso a Paso)

### 5.1. Módulo: Inicio (Home)
* [cite_start]**Objetivo:** Pantalla principal al abrir la aplicación[cite: 150].
* [cite_start]**Acción:** Recuperar de SQLite los eventos del día actual y de los próximos 4 días[cite: 151].
* **UI:** Mostrar los resultados en un `RecyclerView` (lista). [cite_start]Cada tarjeta (item) debe mostrar: fecha, categoría, status, ubicación y persona[cite: 151].

### 5.2. Módulo: Añadir Eventos
* **Objetivo:** Formulario de captura de datos.
* **Componentes clave:**
    * [cite_start]Selector (`Spinner` o botones) para la Categoría y el Status inicial[cite: 14, 29].
    * [cite_start]`DatePickerDialog` y `TimePickerDialog` para capturar la fecha y la hora[cite: 25, 27].
    * **Integración de Contactos:** Un botón que dispare un `Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)`. [cite_start]Al seleccionar un contacto, recuperar su nombre y mostrarlo en el formulario[cite: 37].
    * **Integración de Mapas:** Un botón que abra una actividad con un `SupportMapFragment`. [cite_start]El usuario coloca un marcador, y al confirmar, se guardan las coordenadas y se muestra la ubicación en el formulario[cite: 38].
    * [cite_start]Selector para el tipo de notificación (10 min antes, 1 día antes, etc.)[cite: 40].
* [cite_start]**Acción Final:** Botón "Guardar" que inserta el objeto en la base de datos a través de Room[cite: 34].

### 5.3. Módulo: Consulta y Modificación de Eventos
* **Objetivo:** Búsqueda avanzada y operaciones CRUD.
* [cite_start]**Filtros UI:** Implementar selectores para filtrar por Rango, Año, Día, Mes y Categoría[cite: 51, 52, 53].
* [cite_start]**Visualización:** Tabla o `RecyclerView` adaptable que muestre los resultados de la consulta SQLite[cite: 48].
* **Interacción en los resultados:**
    * [cite_start]Incluir opciones en cada evento para abrir el contacto y ver el mapa guardado[cite: 48].
    * [cite_start]Al tocar un evento, permitir actualizar el estatus (Pendiente, Realizado, Aplazado)[cite: 115].
    * [cite_start]Permitir modificar el contacto y la ubicación[cite: 116].
    * [cite_start]Incluir un botón/icono para eliminar el evento permanentemente de SQLite[cite: 117].

### 5.4. Módulo: Mostrar Calendario
* [cite_start]**UI:** Implementar un `CalendarView` nativo o una librería de calendario de terceros[cite: 119].
* [cite_start]**Lógica:** Consultar la base de datos para obtener las fechas con eventos registrados y dibujar un distintivo (ej. un punto de color) en esos días[cite: 120].
* [cite_start]**Interacción:** Al hacer clic en un día con marca, abrir un `DialogFragment` o navegar a un nuevo `Fragment` mostrando el detalle de los eventos de ese día[cite: 120].

---

## 6. Fase 5: Notificaciones (Servicios en Segundo Plano)

* [cite_start]**Requisito:** El usuario no ve esto como un menú, opera en el "background"[cite: 147].
* [cite_start]**Implementación:** Al momento de guardar un evento (Paso 5.2), leer la opción de recordatorio[cite: 148].
* **Lógica:** * Calcular el tiempo exacto (ej. Timestamp del evento menos 10 minutos).
    * Configurar un `AlarmManager` o un `WorkManager` (recomendado) para que dispare un `BroadcastReceiver` en ese momento exacto.
    * [cite_start]El `BroadcastReceiver` construirá y lanzará una `NotificationCompat` mostrando el título y descripción de la tarea al usuario[cite: 148].

---

## 7. Fase 6: Respaldo y Restauración en la Nube

* [cite_start]**Objetivo:** Exportar la base de datos SQLite local a Google Drive o Dropbox[cite: 143].
* **Lógica de Respaldo:**
    1. Autenticar al usuario con la API seleccionada (ej. Google Sign-In para Drive).
    2. Localizar el archivo físico de la base de datos SQLite en el almacenamiento interno de la app (generalmente en `/data/data/com.tu.paquete/databases/`).
    3. [cite_start]Subir este archivo (`.db`) a la carpeta de aplicación del usuario en la nube[cite: 143].
* **Lógica de Restauración:**
    1. [cite_start]Descargar el archivo `.db` desde la nube[cite: 144].
    2. Sobrescribir el archivo de la base de datos actual en el dispositivo local.
    3. [cite_start]Reiniciar la instancia de Room/SQLite para que los cambios surtan efecto en la interfaz[cite: 144].
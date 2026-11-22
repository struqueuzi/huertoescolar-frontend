# Huerto escolar · guía de desarrollo y defensa

## El problema que resuelve el proyecto

En el taller se repartían los trabajos verbalmente. Para averiguar si se regó un bancal, el docente debía preguntar a cada participante. En la aplicación puede abrir la tarea, ver quién la recibió, cuándo fue entregada, qué evidencia adjuntó el estudiante y qué decidió quien la revisó. Todo el recorrido se puede ensayar en un solo teléfono alternando cuentas ficticias.

## Perfiles y responsabilidades

| Acción | Docente o tallerista | Estudiante |
|---|---|---|
| Pantalla inicial | Trabajos de todo el taller, cola de revisión y sensores | Trabajos propios, sus sectores relacionados y sus reportes |
| Sectores y cultivos | Crea, consulta y edita. Puede eliminar un sector vacío | Lee los sectores y cultivos vinculados a sus asignaciones |
| Tareas | Crea, edita y reasigna mientras están pendientes | Consulta y ejecuta las asignadas a su cuenta |
| Entrega | Lee las entregas, fotos y revisiones anteriores | Inicia, comenta, adjunta una foto, entrega y corrige si se rechaza |
| Revisión | Aprueba o rechaza. El rechazo requiere comentario | Consulta resultado y motivo |
| Problemas | Registra problemas y agrega seguimientos con estado | Registra problemas en sus sectores y consulta sus reportes |
| Datos de prueba | Consulta lecturas ficticias y alertas | Consulta sus avisos de trabajo |

Los permisos se aplican dos veces. `Garden.forUser` limita las colecciones que llegan a las pantallas. Por separado, `Repository` consulta el usuario y la tarea en la base antes de cada escritura y comprueba rol, asignación y estado. Ocultar un botón solo resuelve la parte visual.

## Dónde encontrar cada pieza

Todas las rutas Kotlin parten de `app/src/main/java/cl/huerto/`.

| Archivo o carpeta | Responsabilidad y punto de entrada |
|---|---|
| `MainActivity.kt` | Abre Compose con `setContent`. `App` observa el estado, alterna login y navegación, instala el NavHost y controla destinos por rol. La sesión en memoria termina al morir el proceso. |
| `ui/HuertoViewModel.kt` | `login`, `logout`, `start`, `save` y `deleteSector` reciben intenciones. `runAction` controla la operación en curso y traduce errores para la UI. Los flujos Room se combinan en `ScreenState`. |
| `domain/Rules.kt` | Catálogos de estados, tipos y roles. `validate` devuelve errores por nombre de campo. `overdue` compara la fecha programada y el estado. Se puede probar con JUnit sin teléfono. |
| `data/Models.kt` | Clases de datos que Room convierte en tablas. Cada entidad tiene un ID. Los identificadores relacionados son claves foráneas indexadas. |
| `data/HuertoDb.kt` | Define la base y su DAO. Métodos `observe...` emiten listas actualizadas. Métodos `get...` consultan una fila para decidir operaciones. `latest` selecciona la entrega más reciente. |
| `data/Repository.kt` | Reglas que requieren información guardada. `start` cambia a En desarrollo. `save` delega según el formulario. `seed` crea ejemplos una sola vez. Las operaciones compuestas usan transacciones Room. |
| `data/DemoEvidence.kt` | Dibuja una ilustración identificada como ficticia para el reporte y la entrega inicial. |
| `ui/Screens.kt` | Inicio, sectores, detalle de sector, detalle de cultivo, tareas, cola de revisión, detalle de tarea, reportes, detalle de reporte, historial, avisos y sensores. Las pantallas reciben datos autorizados y eventos de navegación. |
| `ui/Forms.kt` | `Editor` arma siete formularios reutilizando campos y selectores. `initial` carga los valores al editar. Guardar envía el borrador al ViewModel. |
| `ui/Components.kt` | `Page`, `Line`, `Field`, `Choice`, `Status` y `Empty` comparten presentación y retroalimentación. |
| `ui/Photos.kt` | `PhotoField` gestiona cámara y selector. `Preview` renderiza con Coil. La copia a almacenamiento privado se hace en un hilo de E/S. |
| `ui/theme/Theme.kt` | Tema Material 3, tipografía y colores semánticos. `statusColor` asigna el tono conservando una etiqueta de texto para que el color no sea la única señal. |
| `app/src/main/res/` | `drawable/brote.xml` es el ícono. `xml/paths.xml` delimita la carpeta temporal que puede compartir el proveedor de archivos con la cámara. |
| `AndroidManifest.xml` | Declara la actividad de entrada y el FileProvider sin exportar. La app delega la captura a una aplicación de cámara. |
| `app/src/test/` | `RulesTest` comprueba validaciones. `RepositoryTest` usa Robolectric y Room para recorrer rechazo, corrección, aprobación, permisos y persistencia. |
| Configuración Gradle | `settings.gradle.kts` declara repositorios y módulo. El archivo raíz fija versiones. `app/build.gradle.kts` configura SDK, Kotlin, Compose, Room, Coil y pruebas. El Wrapper fija Gradle. |

## Cómo explicar el flujo desde un botón hasta SQLite

1. Luna abre **Mis tareas**, selecciona Riego y pulsa **Iniciar tarea**. El detalle envía el ID al ViewModel.
2. El ViewModel lanza una corrutina. El repositorio lee la tarea y la cuenta activa, verifica que Luna sea la responsable y que el estado permita iniciar.
3. En la misma transacción actualiza la tarea e inserta un evento de historial. Si falla una escritura, ambas se revierten.
4. Room notifica a los observadores. `combine` reúne los cambios y el ViewModel publica otro estado de pantalla. Compose vuelve a dibujar el detalle con el botón **Adjuntar y entregar**.
5. Al entregar se crea una **Evidencia**. La tarea queda pendiente de validación. El docente revisa esa evidencia concreta. Si la rechaza, Luna puede iniciar la corrección y enviar una segunda evidencia, conservando la primera.
6. La fecha de la evidencia representa la realización declarada por el estudiante. La fecha de validación registra la decisión independiente del docente. Una tarea se muestra atrasada cuando pasó su fecha y queda trabajo por hacer. Atrasada se calcula al consultar para evitar inconsistencias con el paso de los días.

## Modelo relacional

| Tabla | Dependencias y motivo |
|---|---|
| Usuario | Cuenta ficticia, rol, grupo y estado activo. |
| Sector | Responsable → Usuario. Nombre, ubicación y fechas describen el sector. |
| Cultivo | Sector → Sector. Guarda la planificación de siembra, trasplante, riego y cosecha. |
| Tarea | Cultivo → Cultivo. Responsable y autor → Usuario. El sector se obtiene desde el cultivo, así se evita asignar una tarea a una combinación contradictoria. |
| Evidencia | Tarea → Tarea, autor → Usuario. Hay una fila por entrega, incluso al reenviar. La foto se guarda como archivo y la tabla conserva la ruta. |
| Validacion | Evidencia → Evidencia, autor → Usuario. Guarda decisión, fecha y comentario sobre esa versión del trabajo. |
| Evento | Tarea → Tarea, autor → Usuario. Registra asignación inicial y pasos del flujo por fecha. |
| ReporteProblema | Sector → Sector, cultivo opcional → Cultivo, autor → Usuario. Permite describir problemas que afectan un bancal completo. Si se selecciona cultivo, el repositorio verifica su sector. |
| Seguimiento | Reporte → ReporteProblema, autor → Usuario. Cada actualización de atención agrega un comentario fechado. |
| Lectura | Sector → Sector. Guarda humedad de suelo, temperatura, humedad ambiental, fecha y estado de una muestra ficticia. |

El diseño separa grupos repetidos y hechos sobre distintas entidades. Los nombres de sector y de responsable no se copian en cada tarea. Las referencias usan claves. El estado actual de tareas y reportes se conserva además de sus eventos para simplificar consultas. Las transacciones mantienen ambos registros coherentes. La eliminación física solo se ofrece en sectores sin relaciones. Así se preservan las evidencias de la evaluación.

## Correspondencia con el checklist y demostración

| Bloque | Qué ejecutar y observar |
|---|---|
| 1. Usuarios y permisos | Iniciar con docente, cerrar e ingresar con luna. Comparar el panel, buscar las tareas de nico y comprobar que no aparecen. En el repositorio, explicar `admin` y `owner`. |
| 2. Vistas | Recorrer login → inicio → sectores → detalle → formulario → cultivo → tareas → detalle → entrega → revisión → reportes → historial. Filtrar tareas por estado, fecha y estudiante. |
| 3. Datos y arquitectura | Mostrar una entidad, su clave foránea, el DAO observable y la escritura en transacción. Cerrar y reabrir la app tras guardar una entrega, volver a iniciar sesión y abrir la foto anterior. |
| 4. Validaciones | Guardar cultivo con cosecha anterior a la siembra y riego cero. Mostrar ambos errores, corregir y guardar. Rechazar una actividad sin comentario también debe marcar el campo. Campos de texto tienen límites y las opciones provienen de catálogos. |
| 5. Recursos nativos | Seleccionar imagen, comprobar vista previa, cancelar otro intento y comprobar que sigue la primera foto. Repetir con cámara. La app copia los bytes a archivos privados para mantener acceso tras reiniciar. |
| 6. UX/UI | Probar rotación, formulario largo y tablet. La columna se desplaza y limita su ancho a 720 dp. Mostrar aparición de errores con AnimatedVisibility, cambio de estado con Crossfade y expansión de la vista previa con animateContentSize. El ícono brote identifica la app instalada. |
| 7. Negocio | Registrar sector, cultivo y tarea. Entregar, rechazar con motivo, corregir y aprobar. Registrar un problema, darle seguimiento y abrir el historial. Mostrar avisos con fechas y sensores con la etiqueta de simulación. |
| 8. Colaboración y entrega | Mostrar historial técnico y README. El tablero, las contribuciones personales, nombres de ambos, accesos del docente y envío por AVA requieren evidencia real del equipo. |
| Bonus | Revisar el ícono verde con brote, en el manifiesto y en el lanzador Android. |

## Cómo se aplicaron los seis PPT

1. **De Kotlin a Android.** Una Activity abre la interfaz declarativa. Los eventos cambian datos observables.
2. **Compose.** Columnas, tarjetas, textos, botones y formularios se componen en funciones reutilizables.
3. **Navigation.** NavHost tiene destinos y argumentos de ID. Un detalle recibe el ID y consulta la colección ya autorizada. Al salir de sesión se reemplaza la navegación.
4. **Validaciones.** Las reglas devuelven un mapa de errores. La interfaz muestra indicadores específicos junto a los campos.
5. **Persistencia.** Entidades, DAO y base Room sostienen el repositorio. El ViewModel expone StateFlow.
6. **Recursos.** Los contratos de actividad abren el selector y la cámara. Los resultados llegan a una vista previa. Se conserva una copia privada y se manejan cancelaciones.

## Defensa individual de 15 minutos

| Minuto | Punto oficial y acción |
|---|---|
| 0–1 | **Entorno.** Ejecutar el proyecto, indicar SDK, JDK y dispositivo, entrar como docente. |
| 1–3 | **Arquitectura.** Abrir Models, Repository y ViewModel. Explicar dónde se impide validar una tarea propia. |
| 3–4 | **Diseño.** Comparar los dos paneles. Explicar jerarquía, navegación, tarjetas y legibilidad. |
| 4–5:30 | **Formularios.** Ingresar fecha imposible y explicar el mensaje por campo. |
| 5:30–7 | **Estado.** Iniciar un trabajo. Seguir el camino desde el botón hasta el nuevo estado de Compose. |
| 7–8 | **Animaciones.** Provocar error, adjuntar foto y cambiar el estado del trabajo. Explicar la función de cada transición. |
| 8–9:30 | **Persistencia.** Mostrar el DAO, cerrar y volver a abrir. Comprobar que la actividad y la evidencia siguen disponibles. |
| 9:30–11 | **Recursos nativos.** Usar cámara o selector, explicar URI temporal, FileProvider y copia privada. Mostrar resultado renderizado. |
| 11–14:30 | **Cambio en vivo.** Practicar una nueva regla, un ajuste de UI y un Composable pequeño. Ejecutar y probar un valor válido y otro inválido. |
| 14:30–15 | Cierre y preguntas. Preparar el equipo y las cuentas antes de empezar. |

**Ejercicio para practicar.** Cambiar la frecuencia máxima de riego de 365 a 90 días en `Rules.validate`, actualizar el texto del error y agregar una prueba para 90 y otra para 91. Abrir el formulario y mostrar ambos casos. Para practicar un cambio visual, añadir bajo el estado de una tarea un texto explicativo visible únicamente cuando queda pendiente de validación. Para un campo persistente, identificar entidad, migración Room, formulario, repositorio y lectura del detalle. Cambiar solo el texto de la pantalla no guarda un dato nuevo.

**Preguntas que conviene poder responder.** ¿Por qué Validacion apunta a Evidencia? Permite identificar exactamente qué entrega se evaluó. ¿Qué pasa si el docente pulsa dos veces? La operación bloquea reenvíos en curso y el repositorio vuelve a comprobar el estado dentro de una transacción. ¿Qué ocurre si se cancela la cámara? Se elimina el temporal y se conserva la selección anterior. ¿Qué cambia al usar otro teléfono? El almacenamiento es local a cada instalación. ¿Quién controla el riego? El usuario. El calendario y las lecturas de prueba solo informan.

**Entrega y participación.** La EP2 exige proyecto completo, repositorio público con acceso para docente y equipo, commits técnicos distribuidos, planificación en Trello o similar, envío del mismo código a AVA grupal por un integrante y activación individual. Revisar el plazo oficial antes de cambiar el remoto: la pauta sanciona modificaciones posteriores a la entrega y previas a la evaluación. Para EP3 cada integrante debe poder ejecutar y modificar el proyecto sin IA ni materiales de apoyo durante su defensa. Los commits de implementación asistida documentan cambios técnicos. La participación de ambos se acredita con trabajo real de cada integrante.

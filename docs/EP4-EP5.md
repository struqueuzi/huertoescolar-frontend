# Huerto escolar · Experiencia 3

## Qué problema resuelve esta versión

El docente necesita conocer el material compartido y coordinar jornadas del taller. Cada teléfono conserva las tareas y evidencias de la experiencia anterior. Inventario y agenda se consultan en dos servicios del colegio. Al cambiar de dispositivo se recupera el catálogo remoto usando los mismos servicios. Las tareas, las fotos y las cuentas de Room siguen siendo propias de cada instalación.

## Responsabilidades y flujo de código

| Componente | Función y decisión que se puede explicar |
|---|---|
| MainActivity y Home | Registran los destinos. Inventario y jornadas son visibles para el docente. El clima está disponible para ambos perfiles. |
| remote/Services.kt | Declara DTO, módulos y contratos Retrofit. Las anotaciones GET, POST, PUT y DELETE indican el verbo y la ruta. registro valida entrada; Retrofit y Gson convierten campos Kotlin a JSON. |
| remote/TallerScreen.kt | TallerViewModel conserva filas, indicador de operación y mensajes en StateFlow. La vista envía una acción al ViewModel. Tras una escritura se consulta de nuevo al servidor. El botón de edición carga el registro y reutiliza el formulario de alta. Hay confirmación antes de eliminar. |
| WeatherScreen y ClimaApi | Consultan Open-Meteo por HTTPS en coordenadas fijas de San Bernardo, muestran temperatura, precipitación y fecha de la observación. El indicador conserva la fuente. No escribe en Room ni en H2. |
| Backend inventario | Insumo tiene ID, nombre y cantidad entera mayor o igual a cero. El controlador valida el cuerpo y envía la operación a JpaRepository. H2 guarda los cambios en disco. |
| Backend agenda | Jornada tiene ID, nombre y fecha ISO. Los contratos son independientes. Cada proceso usa su puerto y su base. |
| Seguridad de demostración | La sesión móvil controla las pantallas. Spring exige sus propias credenciales al recibir cada solicitud. Solo se permite HTTP de laboratorio en direcciones locales autorizadas. La clave se ingresa al consultar y permanece en memoria. |

Ejemplo de una operación completa: al guardar un insumo nuevo se valida nombre y cantidad; Retrofit envía POST /api/insumos; Spring valida otra vez; JpaRepository inserta la fila; H2 asigna un ID; HTTP devuelve 201; el ViewModel recupera la colección y Compose muestra la tarjeta. La edición usa PUT /api/insumos/{id}. La eliminación usa DELETE y espera 204.

## Instalación de laboratorio

1. Preparar JDK 17, Android SDK 35, Build Tools 34.0.0, Android Studio y Maven 3.9. Abrir la carpeta Android que contiene settings.gradle.kts.
2. Arrancar los servicios en dos terminales siguiendo el README del backend. Usar una clave de demostración elegida por el equipo en HUERTO_API_CLAVE.
3. Conectar el emulador o dispositivo. Ejecutar adb devices y los dos comandos adb reverse del README. Repetirlos si se desconecta el dispositivo.
4. Abrir Huerto escolar. Entrar como docente con Huerto123, abrir Inventario compartido e ingresar la clave del servicio. Crear un material, editar su cantidad, reiniciar el servicio desde el mismo directorio y consultarlo. Eliminar el registro de ensayo. Repetir el CRUD con una jornada.
5. Abrir Tiempo en San Bernardo con acceso a internet. Entrar como luna para mostrar los permisos distintos y la continuidad del recorrido local de tareas. Ver la guía anterior para los demás casos.
6. Para ensayar desconexión, detener un servicio y pulsar Consultar servidor. Comprobar el mensaje de error y reintentar después de iniciar el proceso.

## Pruebas y cobertura

Ejecutar ./gradlew :app:testDebugUnitTest :app:coberturaLogica :app:verificarCobertura. JaCoCo genera HTML y XML bajo app/build/reports/jacoco/coberturaLogica/. El umbral es 80 % de líneas en reglas, repositorio, estado autorizado, ViewModels y lógica de campos remotos. Abrir el informe y revisar líneas rojas, incluso si el porcentaje supera la meta. La guía docente usa Kover para explicar la misma distinción entre cobertura y calidad. La prueba de navegación usa Robolectric; las pruebas de repositorio crean una base de ensayo. Las de reglas usan entradas explícitas, sin bases ni servicios. El verificador de contratos del backend crea y elimina datos propios en ambos servicios y también comprueba 401, 400 y 404.

## Firma académica y comprobación

El proyecto EP4 tiene versionCode 2 y versionName 2.0. En Linux, indicar HUERTO_JKS con la ruta absoluta al JKS suministrado de forma separada y leer la clave sin imprimirla, por ejemplo con read -rs HUERTO_CLAVE; export HUERTO_CLAVE. El alias de laboratorio es huerto. Ejecutar ./gradlew :app:assembleRelease. El APK se genera bajo app/build/outputs/apk/release/. Verificar con apksigner verify --verbose --print-certs y luego adb install -r sobre un dispositivo. Una instalación previa con otra firma requiere un dispositivo distinto o retirar la instalación anterior, lo que elimina sus datos locales. Conservar la clave para firmar las próximas versiones. No subir contraseñas al repositorio público.

## Defensa EP5 · 15 minutos

| Tiempo | Demostración individual |
|---|---|
| 0–2 | Iniciar aplicación y servicios, explicar JDK, SDK, puertos y conexión por adb. Mostrar los paneles del docente y del estudiante. |
| 2–3 | Abrir ViewModel, DTO, controlador y entidad. Explicar Room para cada teléfono y H2 para información compartida. |
| 3–5 | Crear un insumo y editar una jornada desde la app. Revisar el JSON y el ID en el servicio. |
| 5–6 | Consultar clima. Explicar ubicación fija, GET HTTPS y manejo del error externo. |
| 6–8 | Reiniciar el servicio y recuperar el registro. Eliminar únicamente el registro preparado para la demo. |
| 8–10 | Ejecutar pruebas, explicar entradas/límites y mostrar cobertura obtenida. Distinguir pruebas puras, integración con base real y navegación simulada. |
| 10–11 | Mostrar APK, certificado y verificación. Explicar alias, clave, versión e instalación. |
| 11–12 | Mostrar ramas, commits y tablero real de ambos integrantes, y explicar el aporte personal. |
| 12–15 | Practicar una modificación: fijar cantidad máxima de 500, aplicar la regla al cliente y al servidor, agregar pruebas de 500 y 501 y ejecutar. |

La evaluación individual pide ejecutar y modificar sin asistencia durante la defensa. El equipo debe completar nombres, planificación y contribuciones reales, verificar el material en el dispositivo del laboratorio y entregar en AVA grupal e individual según la pauta.

## Evidencia obtenida

La suite JVM de reglas, ViewModels y repositorio ejecutó 18 pruebas con éxito. JaCoCo 0.8.12 registró 223 de 229 líneas (97,38 %) y 206 de 282 ramas (73,05 %) en las clases seleccionadas de lógica. La medición usó el compilador Kotlin/Compose y JUnitCore; el XML y la salida de pruebas están en docs/verificacion. El denominador incluye Repository, HuertoViewModel, Garden, TallerViewModel, ServicesKt y domain. El informe excluye pantallas Compose, DTO, SQL generado y servicios Java. El doble FakeDao permite probar reglas y eventos de estado sin dispositivo. Los tests RepositoryTest ejercitan por separado la base Room. Los porcentajes de cobertura de líneas y de ramas responden a preguntas distintas.

El cliente Retrofit real completó CRUD contra ambos procesos Spring. Se verificaron también respuestas HTTP de autenticación, validación y registro inexistente. Inventario recuperó el mismo registro desde H2 después de detener y reiniciar la JVM.

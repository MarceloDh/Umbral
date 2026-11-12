# Umbral | Aplicaciones móviles para IoT

Sumativa Unidad 2 (TI3042). Una misma aplicación Android se instala en dos teléfonos. El **Operador** solicita la apertura y monitorea la puerta. El **Simulador** responde, genera eventos de presencia y cierra la puerta cuatro segundos después. El estado que muestra el Operador proviene del Simulador. La demostración utiliza una puerta simulada.

Umbral aborda la supervisión de un acceso cercano cuando el operador necesita saber si su orden llegó y si la puerta volvió a quedar asegurada. Los dos dispositivos intercambian órdenes, estados y eventos de presencia mediante Bluetooth clásico. La cuenta y el historial de cada dispositivo se guardan en su propia base SQLite.

Consulta el [mockup de la entrega](docs/mockup.md) para ver la navegación y el contenido de las pantallas. El [avance BLE y NFC](README-mockup.md) conserva el diseño de la primera etapa.

El proyecto incluye la [licencia de la tipografía Poppins](app/src/main/assets/licenses/Poppins-OFL.txt).

## Abrir y compilar en Android Studio

1. Descomprime el ZIP. En Android Studio elige **Open** y abre la carpeta **Umbral** que contiene `settings.gradle.kts`.
2. Sincroniza Gradle. El proyecto conserva Gradle 8.13 y los plugins de la versión original. Asegúrate de que Android Studio tenga instalado Android SDK Platform 36 y pueda descargar las dependencias. El proyecto usa Java/JVM 17.
3. En la terminal de Android Studio en Windows, ejecuta `.\gradlew.bat :app:assembleDebug`. En Linux o macOS, usa `bash ./gradlew :app:assembleDebug`.
4. La APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. Para revisar la interfaz y la cuenta local, selecciona un emulador y pulsa **Run**. Para la demostración de Bluetooth, instala la **misma versión** en ambos teléfonos físicos con Android 8.0 o superior.
5. Si Android muestra un conflicto de firma con una instalación anterior, conserva el historial que necesites antes de desinstalarla. Desinstalar borra los datos locales de esa instalación.

## Demo paso a paso

1. En ambos teléfonos, activa Bluetooth y vincúlalos entre sí desde Ajustes de Android. Si hace falta, deja abierta la pantalla de ajustes Bluetooth de uno mientras lo buscas desde el otro.
2. Abre Umbral. Crea un usuario y una contraseña de al menos ocho caracteres en cada equipo. Las cuentas son locales e independientes. Cierra sesión en uno y prueba una contraseña incorrecta y luego la correcta.
3. En el teléfono B elige **Simulador**. Pulsa **Actualizar equipos vinculados**, concede **Dispositivos cercanos** si Android lo solicita y selecciona el teléfono A. Pulsa **Iniciar espera**.
4. En el teléfono A elige **Operador**, actualiza los equipos, selecciona B y pulsa **Conectar** antes de que venza la espera de B (45 segundos).
5. Comprueba que ambos indiquen la conexión y que A muestre **Puerta asegurada**. En A pulsa **Solicitar apertura**. Ambos deben mostrar **Puerta abierta** y, cuatro segundos después, **Puerta asegurada**. En A el historial debe mostrar la confirmación de apertura y cierre con el mismo número.
6. En B pulsa **Simular presencia**. En A comprueba el aviso y el registro en **Historial**. La puerta debe seguir asegurada.
7. Solicita otra apertura. Desconecta el enlace durante esos cuatro segundos. A debe mostrar **Estado sin confirmar** y B debe completar el cierre. Reconecta y comprueba que A recibe el estado asegurado.
8. Cierra sesión, vuelve a entrar y muestra que los eventos se conservan. Si el enlace falla, revisa que B esté en espera, que cada teléfono haya seleccionado al otro y que ambos tengan Bluetooth y permisos.
9. Prueba tres credenciales erróneas, vuelve a abrir Umbral y comprueba que el bloqueo continúa hasta que vence el minuto. Después inicia sesión con la contraseña correcta.

## Arquitectura para la defensa

- **Pantallas** en Jetpack Compose con Material 3, Scaffold, LazyColumn y el tema del avance. El flujo Login → Panel está en `NavigatorHostController.kt`.
- **ControlUmbral** aplica la sesión, coordina los roles, gestiona el cierre y actualiza la interfaz. El Operador espera hasta cinco segundos por la confirmación de apertura. Si el último estado recibido fue abierto y pasan siete segundos sin confirmación de cierre, invalida el estado y pide reconectar para consultarlo.
- **EnlaceBluetooth** usa RFCOMM seguro de Android. B acepta al equipo vinculado que seleccionó su usuario. A conecta como cliente y consulta el estado inicial. El protocolo intercambia tramas de seis bytes con versión, tipo e identificador. Es una simplificación respecto al JSON propuesto en la formativa. El Simulador rechaza identificadores de apertura repetidos dentro de una conexión y el Operador espera confirmación antes de mostrar una apertura.
- **DatosUmbral** guarda la cuenta y los eventos con SQLite de Android. El historial muestra los 100 últimos eventos. PBKDF2 deriva el verificador con una sal aleatoria y AES-GCM lo protege con una clave de Android Keystore. La contraseña se usa localmente. El bloqueo de un minuto tras tres fallos se guarda en SQLite. Si se interrumpe el proceso con una apertura registrada y pendiente, al iniciar de nuevo la puerta simulada parte asegurada y el historial registra esa recuperación.
- **Elección de Bluetooth**. RFCOMM permite que dos Android vinculados intercambien tramas pequeñas a corta distancia. La conexión es directa y evita depender de un punto de acceso Wi-Fi o de un bróker MQTT. El volumen bajo y la ausencia de escaneo continuo reducen la actividad esperada de radio. El consumo depende de los teléfonos y del tiempo de conexión. Para evaluar estabilidad, prueba a distintas distancias y registra cuántas solicitudes reciben confirmación y cuándo se corta el enlace.
- **Seguridad IoT**. Para relacionar el prototipo con ISO/IEC 27400, muestra la sesión obligatoria, la selección del equipo vinculado, el resguardo de la contraseña, la ausencia de secretos en el historial y el cierre ante pérdida del enlace. El código del radar, PIN y NFC del avance queda como referencia de mockup, fuera del flujo de la sumativa.

## Evidencias y entrega

Anota modelo y versión de Android de A y B. Para cada caso, registra resultado, evidencia y cualquier ajuste aplicado. Usa capturas del login, la conexión, el cambio de estado y el historial. Una grabación breve de ambos teléfonos sirve para mostrar el ciclo completo y el corte del enlace.

Verificación local con Android SDK 36 y emulador Medium Phone API 35. Gradle completó `assembleDebug` y `lintDebug` sin errores de Lint. La prueba de serialización comprobó los seis tipos de trama y el rechazo de versión, tipo e identificadores inválidos y de una trama incompleta.

| Caso | Resultado esperado | Resultado real |
| --- | --- | --- |
| Registro y credenciales erróneas | Crea la cuenta, abre el panel y luego rechaza contraseñas erróneas | Verificado en API 35 |
| Bloqueo persistente | Tras tres errores, reiniciar Umbral conserva el bloqueo hasta completar el minuto | Verificado en API 35. La clave correcta fue aceptada después del bloqueo |
| Conexión y consulta inicial | Ambos se conectan y A recibe el estado inicial | Pendiente |
| Apertura y cierre | A confirma ambos cambios y B cierra en cuatro segundos | Pendiente |
| Presencia | A recibe y registra presencia sin abrir | Pendiente |
| Pérdida de enlace | A muestra estado sin confirmar y B termina el cierre | Pendiente |
| Reinicio del simulador | Si una apertura quedó registrada y pendiente, B inicia asegurado y anota la recuperación | Registro y limpieza de la apertura pendiente verificados en API 35 con datos de prueba. Falta repetirlo durante un enlace Bluetooth |
| Persistencia | El historial sigue disponible tras reabrir la app | Pendiente |
| Permiso rechazado | La app informa el permiso necesario y permite reintentar | Verificado en API 35. El permiso se concedió al reintentar |

Sube el contenido de esta carpeta a la raíz del repositorio de GitHub, incluyendo `.gitignore`, `gradlew`, `gradlew.bat` y `gradle/wrapper/`. Verifica que el docente tenga acceso y pega la URL real del repositorio en el archivo de entrega para AAI. Lleva la APK instalada, los dos teléfonos y las evidencias.

Para la validación con el docente, presenta el problema, recorre el mockup y prueba el registro, un rechazo de contraseña y la navegación al panel. Anota la fecha, la observación, el ajuste aplicado y cómo lo volviste a probar. Registra también las preguntas y los ajustes finales que surjan en la demostración.

Referencia de implementación: [Conectar dispositivos Bluetooth, Android Developers](https://developer.android.com/develop/connectivity/bluetooth/connect-bluetooth-devices).

## Rúbrica de la sumativa

| Criterio | Qué presentar |
| --- | --- |
| 2.1.1.1 Problemática | Explicar el problema de supervisar la puerta e identificar el Operador, el Simulador y los datos que intercambian. |
| 2.1.1.2 Funcionalidades | Mostrar el login, la conexión, el monitoreo, el activador, la presencia y el historial. |
| 2.1.1.3 Herramientas | Justificar Kotlin y Compose por continuidad con el avance. Explicar que SQLite permite consultar datos sin servidor y que las APIs Bluetooth de Android manejan el enlace. |
| 2.1.1.4 Propuesta | Mostrar la formativa y registrar qué observó el docente sobre el alcance de la puerta simulada y los roles. |
| 2.1.2.5 Comunicación | Explicar por qué se eligió RFCOMM. Repetir la apertura cerca y a mayor distancia, anotar respuestas recibidas y cortes. Distinguir el consumo esperado de cualquier medición real. |
| 2.1.2.6 Mockup | Complementar las capturas de la formativa con Login, Conexión, Monitoreo e Historial. Mostrar la ruta Login → Panel → Historial en el mismo panel. |
| 2.1.2.7 Validación de diseño | Presentar al docente la interfaz y el login. Registrar la observación y el ajuste que se hizo, si corresponde. |
| 2.1.3.8 Implementación | Mostrar la app en Android Studio y explicar los archivos de interfaz, control, conexión y almacenamiento. |
| 2.1.3.9 Permisos | Denegar y conceder Dispositivos cercanos en Android 12 o posterior. Probar que la lista de equipos y la conexión se recuperan. |
| 2.1.3.10 Seguridad IoT | Explicar el verificador de contraseña protegido, la selección del par autorizado y el enlace cifrado. |
| 2.1.3.11 Pruebas | Completar la tabla de resultados reales e indicar qué error se corrigió y cómo se volvió a probar. |
| 2.1.4.12 Demo | Ejecutar apertura, cierre, presencia e historial entre los dos equipos. |
| 2.1.4.13 Defensa técnica | Explicar por qué el Simulador confirma el estado, cómo se identifica cada orden y por qué se usa SQLite. |
| 2.1.4.14 Evidencia de seguridad | Mostrar el rechazo de un login erróneo, la pérdida de enlace y un historial sin credenciales. |
| 2.1.4.15 Retroalimentación final | Responder preguntas y registrar cualquier ajuste pedido por el docente. |

### Continuidad con la formativa

Se mantienen el problema de control de acceso, los roles Operador y Simulador, Bluetooth clásico, la confirmación remota, el cierre a cuatro segundos y la presencia simulada. Se incorpora SQLite para concretar el almacenamiento y el login pedidos por la sumativa. El protocolo usa tramas cortas y la sesión de usuario se valida en cada teléfono. El Simulador autoriza órdenes del par Bluetooth seleccionado mientras su propia sesión esté activa. El alcance se limita a dos Android y a una puerta simulada.

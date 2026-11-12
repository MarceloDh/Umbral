# Umbral — Panel de acceso con BLE + NFC

Este documento describe el diseño inicial. Consulta el [mockup actual](docs/mockup.md) para ver las pantallas de Umbral.

App Android en Kotlin + Jetpack Compose para controlar una puerta:

- **Radar BLE**: detecta teléfonos/beacons cercanos y estima la distancia por RSSI.
- **Código de acceso**: PIN de 4 dígitos, guardado como hash SHA-256.
- **NFC**: vincula una tarjeta y abre la puerta al apoyarla.
- **Historial**: accesos concedidos, denegados y presencias detectadas.
- **Ajustes**: cierre automático, radio de detección y vigilancia.

## Abrir el proyecto

1. Android Studio → **Open** → seleccionar esta carpeta.
2. Esperar el *Gradle Sync* (ya incluye wrapper 8.13, AGP 8.13, Kotlin 2.0.21).
3. Ejecutar en un dispositivo con Android 8.0 (API 26) o superior.

También se puede compilar por consola:

```powershell
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

## Código inicial

```
2468
```

Se puede cambiar en **Ajustes → Código de acceso**.

## Permisos

- Android 12+: `BLUETOOTH_SCAN` y `BLUETOOTH_CONNECT` (sin ubicación).
- Android 11 o anterior: `ACCESS_FINE_LOCATION` (requisito del sistema para BLE).
- NFC no requiere permiso en tiempo de ejecución.

## Limitaciones reales del hardware

- **BLE detecta dispositivos que emiten**, no personas. Para identificar personas
  hay que repartir beacons/pulseras o sumar un sensor IoT (PIR, mmWave) en la puerta.
- **El emulador no tiene Bluetooth LE ni NFC**: el radar mostrará un aviso y el
  lector NFC dirá que el equipo no tiene hardware. Código, ajustes e historial
  funcionan igual.
- El UID de una tarjeta NFC es público y se puede copiar; en producción conviene
  autenticación criptográfica (MIFARE DESFire, NDEF firmado).

## Estructura

```
app/src/main/java/com/fossati/umbral/
├── MainActivity.kt
├── NavigatorHostController.kt     # NavegacionApp + rutas
├── Screens/                       # PantallaInicio, Codigo, Nfc, Radar, Historial, Ajustes
├── theme/                         # Poppins, paleta obsidiana/latón, formas
├── model/                         # data class, sealed class, extensiones
├── data/                          # SharedPreferences (PIN hash, NFC, historial)
├── ble/                           # EscanerBluetooth (BLE + RSSI)
├── nfc/                           # LeerNfc (reader mode)
├── ui/componentes/                # ModuloPuerta, Radar, Teclado, tarjetas
├── util/                          # permisos
└── viewmodel/                     # PorteroViewModel
```

## Nota de diseño

Tipografía **Poppins** empaquetada en `res/font` (no depende de Google Fonts en
runtime). Paleta oscura "obsidiana y latón", animaciones con `AnimatedContent`,
`animateFloatAsState` e `InfiniteTransition`, y fondo con halos propios.

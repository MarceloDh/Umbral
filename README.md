# Umbral

App Android para monitorear y operar una puerta simulada por Bluetooth. Un teléfono funciona como **Operador** y solicita la apertura. El otro funciona como **Portero**, confirma el estado y simula el cierre. Cada equipo guarda sus eventos en SQLite.

[Descargar APK de demostración](https://github.com/MarceloDh/Umbral/releases/download/apk-demo/Umbral-debug.apk)

## Cómo probarla

1. Instala la APK en dos teléfonos con Android 8.0 o posterior y vincúlalos por Bluetooth.
2. En uno elige **Completar demo Portero** e inicia sesión. Configura un PIN en **Herramientas de la estación → Ajustes**. Regresa al panel, selecciona al Operador y pulsa **Iniciar espera**.
3. En el otro elige **Completar demo Operador**, selecciona al Portero, ingresa el PIN y pulsa **Conectar**. Prueba la apertura, el cierre automático y el historial.

En el emulador se comprobó el inicio de sesión y su registro en el historial. Las pruebas automáticas cubren el PIN correcto, el PIN incorrecto y el rechazo de una firma de un desafío anterior. El ciclo completo por Bluetooth y la lectura de NFC y BLE requieren pruebas con equipos físicos.

## Compilar

Abre el proyecto en Android Studio con el SDK 36 y Java 17. En Windows ejecuta `.\gradlew.bat assembleDebug`. En macOS o Linux ejecuta `./gradlew assembleDebug`. La APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Guía y capturas

[Guía de Marcelo Ducommun](docs/Gu%C3%ADa%20-%20Marcelo%20Ducommun%20Umbral.docx) · [Mockup y pantallas](docs/mockup.md)

<img src="docs/login-actual.png" alt="Inicio de sesión" width="210">
<img src="docs/operador-actual.png" alt="Panel del Operador e historial" width="210">

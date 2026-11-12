# Umbral

Umbral es una app Android para monitorear y operar una puerta simulada mediante Bluetooth. Un teléfono actúa como **Operador** y solicita la apertura. El otro actúa como **Simulador**, confirma el estado, cierra la puerta automáticamente y genera eventos de presencia. Cada teléfono guarda su cuenta y su historial de eventos de forma local.

## Pruebas de la app

En el emulador Android 15 (API 35) se comprobó el registro, el inicio de sesión, el rechazo de contraseñas incorrectas, el bloqueo después de tres intentos incluso al reiniciar la app y la solicitud del permiso Bluetooth tras rechazarlo. La app también recuperó un registro de apertura pendiente en SQLite con datos de prueba.

<img src="docs/registro.png" alt="Registro de una cuenta local en Umbral" width="230">
<img src="docs/panel.png" alt="Panel del Operador con estado sin confirmar e historial local" width="230">

El panel muestra **Estado sin confirmar** mientras no hay conexión. Queda pendiente comprobar la apertura, el cierre automático y los eventos de presencia de extremo a extremo entre dos teléfonos vinculados.

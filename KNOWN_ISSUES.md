# Errores Conocidos y Soluciones del Proyecto OpenRadio

## [2026-09-11] Error: Fallo de compilación en DatabaseTest por discrepancia de constructor
- **Síntoma**: `e: .../DatabaseTest.kt:27:32 No value passed for parameter 'playbackHistoryDao'`.
- **Causa raíz**: Se había extendido `RadioRepository` para aceptar `playbackHistoryDao` en turnos anteriores, pero no se había actualizado el test unitario en `DatabaseTest.kt`.
- **Solución aplicada**: Se añadió `db.playbackHistoryDao()` en la instanciación de `RadioRepository` en `DatabaseTest.kt`.
- **Prevención**: Ejecutar siempre el task de unit tests `:app:testDebugUnitTest` como parte del ciclo de verificación.

## [2026-09-11] Error: Timeout en prueba de MediaController bajo Robolectric
- **Síntoma**: `java.util.concurrent.TimeoutException at PlaybackTest.kt:25` al esperar `controllerFuture.get(10, TimeUnit.SECONDS)`.
- **Causa raíz**: `MediaController.buildAsync()` requiere un dispatch de eventos IPC que en entorno JVM simulado con Robolectric no avanza sincrónicamente a través de `bindService`.
- **Solución aplicada**: Se refactorizó `PlaybackTest.kt` para evaluar el ciclo de vida del servicio (`onCreate`, `onDestroy`) directamente usando `Robolectric.buildService(PlaybackService::class.java)`.
- **Prevención**: Usar el controlador de servicios de Robolectric para tests unitarios locales en JVM y dejar el binding asíncrono para tests en runtime o mocks.

# Progreso del Proyecto OpenRadio

## [2026-09-11] Funcionalidad: Fullscreen Player Overlay Nativo Edge-to-Edge
Estado: Completo y probado
Descripción: Migración del reproductor fullscreen desde un `Dialog` a un overlay de Compose gestionado por estado con `zIndex(10f)` en la jerarquía raíz de `RadioApp.kt`. Elimina la franja gris/blanca del status bar en Lenovo/ZUI y Android 15, garantizando edge-to-edge auténtico.
Archivos involucrados:
- `app/src/main/java/com/example/ui/RadioApp.kt`
- `app/src/main/java/com/example/ui/components/Dialogs.kt`

## [2026-09-11] Funcionalidad: Robustez de Streaming y Audio Buffer
Estado: Completo y probado
Descripción: Configuración de `DefaultLoadControl` con duraciones de buffer adaptadas a streaming de radio en vivo (2s para inicio, 4s rebuffer, 30s max), timeouts HTTP explícitos (8s conexión, 15s lectura) con soporte de redirecciones cross-protocol y User-Agent en `PlaybackService.kt`. Reintento automático ante fallas de socket/conexión transitorias en `RadioViewModel.kt`.
Archivos involucrados:
- `app/src/main/java/com/example/service/PlaybackService.kt`
- `app/src/main/java/com/example/ui/RadioViewModel.kt`

## [2026-09-11] Funcionalidad: UI Responsiva y Textos con Marquee
Estado: Completo y probado
Descripción: Implementación de `basicMarquee` en títulos largos de emisoras y canciones tanto en el reproductor a pantalla completa como en el mini-dashboard de reproducción. Soporte responsivo de 2 columnas para orientación horizontal / pantallas grandes en `FullPlayerScreen`.
Archivos involucrados:
- `app/src/main/java/com/example/ui/components/Dialogs.kt`
- `app/src/main/java/com/example/ui/components/PlaybackDashboard.kt`

## [2026-09-11] Funcionalidad: Automatización CI/CD con GitHub Actions
Estado: Completo y probado
Descripción: Pipeline en `.github/workflows/build-apk.yml` que ejecuta los tests unitarios (`gradle :app:testDebugUnitTest`), genera el keystore debug, compila el APK (`assembleDebug`), sube el artefacto temporal y publica automáticamente una Release en GitHub con el archivo APK descargable.
Archivos involucrados:
- `.github/workflows/build-apk.yml`

# Errores Conocidos y Soluciones del Proyecto OpenRadio

## [2026-09-11] Error: Fallo en tests de Robolectric en CI (UnsupportedOperationException en DefaultSdkProvider)
- **Síntoma**: En GitHub Actions `:app:testDebugUnitTest` fallaba con `ExampleRobolectricTest > classMethod FAILED: java.lang.UnsupportedOperationException at DefaultSdkProvider.java:170` y `GreetingScreenshotTest > classMethod FAILED`.
- **Causa raíz**: Ambos archivos de test tenían configurado `@Config(sdk = [36])`. La versión de Robolectric en el proyecto no soporta Android API 36, arrojando excepción al no encontrar el runtime del SDK.
- **Solución aplicada**: Se ajustó la configuración a `@Config(sdk = [34])` en `ExampleRobolectricTest.kt` y `GreetingScreenshotTest.kt`.
- **Prevención**: Configurar siempre niveles de SDK estables y probados en anotaciones `@Config` de Robolectric (máximo API 34 o 35).

## [2026-09-11] Error: Incompatibilidad de actualización de APK ("No son compatibles")
- **Síntoma**: Al descargar un APK nuevo e intentar actualizarlo sobre la versión ya instalada, Android muestra un error de incompatibilidad o conflicto de paquetes, obligando a desinstalar la versión anterior.
- **Causa raíz**: En GitHub Actions (`build-apk.yml`), el comando `keytool -genkey` generaba un nuevo par de claves criptográficas (`debug.keystore`) aleatorio y efímero en cada ejecución del workflow. El sistema operativo Android (PackageManager) exige por seguridad que toda actualización esté firmada exactamente con el mismo certificado digital.
- **Solución aplicada**: Se modificó `build-apk.yml` para restaurar persistentemente el almacén de claves oficial del repositorio (`debug.keystore.base64`) mediante `base64 -d`. De este modo, todos los APKs generados comparten la misma firma digital SHA-256.
- **Prevención**: No generar claves nuevas en runners efímeros de CI/CD; reutilizar siempre el keystore versionado o almacenado en secrets.

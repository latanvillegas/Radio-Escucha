# Errores Conocidos y Soluciones del Proyecto OpenRadio

## [2026-09-11] Error: Fallo de compilación en DatabaseTest por discrepancia de constructor
- **Síntoma**: `e: .../DatabaseTest.kt:27:32 No value passed for parameter 'playbackHistoryDao'`.
- **Causa raíz**: Se había extendido `RadioRepository` para aceptar `playbackHistoryDao` en turnos anteriores, pero no se había actualizado el test unitario en `DatabaseTest.kt`.
- **Solución aplicada**: Se añadió `db.playbackHistoryDao()` en la instanciación de `RadioRepository` en `DatabaseTest.kt`.
- **Prevención**: Ejecutar siempre el task de unit tests `:app:testDebugUnitTest` como parte del ciclo de verificación.

## [2026-09-11] Error: Incompatibilidad de actualización de APK ("No son compatibles")
- **Síntoma**: Al descargar un APK nuevo e intentar actualizarlo sobre la versión ya instalada, Android muestra un error de incompatibilidad o conflicto de paquetes, obligando a desinstalar la versión anterior.
- **Causa raíz**: En GitHub Actions (`build-apk.yml`), el comando `keytool -genkey` generaba un nuevo par de claves criptográficas (`debug.keystore`) aleatorio y efímero en cada ejecución del workflow. El sistema operativo Android (PackageManager) exige por seguridad que toda actualización esté firmada exactamente con el mismo certificado digital.
- **Solución aplicada**: Se modificó `build-apk.yml` para restaurar persistentemente el almacén de claves oficial del repositorio (`debug.keystore.base64`) mediante `base64 -d`. De este modo, todos los APKs generados comparten la misma firma digital SHA-256.
- **Prevención**: No generar claves nuevas en runners efímeros de CI/CD; reutilizar siempre el keystore versionado o almacenado en secrets.

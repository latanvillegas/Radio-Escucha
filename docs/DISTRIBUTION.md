# Distribución de OpenRadio

El paquete Android de producción es `com.aistudio.radiosenvivo.pxtrkz`. No lo cambies después de la primera publicación: hacerlo rompe las actualizaciones instaladas.

## Firma permanente (una sola vez)

Genera y conserva **fuera de Git** un keystore de producción. Nunca publiques la contraseña, el archivo `.jks` ni su contenido Base64:

```bash
keytool -genkeypair -v -keystore openradio-release.jks -alias openradio -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 openradio-release.jks
```

En GitHub > Settings > Secrets and variables > Actions, crea los siguientes **repository secrets**:

- `ANDROID_KEYSTORE_BASE64`: contenido Base64 de `openradio-release.jks` sin saltos de línea.
- `ANDROID_KEYSTORE_PASSWORD`: contraseña del keystore.
- `ANDROID_KEY_ALIAS`: alias elegido (`openradio` en el ejemplo).
- `ANDROID_KEY_PASSWORD`: contraseña de la clave.

Guarda una copia de seguridad cifrada del keystore. Si se pierde, no podrás distribuir actualizaciones compatibles con los APK ya instalados con esa firma. El archivo de depuración anterior está firmado con una clave distinta; antes de desinstalarlo, exporta o respalda sus datos.

## Lanzamiento

1. Confirma que `main` supera los tests de GitHub Actions.
2. Agrega capturas reales en `fastlane/metadata/android/en-US/images/phoneScreenshots/` y `fastlane/metadata/android/es-ES/images/phoneScreenshots/`. Agrega `icon.png` (48 a 512 píxeles) extraído del icono definitivo de la app. No uses capturas simuladas.
3. Verifica que el archivo `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` existe antes de crear la etiqueta.
4. Crea una etiqueta de versión, por ejemplo:

```bash
git checkout main
git pull --ff-only origin main
git tag v1.0.0
git push origin v1.0.0
```

El workflow solamente crea un GitHub Release si los tests pasan y los cuatro secretos de firma están configurados. Construye con `assembleRelease`, verifica la firma con `apksigner`, y publica un `OpenRadio-v1.0.0.apk` junto con su `SHA-256`. Los APK debug se entregan únicamente como artefactos efímeros de CI.

### Regla de versiones

Etiqueta `vMAJOR.MINOR.PATCH`, cada segmento entre 0 y 999 y MAJOR mayor que 0. `versionCode = MAJOR * 1_000_000 + MINOR * 1_000 + PATCH`. Usa siempre versiones crecientes. Ejemplo: `v1.0.0 = 1000000`, `v1.0.1 = 1000001`, `v1.1.0 = 1001000`.

## Tiendas y gestores

- **Orion Store:** envía una solicitud usando el cliente o el repositorio [Orion-Data](https://github.com/RookieEnough/Orion-Data/issues). Adjunta el enlace al GitHub Release firmado, el repositorio fuente, la licencia y el identificador de paquete.
- **IzzyOnDroid:** comprueba la [política de inclusión](https://izzyondroid.org/docs/general/AppInclusionPolicy/) y solicita el alta mediante el sistema de incidencias de su repositorio. Requiere APK Release firmado, Fastlane, imágenes y licencia libre; no publiques solo el APK debug.
- **F-Droid:** pide una revisión del código y las dependencias libres siguiendo la [guía oficial](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/). La inclusión no es automática.
- **Obtainium:** los usuarios pueden añadir directamente `https://github.com/latanvillegas/Radio-Escucha` como fuente de nuevas versiones.

## Revisión previa a la inclusión

- Revisa la autoría y licencias de iconos, imágenes y demás recursos incluidos. El directorio de radios, los logotipos externos y las señales de terceros no se licencian por la licencia del código.
- El `AndroidManifest.xml` permite HTTP claro porque ciertas fuentes de radio y el servicio `ip-api.com` usan HTTP. Valora restringir este acceso o sustituir servicios HTTP antes de solicitar la inclusión en tiendas estrictas. No elimines la opción global sin validar la reproducción de fuentes HTTP.
- Prueba instalación nueva, actualización, reproducción en segundo plano y Android Auto. Confirma privacidad, permisos y ausencia de SDKs publicitarios incompatibles con la tienda.
- La creación de una etiqueta o la preparación de estos metadatos no incorpora automáticamente la app a ningún catálogo externo.

# Distribución de OpenRadio

Esta guía documenta el proceso **actual** del repositorio. El paquete Android de producción es `com.aistudio.radiosenvivo.pxtrkz`; no cambies el `applicationId` ni la clave de firma después del primer lanzamiento.

- APK oficial más reciente: https://github.com/latanvillegas/Radio-Escucha/releases/latest
- Automatización: [GitHub Actions](../.github/workflows/build-apk.yml)
- Metadatos Fastlane: [`fastlane/metadata/android/`](../fastlane/metadata/android/)
- Auditoría: [`SECURITY_PRIVACY_AUDIT.md`](SECURITY_PRIVACY_AUDIT.md)

## Compilación local

Requisitos: JDK 17 y Android SDK API 35. Desde un clon nuevo del repositorio, usa el Gradle Wrapper **8.9** versionado junto al proyecto:

```bash
git clone https://github.com/latanvillegas/Radio-Escucha.git
cd Radio-Escucha
./gradlew :app:assembleDebug --no-daemon
./gradlew :app:testDebugUnitTest --no-daemon
```

En Windows ejecuta `gradlew.bat` en lugar de `./gradlew`. El APK Debug está en `app/build/outputs/apk/debug/app-debug.apk`. Un APK Debug no sustituye a una versión Release firmada.

## Firma permanente

Los **cuatro secretos ya utilizados por el workflow de producción** son:

| Secreto de GitHub Actions | Valor |
| --- | --- |
| `KEYSTORE_BASE64` | Contenido Base64 del keystore privado de producción |
| `STORE_PASSWORD` | Contraseña del keystore |
| `KEY_ALIAS` | Alias de la clave de firma |
| `KEY_PASSWORD` | Contraseña de la clave privada |

Configúralos en **Settings → Secrets and variables → Actions**. El workflow transforma los valores en `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y `ANDROID_KEY_PASSWORD` al compilar.

**No** publiques el fichero `.jks`, el Base64 ni las contraseñas. Mantén una copia de seguridad cifrada del keystore original: perder la clave puede impedir actualizaciones compatibles con las instalaciones existentes. Los APK Debug previamente instalados pueden estar firmados con otra clave; respalda los datos antes de cambiarlos por Release.

## Preparar una versión nueva

1. Confirma que el último commit de `main` pasó la auditoría de dependencias, tests, compilación Debug y compilación Release minificada.
2. Comprueba el icono en `fastlane/metadata/android/en-US/images/icon.png` y `es-ES/images/icon.png`, y las cinco capturas reales en `fastlane/metadata/android/es-ES/images/phoneScreenshots/1.jpg` a `5.jpg`.
3. Crea los changelogs `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` y `es-ES/changelogs/<versionCode>.txt`, según corresponda.
4. Publica una **etiqueta nueva** `vMAJOR.MINOR.PATCH` (sin modificar etiquetas ya existentes) o crea una rama `release/vMAJOR.MINOR.PATCH` desde el commit aprobado. Ejemplo:

```bash
git switch main
git pull --ff-only origin main
git tag v1.0.3
git push origin v1.0.3
```

**Nota:** el ejemplo requiere que el changelog de `v1.0.3` exista; no ejecutes esos comandos sin preparar la siguiente versión. Las versiones `v1.0.0`, `v1.0.1` y `v1.0.2` ya se publicaron y no deben volver a etiquetarse.

El workflow genera `OpenRadio-vX.Y.Z.apk`, valida su firma con `apksigner` y publica el archivo `.sha256` en GitHub Releases. La versión `vMAJOR.MINOR.PATCH` se traduce a `versionCode = MAJOR * 1000000 + MINOR * 1000 + PATCH` (cada componente entre 0 y 999, y MAJOR mayor que cero). Mantén el número estrictamente creciente.

## Recursos de tienda

- El icono oficial se genera desde el vector del launcher (PNG de 512 × 512).
- Las capturas originales se preparan para la relación 2:1 sin recortar contenido; el directorio en español contiene cinco imágenes de 768 × 1536.
- La presencia de imágenes, iconos y metadatos no garantiza que una tienda apruebe la aplicación.

Consulta [la preparación de recursos](STORE_ASSETS_AND_RELEASE.md) y [la guía de capturas](STORE_SCREENSHOTS.md).

## Seguimiento de tiendas

| Canal | Estado de referencia |
| --- | --- |
| GitHub Releases | APK Release firmado disponible |
| Orion Store | [Incidencia oficial generada desde la app #3187](https://github.com/RookieEnough/Orion-Data/issues/3187); evaluación pendiente |
| IzzyOnDroid | [Solicitud #930](https://codeberg.org/IzzyOnDroid/repodata/issues/930); evaluación pendiente |
| F-Droid | No incorporado: requiere auditoría de licencias, fuentes, dependencias y receta de compilación |
| Obtainium | Permite seguir Releases de https://github.com/latanvillegas/Radio-Escucha |

La incidencia antigua de Orion Store `#3159` se rechazó por **formato de solicitud** y fue reemplazada por `#3187`, generado desde su propia aplicación.

## Seguridad, privacidad y compatibilidad

- La detección inicial del país usa la configuración regional del sistema; la geolocalización basada en IP solo se consulta si el usuario la solicita.
- Las API y servicios auxiliares identificados utilizan HTTPS.
- **No se ha prohibido HTTP globalmente**: ciertas radios de terceros y URLs agregadas manualmente solo admiten HTTP. Android permite esos streams para no interrumpir la reproducción.
- Valida en teléfonos reales la reproducción HTTP/HTTPS/HLS, las redirecciones, el segundo plano, Android Auto, la biblioteca y las actualizaciones firmadas.
- Revisa por separado las licencias de dependencias, imágenes, marcas de emisoras y recursos de terceros; la licencia MIT cubre el código original de OpenRadio.
- La aceptación en IzzyOnDroid y F-Droid está condicionada también a las políticas de procedencia de código, compilación y transparencia del proyecto.

Más detalles: [Privacidad](PRIVACY.md) y [Auditoría técnica](SECURITY_PRIVACY_AUDIT.md).

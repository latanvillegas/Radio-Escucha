# OpenRadio — capturas oficiales (Fastlane es-ES)

## Orden de la ficha

Las capturas de pantalla reales proporcionadas por el desarrollador se publican con nombres numéricos, sin marcos publicitarios ni cambios en el contenido:

| Archivo | Sección | Imagen suministrada |
|---|---|---|
| `1.jpg` | Inicio | `1000102179.jpg` |
| `2.jpg` | Descubrir | `1000102175.jpg` |
| `3.jpg` | Reproductor | `1000102177.jpg` |
| `4.jpg` | Biblioteca | `1000102178.jpg` |
| `5.jpg` | Ajustes | `1000102176.jpg` |

Las capturas originales del desarrollador miden **691 × 1536 píxeles**. El pipeline prepara copias finales **768 × 1536** añadiendo márgenes laterales, sin recortar contenido, para cumplir la relación máxima de 2:1 recomendada por IzzyOnDroid. Los archivos de publicación se almacenan en:

```
fastlane/metadata/android/es-ES/images/phoneScreenshots/1.jpg
fastlane/metadata/android/es-ES/images/phoneScreenshots/2.jpg
fastlane/metadata/android/es-ES/images/phoneScreenshots/3.jpg
fastlane/metadata/android/es-ES/images/phoneScreenshots/4.jpg
fastlane/metadata/android/es-ES/images/phoneScreenshots/5.jpg
```

### Cómo incorporar los binarios al repositorio

1. Descomprimir el paquete `OpenRadio-Fastlane-5-capturas.zip` con las cinco fotografías originales, sus sumas SHA-256 y el manifiesto.
2. Abrir [la carpeta de capturas de Fastlane](https://github.com/latanvillegas/Radio-Escucha/tree/main/fastlane/metadata/android/es-ES/images/phoneScreenshots) en GitHub.
3. Pulsar **Add file → Upload files**, seleccionar los cinco JPG (no el ZIP) y confirmar **Commit changes** en `main`. Los originales deben conservarse en el historial; las versiones procesadas pueden tener hashes diferentes debido al relleno y a la recompresión.
4. Verificar que los cinco JPG son navegables en GitHub. No eliminar la carpeta al subir el contenido.
5. Confirmar que el workflow de preparación de imágenes ha generado el icono y normalizado las cinco imágenes antes de publicar la versión **v1.0.1**. **No modificar el tag `v1.0.0` ya publicado**.

Las capturas en español deben permanecer en `es-ES`. Para el directorio `en-US`, tomar capturas con la interfaz traducida al inglés, si existe. No duplicar capturas en español simulando localización.

## Control visual de calidad

- En la captura **Biblioteca**, el texto de estado vacío tiene bajo contraste contra el tema oscuro. Conviene corregirlo antes de solicitar inclusión en catálogos con revisión de accesibilidad.
- Hay títulos y opciones que se cortan visualmente por el ancho del teléfono y el tamaño de fuente; valorar un ajuste de la interfaz en una versión futura.
- Las capturas proporcionadas representan el comportamiento real de la aplicación, no una maqueta.

La carpeta se crea inicialmente con `.gitkeep` solo para habilitar la carga web. Esta documentación no significa que los archivos JPG ya estén subidos.

La preparación automática está documentada en [STORE_ASSETS_AND_RELEASE.md](STORE_ASSETS_AND_RELEASE.md). La carpeta existe y contiene las capturas, pero se requiere la finalización del workflow para que los PNG y JPG procesados aparezcan en el mismo commit de publicación.

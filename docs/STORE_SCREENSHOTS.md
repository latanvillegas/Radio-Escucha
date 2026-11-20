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

Cada JPG original mide **691 × 1536 píxeles**. Los archivos deben colocarse en:

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
3. Pulsar **Add file → Upload files**, seleccionar los cinco JPG (no el ZIP) y confirmar **Commit changes** en `main`. Los archivos numéricos deben permanecer idénticos al paquete.
4. Verificar que los cinco JPG son navegables en GitHub. No eliminar la carpeta al subir el contenido.
5. Publicar una actualización posterior (**v1.0.1**) con el changelog `1000001.txt` para que las tiendas que leen el árbol asociado a una versión puedan recoger estas imágenes. **No modificar el tag `v1.0.0` ya publicado**.

Las capturas en español deben permanecer en `es-ES`. Para el directorio `en-US`, tomar capturas con la interfaz traducida al inglés, si existe. No duplicar capturas en español simulando localización.

## Control visual de calidad

- En la captura **Biblioteca**, el texto de estado vacío tiene bajo contraste contra el tema oscuro. Conviene corregirlo antes de solicitar inclusión en catálogos con revisión de accesibilidad.
- Hay títulos y opciones que se cortan visualmente por el ancho del teléfono y el tamaño de fuente; valorar un ajuste de la interfaz en una versión futura.
- Las capturas proporcionadas representan el comportamiento real de la aplicación, no una maqueta.

La carpeta se crea inicialmente con `.gitkeep` solo para habilitar la carga web. Esta documentación no significa que los archivos JPG ya estén subidos.

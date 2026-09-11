# Decisiones Técnicas del Proyecto OpenRadio

## [2026-09-11] Decisión: Reproductor a Pantalla Completa como Overlay en Árbol Principal (vs Dialog / NavHost)
- **Contexto**: `Dialog` en Jetpack Compose crea una sub-ventana (`android.view.Window`) secundaria gestionada por el WindowManager de Android. En capas de personalización de fabricantes (como Lenovo ZUI en tablets) y en Android 15, los Dialogs imponen restricciones en flags de Edge-to-Edge y color de Status/Navigation bar, produciendo franjas grises no removibles.
- **Alternativas consideradas**:
  1. `Dialog` con `DialogWindowProvider`: Frágil ante capas de personalización de fabricantes.
  2. Destino en `NavHost`: Añade complejidad de backstack innecesaria para un reproductor que mantiene estado en vivo y se colapsa/expande con frecuencia.
  3. Overlay composable condicional con `zIndex(10f)` en la jerarquía raíz: Solución elegida.
- **Razón**: Dibuja directamente en la ventana única principal de la Activity, respetando `enableEdgeToEdge()` de forma 100% nativa y sin artefactos de ventana secundaria, manejando el botón atrás mediante `BackHandler`.

## [2026-09-11] Decisión: Calibración de Buffer de ExoPlayer para Radio en Vivo
- **Contexto**: La configuración por defecto de ExoPlayer está optimizada para video bajo demanda (VOD), asignando buffers de hasta 50 segundos y exigiendo hasta 2.5 segundos de datos antes de iniciar.
- **Razón**: Para radio por streaming en vivo, la latencia y el consumo de memoria deben ser mínimos: `minBufferMs = 4_000`, `maxBufferMs = 30_000`, `bufferForPlaybackMs = 2_000`, `bufferForPlaybackAfterRebufferMs = 4_000`. Se prioriza tiempo sobre tamaño de buffer (`prioritizeTimeOverSizeThresholds = true`).

## [2026-09-11] Decisión: Reintento Automático Resiliente en RadioViewModel
- **Contexto**: Las emisoras de radio por internet suelen experimentar microcortes de red, renegociaciones de sockets o caídas temporales de CDN.
- **Razón**: Ante errores tipo `ERROR_CODE_IO_NETWORK_CONNECTION_FAILED` o timeout HTTP, si la red del dispositivo sigue disponible, se intenta una reconexión controlada tras 2.5 segundos en lugar de detener la experiencia abruptamente.

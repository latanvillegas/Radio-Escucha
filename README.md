# 📻 OpenRadio - Plataforma de Radio en Vivo & Streaming Multimedia

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-green.svg)](https://developer.android.com/jetpack/compose)
[![Media3 ExoPlayer](https://img.shields.io/badge/Audio-Jetpack%20Media3%201.5.1-blue.svg)](https://developer.android.com/media/media3)
[![Room DB](https://img.shields.io/badge/Storage-Room%202.6.1-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-brightgreen.svg)](https://developer.android.com/topic/architecture)

**OpenRadio** es una aplicación nativa de Android de alto rendimiento diseñada para la reproducción de estaciones de radio FM/AM y digitales en vivo de Perú, Latinoamérica y el mundo. La aplicación combina una arquitectura **Offline-First**, integración con **Jetpack Media3 (ExoPlayer)**, motor de grabación en vivo, soporte para **Android Auto** y una interfaz inmersiva **Edge-to-Edge** en **Jetpack Compose (Material Design 3)**.

---

## 🏛️ Arquitectura del Sistema

La aplicación sigue los principios de **Clean Architecture** y el patrón **MVVM (Model-View-ViewModel)** recomendado por Google, garantizando reactividad unidireccional (UDF), testabilidad y mantenibilidad.

```
┌────────────────────────────────────────────────────────┐
│               UI Layer (Jetpack Compose M3)            │
│  - Navigation Compose & Type-Safe Screens              │
│  - Full-Screen Immersive Player (Edge-to-Edge)         │
│  - Custom Theme System (AMOLED, Cósmico, Ámbar)        │
└───────────────────────────┬────────────────────────────┘
                            │ Collects StateFlows
                            ▼
┌────────────────────────────────────────────────────────┐
│             ViewModel Layer (RadioViewModel)           │
│  - StateFlow & SharedFlow (UDF)                        │
│  - Search, Filter, Geo-location Logic & Audio Controls │
└───────────────────────────┬────────────────────────────┘
                            │ Requests Data / Commands
                            ▼
┌────────────────────────────────────────────────────────┐
│            Data Layer (Repository Pattern)             │
│  ┌──────────────────────────┬────────────────────────┐ │
│  │     Local Data Source    │   Remote Data Source   │ │
│  │  - Room Persistence DB   │  - RadioBrowser API    │ │
│  │  - DataStore Preferences │  - Custom Stream APIs  │ │
│  │  - Local Stream Recs     │  - IP GeoLocation API  │ │
│  └──────────────────────────┴────────────────────────┘ │
└───────────────────────────┬────────────────────────────┘
                            │ Audio Pipeline
                            ▼
┌────────────────────────────────────────────────────────┐
│      Playback Service Engine (Jetpack Media3)         │
│  - MediaSession & ExoPlayer Foreground Service         │
│  - Audio Focus handling, Notification & Android Auto   │
└────────────────────────────────────────────────────────┘
```

---

## ✨ Características Principales

### 🎧 Reproductor Inmersivo Full-Screen
- **Diseño Edge-to-Edge Total**: Transparencia nativa en la barra de estado y de navegación con soporte para gestos del sistema (`BackHandler`).
- **Visualizador de Audio Activo**: Animaciones reactivas en tiempo real según el estado de reproducción (`Playing`, `Buffering`, `Paused`).
- **Extracción de Color Dinámico**: Paleta adaptativa derivada del arte de la emisora.
- **Detalles del Stream**: Inspección de códec, bitrate y calidad de emisión en tiempo real.

### 🌐 Motor de Agregación & Búsqueda Multi-Fuente
- **RadioBrowser API Integration**: Acceso a un directorio global de más de 30,000 emisoras con filtros por país, género y bitrate.
- **Geolocalización Inteligente**: Detección automática por IP/dispositivo con opción de anulación manual a "Ninguno" o país/región específica.
- **Emisoras Personalizadas**: Permite agregar fuentes de audio directas mediante URLs personalizadas (AAC, MP3, HLS).

### 🎙️ Grabación de Audio en Vivo
- Permite grabar emisiones en vivo directamente al almacenamiento local con un solo toque y gestionar los archivos grabados desde la biblioteca.

### ⏰ Alarma Despertador & Temporizador de Apagado (Sleep Timer)
- **Radio Alarma**: Programa despertadores para iniciar el día escuchando tu estación favorita.
- **Sleep Timer Progresivo**: Temporizador programable con desvanecimiento gradual de volumen para un apagado suave.

### 🚗 Soporte para Android Auto & Reproducción en Segundo Plano
- Integración nativa con `MediaSessionService` para control multimedia desde dispositivos externos, auriculares, smartwatch y pantallas de vehículos con Android Auto.

### 🎨 Personalización & Accesibilidad
- **5 Temas de Color**: Cósmico, AMOLED Puro, Medianoche Azul, Ámbar Cálido y Esmeralda.
- **Tipografía Adaptativa**: Control de escala de fuente e íconos para mejorar la accesibilidad visual.

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología / Librería | Versión |
| :--- | :--- | :--- |
| **Lenguaje** | Kotlin | `2.0.21` |
| **UI Framework** | Jetpack Compose / Material 3 | `1.7.5` / `1.3.1` |
| **Audio Engine** | Jetpack Media3 (ExoPlayer + MediaSession) | `1.5.1` |
| **Base de Datos** | Room Database | `2.6.1` |
| **Preprocesado KSP** | Kotlin Symbol Processing (KSP) | `2.0.21-1.0.25` |
| **Persistencia KV** | Jetpack DataStore Preferences | `1.1.1` |
| **Red & HTTP** | Retrofit + OkHttp3 + Moshi | `2.11.0` / `4.12.0` |
| **Imágenes** | Coil Compose | `2.7.0` |
| **Asincronía** | Kotlin Coroutines & Flow | `1.9.0` |
| **Pruebas** | Robolectric & Roborazzi Screenshot Testing | `4.14.1` / `1.39.0` |

---

## 📂 Estructura del Proyecto

```
app/src/main/java/com/example/
├── data/                      # Capa de Datos (Modelos, API, Room DB)
│   ├── database/              # Room DAOs, Entities y AppDatabase
│   ├── RadioBrowserApi.kt     # Cliente HTTP para directorio de radios
│   ├── MultiSourceRadioApi.kt # Agregador de streams y fuentes secundarias
│   └── DiscoverCacheManager.kt# Manejador de cache offline para explorador
├── service/                   # Servicios en Segundo Plano
│   └── RadioPlaybackService.kt# Media3 MediaSessionService para ExoPlayer
├── ui/                        # Capa de Presentación (Compose & ViewModel)
│   ├── components/            # Componentes reutilizables (Dialogs, Tabs, Cards)
│   ├── theme/                 # Paleta de colores, fuentes, formas M3
│   ├── RadioViewModel.kt      # ViewModel principal de la aplicación
│   └── RadioApp.kt            # Contenedor raíz UI y navegación
└── MainActivity.kt            # Entrypoint de Android & Edge-to-Edge Setup
```

---

## 🚀 Compilación y Ejecución

### Requisitos Previos
- **Android Studio**: Ladybug / Koala o superior.
- **JDK**: Java 17 o Java 21.
- **Android SDK**: `compileSdk = 35`, `minSdk = 24`.

### Pasos de Instalación
1. Clona este repositorio:
   ```bash
   git clone https://github.com/tu-usuario/openradio-android.git
   ```
2. Abre el proyecto en Android Studio.
3. Sincroniza los archivos de Gradle (`Sync Project with Gradle Files`).
4. Ejecuta la aplicación en un dispositivo físico o emulador:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🧪 Pruebas y Control de Calidad

El proyecto incluye pruebas unitarias y de captura visual en JVM sin necesidad de emulador físico:

```bash
# Ejecutar pruebas unitarias de lógica de negocio y ViewModel
gradle :app:testDebugUnitTest

# Verificar captura de pantalla de componentes UI (Roborazzi)
gradle :app:verifyRoborazziDebug

# Grabar nuevas capturas de referencia UI
gradle :app:recordRoborazziDebug
```

---

## 🔒 Privacidad & Permisos
- `INTERNET`: Requerido para la transmisión de streams de audio HTTP/HTTPS.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Mantiene la reproducción activa cuando la pantalla está apagada o la app está en segundo plano.
- `POST_NOTIFICATIONS`: Permite mostrar los controles multimedia interactivos en la barra de estado de Android 13+.
- `RECORD_AUDIO` / `POST_NOTIFICATIONS`: Para la función opcional de grabación en vivo.

---

<p align="center">
  <b>OpenRadio</b> • Desarrollado con prácticas Staff Engineer de alto rendimiento en Android.
</p>

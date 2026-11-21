# Store submission package — OpenRadio v1.0.1

> **Archivo histórico:** este documento conserva el paquete inicial v1.0.1 para trazabilidad; el último Release firmado está en https://github.com/latanvillegas/Radio-Escucha/releases/latest. La incidencia vigente de Orion Store se generó con el cliente oficial y es https://github.com/RookieEnough/Orion-Data/issues/3187. No vuelvas a enviar manualmente el JSON de esta página: la antigua incidencia #3159 tenía un formato no aceptado. IzzyOnDroid sigue el proceso en https://codeberg.org/IzzyOnDroid/repodata/issues/930.



The source code, real screenshots, store icon and signed APK have been published and verified against the same immutable GitHub tag.

## Official release

- Version: `1.0.1`; Android `versionCode`: `1000001`
- Package name: `com.aistudio.radiosenvivo.pxtrkz`
- Source: https://github.com/latanvillegas/Radio-Escucha
- APK: https://github.com/latanvillegas/Radio-Escucha/releases/download/v1.0.1/OpenRadio-v1.0.1.apk
- GitHub release: https://github.com/latanvillegas/Radio-Escucha/releases/tag/v1.0.1
- License: MIT for the original app code, subject to rights of external radio streams/logos
- Minimum Android SDK: 24
- Build: Kotlin, Jetpack Compose, Android Media3; signed release passed `apksigner` verification
- Icons: `fastlane/metadata/android/{es-ES,en-US}/images/icon.png` (512 × 512 PNG)
- Screenshots: `fastlane/metadata/android/es-ES/images/phoneScreenshots/1.jpg` through `5.jpg` (768 × 1536 JPG)

## 1. Orion Store

Official submission tracker: https://github.com/RookieEnough/Orion-Data/issues/new

An attempt to create the issue with the GitHub integration resulted in `403 Resource not accessible by integration`. The app is **not submitted or approved** by Orion Store until a user with permission posts the issue.

**Issue title:**

`App Submission [1 App] — OpenRadio (Android)`

**Issue body:**

### App Submission Request

I would like to submit the following Android app to Orion Store:

```json
[
  {
    "id": "openradio-radio-escucha",
    "name": "OpenRadio",
    "description": "Open-source native Android Internet radio player built with Kotlin, Jetpack Compose and Media3. Discover live stations by country and genre, add custom stations, save favorites, and listen in the background with a sleep timer and Android Auto support.",
    "icon": "https://raw.githubusercontent.com/latanvillegas/Radio-Escucha/v1.0.1/fastlane/metadata/android/en-US/images/icon.png",
    "version": "1.0.1",
    "latestVersion": "1.0.1",
    "downloadUrl": "https://github.com/latanvillegas/Radio-Escucha/releases/download/v1.0.1/OpenRadio-v1.0.1.apk",
    "repoUrl": "https://github.com/latanvillegas/Radio-Escucha",
    "githubRepo": "latanvillegas/Radio-Escucha",
    "releaseKeyword": "OpenRadio-",
    "packageName": "com.aistudio.radiosenvivo.pxtrkz",
    "category": "Music",
    "platform": "Android",
    "author": "latanvillegas",
    "officialSite": "https://github.com/latanvillegas/Radio-Escucha",
    "screenshots": [
      "https://raw.githubusercontent.com/latanvillegas/Radio-Escucha/v1.0.1/fastlane/metadata/android/es-ES/images/phoneScreenshots/1.jpg",
      "https://raw.githubusercontent.com/latanvillegas/Radio-Escucha/v1.0.1/fastlane/metadata/android/es-ES/images/phoneScreenshots/2.jpg",
      "https://raw.githubusercontent.com/latanvillegas/Radio-Escucha/v1.0.1/fastlane/metadata/android/es-ES/images/phoneScreenshots/3.jpg",
      "https://raw.githubusercontent.com/latanvillegas/Radio-Escucha/v1.0.1/fastlane/metadata/android/es-ES/images/phoneScreenshots/4.jpg",
      "https://raw.githubusercontent.com/latanvillegas/Radio-Escucha/v1.0.1/fastlane/metadata/android/es-ES/images/phoneScreenshots/5.jpg"
    ]
  }
]
```

The latest GitHub Release contains a signed production APK and its SHA-256 file. The source repo, MIT license, bilingual Fastlane metadata, launcher-vector-derived 512 × 512 PNG icons, and five 2:1 Spanish screenshots are publicly accessible at the v1.0.1 tag.

## 2. IzzyOnDroid

Inclusion policy: https://izzyondroid.org/docs/general/AppInclusionPolicy/
Issue tracker: https://codeberg.org/IzzyOnDroid/repodata/issues

The official issue tracker is on Codeberg, which is **not writable via the currently connected GitHub integration**. Submit from a Codeberg account and follow the template for a new app inclusion.

**Suggested issue title:**
`New app: OpenRadio (com.aistudio.radiosenvivo.pxtrkz)`

**Suggested issue body:**

> Please consider including OpenRadio, a free/open-source Android Internet radio player.
>
> Source and license: https://github.com/latanvillegas/Radio-Escucha
>
> Latest signed release: https://github.com/latanvillegas/Radio-Escucha/releases/tag/v1.0.1
>
> APK: https://github.com/latanvillegas/Radio-Escucha/releases/download/v1.0.1/OpenRadio-v1.0.1.apk
>
> Package: `com.aistudio.radiosenvivo.pxtrkz`
>
> Version: `1.0.1` (`versionCode 1000001`), minSdk 24.
>
> Metadata: `fastlane/metadata/android/` has `en-US` and `es-ES` descriptions, PNG icons, five JPG screenshots, and changelogs.
>
> The release APK was compiled in GitHub Actions and verified using `apksigner`.
>
> The application contacts third-party station directories and radio streaming hosts. Some streams and the IP geolocation fallback use HTTP; the manifest currently permits cleartext traffic. Please flag any policy, dependency, or permission changes needed before inclusion.

### Caveats for reviewers

- Real-device/on-network testing is still required. This documentation is not a claim that external store maintainers completed their review.
- The `android:usesCleartextTraffic="true"` manifest attribute supports HTTP radio streams and the current IP lookup fallback. Restricting it is a separate compatibility task.
- The screenshots show third-party station logos and content supplied by those broadcasters, not artwork licensed by the original app's MIT license.
- F-Droid official inclusion is a separate review that may require dependency audits and its own reproducible source-build metadata.

## 3. F-Droid

Initial guide: https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/

Do not claim F-Droid inclusion until its maintainers have reviewed sources, dependencies, licensing and build instructions.

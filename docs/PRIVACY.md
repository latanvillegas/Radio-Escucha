# OpenRadio — Privacy and network notice

OpenRadio is an Android internet radio player. It does not intentionally include an advertising or analytics SDK. CI checks for a number of known tracking/proprietary SDK groups in the resolved release dependency graph, but this check is not proof that all dependencies are free software.

- **App startup:** The starting country comes from your device locale. No automatic IP geolocation request is made.
- **Explicit IP country detection:** If you request location by IP, OpenRadio queries ipapi.co or ipwho.is over HTTPS. Those services can see your public IP and network request.
- **Station discovery:** Internet radio directory services such as RadioBrowser, SomaFM, iHeartRadio and FMStream receive network requests.
- **Listening:** The radio station provider sees the connection. Some broadcasts use unencrypted HTTP, especially user-added stations.
- **Cover art and logos:** External image hosts are contacted. If an image fails, HTTPS fallback services wsrv.nl and Google S2 Favicon can receive the logo URL or hostname.
- **Song information:** Apple's iTunes metadata endpoint may be queried to identify or show cover art.
- **Stored data:** Favorites, history, custom stations and theme preferences may be saved locally and included in Android system backups according to device configuration.

OpenRadio does not control third-party service data retention or logging. Internet connectivity is required for most stations.

Source and issue tracking: https://github.com/latanvillegas/Radio-Escucha

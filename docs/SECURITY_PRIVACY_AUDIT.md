# OpenRadio distribution and network audit — 2026-11-20

## Completed changes

- Removed the HTTP-only ip-api.com country fallback. When the user requests IP-based detection, HTTPS ipapi.co is tried first, followed by HTTPS ipwho.is when necessary.
- Application startup uses the Android device locale rather than sending the user's IP to a geolocation service automatically.
- Android Network Security Config explicitly denies cleartext connections to application-controlled radio directory APIs, metadata providers and image fallback domains.
- Radio-station artwork no longer falls back from HTTPS to HTTP. The image placeholder is retained when HTTPS is not available.
- Retrofit/OkHttp API clients reject HTTPS-to-HTTP cross-protocol redirects.
- Unused Firebase, Google Play Services, camera and Gradle secrets-plugin aliases were removed. An unused OkHttp logging interceptor dependency was removed.
- CI now scans the actual releaseRuntimeClasspath and checks key network security invariants. Its dependency tree is uploaded as an artifact.

## Important exceptions and residual risks

1. **HTTP radio streams:** Some station and user-supplied audio URLs do not support TLS. The network-security base-config still enables cleartext globally so these arbitrary hosts keep playing. The domain-specific rules above reduce risk for API calls but do NOT mean the app is HTTPS-only. A future opt-in HTTPS-only playback mode should be tested against real radio catalogues.
2. **Third-party image processing:** The image loader still uses wsrv.nl as a HTTPS image proxy and Google S2 favicon lookup as optional fallbacks. These services may learn which domains or logos are being requested. Review whether these fallbacks should become opt-in for a stricter privacy build.
3. **Metadata and radio providers:** Online directories, streaming hosts and metadata services may observe user IP addresses and request metadata. HTTP streams remain unencrypted. A plain-language explanation is in PRIVACY.md.
4. **Software licences:** Static package group checks do not establish FOSS status for every transitively bundled dependency. Review resolved artifacts and individual SPDX licences before F-Droid submission. MIT only covers original project code; broadcaster names, imagery and streams have separate rights.
5. **Android Auto:** The AndroidManifest metadata named com.google.android.gms.car.application is not itself a Play Services runtime dependency; verify the resolved release graph rather than assuming it is.
6. **Human review and AI policy:** IzzyOnDroid currently restricts LLM-produced application code. This project must be reviewed and declared transparently by its maintainers; CI cannot certify code provenance or guarantee inclusion.
7. **Reproducible builds:** Gradle 8.9 is pinned in the official Gradle Wrapper committed to the source repository. A complete F-Droid build recipe is still missing, and source builds have not been shown to be deterministically reproducible. Perform independent clean-build and APK reproducibility checks before requesting official F-Droid inclusion.
8. **Device testing:** Validate Android 7–15+, HTTP-only stations, HTTPS/HLS streams with redirected segment requests, logo fallbacks, local country selection, explicit IP refresh, manual country override, and Android Auto on real devices.

## Manual CI-equivalent audit

From the repository root:

    python3 -m unittest discover -s scripts/tests -p 'test_*.py'
    ./gradlew :app:dependencies --configuration releaseRuntimeClasspath --no-daemon > release-runtime-dependencies.txt
    python3 scripts/audit_distribution.py --dependencies release-runtime-dependencies.txt
    ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --no-daemon

## Submission tracking

- IzzyOnDroid inclusion: https://codeberg.org/IzzyOnDroid/repodata/issues/930
- Orion Store inclusion: https://github.com/RookieEnough/Orion-Data/issues/3187
- IzzyOnDroid policy: https://izzyondroid.org/docs/general/AppInclusionPolicy/
- F-Droid inclusion policy: https://f-droid.org/docs/Inclusion_Policy/

An issue being open is not evidence of catalogue acceptance.

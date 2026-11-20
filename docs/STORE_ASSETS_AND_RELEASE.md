# Store assets and release checklist

OpenRadio publishes a signed APK from `vMAJOR.MINOR.PATCH` tags or the dedicated `release/vMAJOR.MINOR.PATCH` branch. Do not retag existing published releases.

## Canonical icon

The Fastlane icons for `es-ES` and `en-US` are **generated from the existing** `app/src/main/res/mipmap/ic_launcher.xml` Android vector and saved as `512 × 512` PNG files. Neither manually drawn nor third-party icons are used.

## Screenshots

The original captures uploaded by the developer are kept in the Git history. The Fastlane screenshot files 1.jpg through 5.jpg are in this order:

1. Home / Inicio
2. Discover / Descubrir
3. Player / Reproductor 3D
4. Library / Biblioteca
5. Settings / Ajustes

The original 691 × 1536 captures exceed IzzyOnDroid's recommended 2:1 maximum ratio. `scripts/prepare_store_assets.py` pads each on the left and right, preserving all visible screen content, to **768 × 1536**; it never crops. Existing screenshots with a valid ratio are left unchanged.

`.github/workflows/store-assets.yml` generates these assets after relevant changes to `main` and commits them. The generated artwork must be present in the commit used for an app Release, because F-Droid and IzzyOnDroid use metadata from the same commit/tag as the APK.

## Release v1.0.1

Once assets appear in `main`, create a branch `release/v1.0.1` at the verified `main` commit. GitHub Actions tests, builds and signs an APK, verifies it with `apksigner`, and publishes the release tag `v1.0.1` on success. Do not create the release branch before verifying the image assets.

The app package remains `com.aistudio.radiosenvivo.pxtrkz` and all releases use the same signing key.

## Submission after release

- Orion Store: submit an app listing at https://github.com/RookieEnough/Orion-Data/issues with repository link, signed release APK URL, icon, screenshots, package ID, licence and category.
- IzzyOnDroid: follow https://izzyondroid.org/docs/general/AppInclusionPolicy/ and open a new-inclusion request through its issue tracker. Note that any FOSS/licence review, APK scanners and device testing are performed by maintainers.
- F-Droid: request inclusion separately if all build dependencies and app assets meet its stricter free-software requirements.

No store is guaranteed to accept an application; listing requires a separate review.

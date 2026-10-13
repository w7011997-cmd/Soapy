# Soapy

Android app that helps people quit porn by blocking the words and sites they use when the urge hits.
Working title, may change.

## v0.1 (this version)

- Add and remove blocked words (stored on the device with Room, nothing uploaded)
- Accessibility service watches browser address and search bars
- On a match: sends the user to the home screen and shows a rotating message
- Bundled starter messages in English and Pidgin (works offline)

Not yet built: VPN site blocker, 24-hour removal cooldown, uninstall protection, streak,
urge button, Supabase sync and admin panel.

## Build

Pushing to `main` runs the GitHub Actions workflow in `.github/workflows/build.yml`.
Download the APK from the run's **Artifacts** section (`soapy-debug-apk`).

## Install notes

- Android 13 and newer block accessibility for APKs installed outside the Play Store.
  If the Soapy toggle is greyed out: Settings > Apps > Soapy > three-dot menu >
  **Allow restricted settings**, then turn on the service again.
- Open Soapy, add a word, tap **Turn on**, and enable **Soapy** under Accessibility.

## Project layout

- `app/src/main/java/com/soapy/app/MainActivity.kt` blocklist screen
- `app/src/main/java/com/soapy/app/SoapyAccessibilityService.kt` detection and message overlay
- `app/src/main/java/com/soapy/app/MessageRotator.kt` bundled messages
- `app/src/main/java/com/soapy/app/data/Database.kt` Room database
- `app/src/main/res/xml/accessibility_service_config.xml` which browsers are watched

# Soapy

Android app that helps people quit porn by blocking the words and sites they use when the urge hits.
Working title, may change.

## v0.4

- Privacy Policy and Terms of Use: in the app under Settings > Legal (works offline), and as web pages
  in `docs/` for store listings. Text lives in `tools/legal_content.py`; run
  `python3 tools/make_legal.py .` to regenerate the Kotlin file and the HTML pages.
- To host the pages: GitHub repo > Settings > Pages > deploy from branch `main`, folder `/docs`.
  Links then are `https://w7011997-cmd.github.io/Soapy/privacy.html` and `.../terms.html`.

## v0.3

- Splash shows "By" with "Peter Wisdom K." typing itself out at the bottom
- Settings page ends with a note from Wisdom and links to Discovr, Chatify and Resellr
  (APKPure pages, `apkpure.com/p/<package>`; not published there yet)

## v0.2

- Dark purple theme, splash screen and app icon
- Home screen: protection status, add a word or website, your blocklist
- Words and sites are permanent: once added they cannot be removed (the app asks first)
- Settings page (gear icon, top right)
  - App lock: 4-digit PIN, always with a security question for PIN recovery
  - Uninstall protection: device admin
- Uninstall question: when an uninstall or device-admin deactivate screen for Soapy opens, Soapy
  asks "are you uninstalling me because you want to return to the habit, or because you have quit?"
- Everything runs on the device, so it works offline

Not yet built: VPN site blocker, streak, urge button, Supabase sync and admin panel.

## v0.1

- Accessibility service watches browser address and search bars, sends the user home on a match
  and shows a rotating English or Pidgin message

## Build

Pushing to `main` runs the GitHub Actions workflow in `.github/workflows/build.yml`.
Download the APK from the run's **Artifacts** section (`soapy-debug-apk`).

## Install notes

- Android 13 and newer block accessibility for APKs installed outside the Play Store.
  If the Soapy toggle is greyed out: Settings > Apps > Soapy > three-dot menu >
  **Allow restricted settings**, then turn on the service again.
- Open Soapy, tap **Turn on** under Protection, and enable **Soapy** under Accessibility.
- Turn on **Uninstall protection** to make Soapy harder to remove.

## Project layout

- `MainActivity.kt` splash, lock gate and navigation host
- `SoapyAccessibilityService.kt` word detection, message overlay, uninstall question
- `SoapyDeviceAdminReceiver.kt` device admin (shows the question when deactivating)
- `MessageRotator.kt` bundled messages
- `data/` Room database, PIN and security answer storage (hashed), word and site parser
- `ui/` Compose screens: home, settings, lock, splash, theme
- `res/xml/accessibility_service_config.xml` which apps the service watches

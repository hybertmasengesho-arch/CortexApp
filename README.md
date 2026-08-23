# Cortex (Android app)

A small native Android app that:

1. Opens your live Cortex site (`https://reasoningcortexhub.netlify.app`) in a WebView — this app does not duplicate any of the website's functionality, it just wraps it.
2. Adds a **Quick Settings tile** ("Cortex Files") to the phone's control panel — the tray with Bluetooth, Wi-Fi, etc. — that jumps straight to the file upload screen.

This is a separate project from the `rh4work` website repo. A change to one does not automatically appear in the other.

## Option A — build it in the cloud (no install on your PC)

This repo includes `.github/workflows/build-apk.yml`, which builds the app automatically on GitHub's own servers every time you push.

1. Create a new, empty repository on GitHub (e.g. `CortexApp`) — don't add a README/gitignore when creating it, since this project already has one.
2. Push this folder to it (see git commands below).
3. On GitHub, open the **Actions** tab of that repo. You should see "Build Cortex APK" running (or click **Run workflow** to trigger it manually).
4. When it finishes (green checkmark), click into that run → under **Artifacts** at the bottom, download `cortex-debug-apk`. Unzip it — that's your `app-debug.apk`.
5. Transfer the APK to your phone (email it to yourself, upload to Google Drive, or use a USB cable) and open it there. You'll need to allow "install from unknown sources" the first time.

Nothing is installed on your PC with this route — GitHub's servers do the actual building.

## Option B — build it locally with Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. **File → Open** → select this `CortexApp` folder.
3. Let Android Studio sync Gradle the first time — it will download everything it needs automatically (this can take a few minutes).
4. Plug in an Android phone (with USB debugging enabled) or use an emulator, then press the green **Run ▶** button.

## How to add the tile on your phone

Installing the app does **not** automatically add the tile — the user has to add it once, same as any Quick Settings tile:

1. Swipe down twice to open the full Quick Settings tray.
2. Tap the pencil/edit icon.
3. Find **"Cortex Files"** in the list of available tiles and drag it up into the active tray.
4. Done — tapping it from then on opens Cortex straight to the file upload screen.

## Changing the domain

If your Netlify site's URL ever changes, update `BASE_URL` in:

```
app/src/main/java/com/cortex/app/MainActivity.kt
```

## Publishing to the Play Store

Right now, either build option above gives you a debug APK for sideloading only. To publish on the Google Play Store, you'd need a one-time $25 Google Play developer account and to go through Play Console's app review — a separate process from anything here, and it requires a signed release build rather than the debug build the workflow produces.

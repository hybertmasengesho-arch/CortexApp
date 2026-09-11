# Cortex (Android app)

A small native Android app that:

1. Opens your live Cortex site (`https://caputa.netlify.app`) in a WebView — this app does not duplicate any of the website's functionality, it just wraps it.
2. Adds a **Quick Settings tile** ("Cortex Files") to the phone's control panel — the tray with Bluetooth, Wi-Fi, etc. — that jumps straight to the file upload screen.

This is a separate project from the `rh4work` website repo. A change to one does not automatically appear in the other.

## Release builds (signed, not debug)

The workflow now builds a proper **signed release APK** (`assembleRelease`), not a debug build. Release builds are what you'd actually distribute — smaller, and (once signed) able to receive updates without users needing to uninstall the old version first.

**This repo does NOT contain your signing key** — it can't, since anything committed to a public repo is public. Instead, the key lives as encrypted GitHub Secrets, decoded only inside each CI run.

### One-time setup

I generated a real signing keystore for you (`cortex-release.keystore`, alongside this project) — the same key **must** be used for every future release, or Android will refuse to treat a new build as an "update" to the old one. Guard it like a password: if you lose it, you can never update this app under the same identity again, only publish it as a brand-new, separate app.

Add these as repo secrets — GitHub repo → Settings → Secrets and variables → Actions → **New repository secret**, one at a time:

| Secret name | Value |
|---|---|
| `CORTEX_KEYSTORE_BASE64` | The contents of `cortex-release.keystore.base64` (the whole file, as one block of text) |
| `CORTEX_KEYSTORE_PASSWORD` | See `keystore-credentials.txt` |
| `CORTEX_KEY_ALIAS` | `cortex` |
| `CORTEX_KEY_PASSWORD` | Same as `CORTEX_KEYSTORE_PASSWORD` (PKCS12 keystores use one password for both) |

Once all four secrets are set, push again (or re-run the workflow) — the release APK will come out properly signed, and the shareable link below will serve the signed version.

**Store `cortex-release.keystore` and `keystore-credentials.txt` somewhere safe outside of git** (a password manager, an encrypted drive) — don't commit them to this repo.

## Sharing the app as a link

Every push to `main` automatically publishes the built APK to a GitHub Release tagged "latest" — this gives you one **permanent link** you can share with anyone (no GitHub login needed, unlike the Actions artifact link, which expires and requires being signed in):

```
https://github.com/<your-github-username>/CortexApp/releases/latest/download/app-release.apk
```

Replace `<your-github-username>` with your actual GitHub username (e.g. `hybertmasengesho-arch`). Send that link to anyone — tapping it on an Android phone downloads the APK directly. They'll still need to allow "install from unknown sources" the first time, same as any sideloaded app.

This link always points to whatever was built from the most recent push — you never need to update the link itself, only push new code.

## Option A — build it in the cloud (no install on your PC)

This repo includes `.github/workflows/build-apk.yml`, which builds the app automatically on GitHub's own servers every time you push.

1. Create a new, empty repository on GitHub (e.g. `CortexApp`) — don't add a README/gitignore when creating it, since this project already has one.
2. Push this folder to it (see git commands below).
3. On GitHub, open the **Actions** tab of that repo. You should see "Build Cortex APK" running (or click **Run workflow** to trigger it manually).
4. When it finishes (green checkmark), either: grab the permanent share link above, OR click into that run → under **Artifacts** at the bottom, download `cortex-release-apk` (this one requires being logged into GitHub and expires after 90 days — the Releases link above doesn't).
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

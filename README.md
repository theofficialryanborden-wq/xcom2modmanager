# XCOM 2 Android Mod Manager

An Android starter app for managing downloaded mod files for **XCOM 2 Collection** on Android.

This project is written for a beginner-friendly workflow: the app helps you import mod ZIP files, mark which ones should be exported, export enabled mod ZIPs plus a manifest, open Nexus Mods, view common console commands, and prepare files for a Google Drive sync workaround.

## Why your imported mod did not work in-game yet

The first test APK copied your `mod.zip` into this manager app's private storage. It did **not** copy the mod into XCOM 2 Collection's private game files, and it did **not** make XCOM load the mod.

That is why installing/importing a ZIP in the first APK did not change the game.

The Android version of XCOM 2 Collection does not appear to officially support the same PC mod system. Public documentation confirms Google Drive is used for save backups, but it does not document a supported Android mod folder. Until we verify an actual folder or sync format that XCOM reads, the app can only safely:

1. store downloaded mod ZIPs,
2. track which ones you want enabled,
3. export those enabled ZIPs to a Drive folder,
4. and open XCOM so you can test whether the game sees them.

## What this first version does

- **Import mods:** pick a downloaded mod ZIP from your phone and copy it into app storage.
- **Remove imports:** remove the manager's stored copy of a mod.
- **Enable / disable exports:** track which imported mods should be included when exporting to Drive.
- **Download online mods:** open the XCOM 2 Nexus Mods page in your browser.
- **Cheat sheet:** show common XCOM 2 console commands.
- **Google Drive helper:** choose a Drive folder and export enabled mod ZIPs plus the enabled-mod manifest there.
- **XCOM themed design:** dark tactical panels with cyan/orange highlight colors.
- **Open XCOM 2:** attempt to launch the Android game after preparing sync files.

## Important limitations

Android normally prevents one app from editing another app's private files. That means a normal non-root app cannot reliably copy mod files directly into XCOM 2 Collection's internal game folders.

This app also **does not inject code into XCOM 2**. A true in-game floating mod menu would require one of the following:

1. official support from the game,
2. root-level access and verified file paths,
3. a device/game-specific hooking layer,
4. or a mod loader supported by the Android build of XCOM 2.

Those approaches need real-device research and can break game terms of service or Android security expectations. This repo starts with the safe pieces first.

The Google Drive sync prompt also cannot currently be forced by a normal Android app. This app can write enabled mod ZIPs and a manifest to a Drive folder and launch the game, but the game decides when to show its own sync prompt and whether it ignores non-save files.

## Project structure

```text
app/
  src/main/
    AndroidManifest.xml
    java/com/xcom2modmanager/MainActivity.kt
    res/values/
docs/
  ROADMAP.md
```

## How to open this project

1. Install **Android Studio**.
2. Open this folder in Android Studio.
3. Let Android Studio download the Android Gradle Plugin and Android SDK if it asks.
4. Connect your Android phone or start an emulator.
5. Press **Run**.

This repository currently does not include the Gradle wrapper files, so Android Studio is the easiest way to open it.

## Download the test APK

A debug APK is included for easy phone testing:

```text
downloads/xcom2-mod-manager-debug.apk
```

On GitHub, open the `downloads` folder, click `xcom2-mod-manager-debug.apk`, then use the download button. On your phone, Android may ask you to allow installing apps from your browser or file manager because this is a test build.

## Beginner explanation

Think of this first app as a control panel and filing cabinet:

- It keeps copies of mod downloads.
- It remembers which ones you marked enabled for export.
- It writes that list into a file called `xcom2_mod_manager_enabled_mods.json`.
- It helps you get that file and the enabled ZIPs into Google Drive.

The next hard part is proving exactly which files XCOM 2 Collection on Android reads during Google Drive sync. Once that is known, the app can export files in the format and folder layout the game expects.

# XCOM 2 Android Mod Manager

An Android starter app for managing downloaded mod files for **XCOM 2 Collection** on Android.

This project is written for a beginner-friendly workflow: the app helps you import mod ZIP files, turn them on or off inside the manager, export an enabled-mod manifest, open Nexus Mods, view common console commands, and prepare files for a Google Drive sync workaround.

## What this first version does

- **Install mods:** pick a downloaded mod ZIP from your phone and copy it into app storage.
- **Uninstall mods:** remove the manager's stored copy of a mod.
- **Enable / disable mods:** track which installed mods should be active.
- **Download online mods:** open the XCOM 2 Nexus Mods page in your browser.
- **Cheat sheet:** show common XCOM 2 console commands.
- **Google Drive helper:** choose a Drive folder and export the enabled-mod manifest there.
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

The Google Drive sync prompt also cannot currently be forced by a normal Android app. This app can write a manifest to a Drive folder and launch the game, but the game decides when to show its own sync prompt.

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

## Beginner explanation

Think of this first app as a control panel and filing cabinet:

- It keeps copies of mod downloads.
- It remembers which ones you marked enabled.
- It writes that list into a file called `xcom2_mod_manager_enabled_mods.json`.
- It helps you get that file into Google Drive.

The next hard part is proving exactly which files XCOM 2 Collection on Android reads during Google Drive sync. Once that is known, the app can export files in the format and folder layout the game expects.

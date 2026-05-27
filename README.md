# XCOM 2 Android Mod Manager

An Android starter app for managing downloaded mod files for **XCOM 2 Collection** on Android.

This project is written for a beginner-friendly workflow: the app helps you import mod ZIP files, mark which ones should be enabled, build an Android USB install package for XCOM 2 Collection, generate `XComModOptions.ini`, export backup ZIPs plus a manifest, open Nexus Mods, and view common console commands.

## Why your imported mod did not work in-game yet

The first test APK copied your `mod.zip` into this manager app's private storage. It did **not** copy the mod into XCOM 2 Collection's private game files, and it did **not** make XCOM load the mod.

That is why installing/importing a ZIP in the first APK did not change the game.

Many PC mods can work on Android, but the files must be placed in XCOM 2 Collection's Android data folder and mods must be enabled in config files. The app can safely:

1. store downloaded mod ZIPs,
2. track which ones you want enabled,
3. extract enabled mods into the Android WOTC folder layout,
4. generate `XComModOptions.ini` with `ActiveMods="..."` lines,
5. create a USB-copy package for Windows/MTP transfer,
6. and open XCOM after you copy/edit the files.

## What this first version does

- **Import mods:** pick a downloaded mod ZIP from your phone and copy it into app storage.
- **Remove imports:** remove the manager's stored copy of a mod.
- **Enable / disable mods:** track which imported mods should be included in the Android install package.
- **Download online mods:** open the XCOM 2 Nexus Mods page in your browser.
- **Cheat sheet:** show common XCOM 2 console commands.
- **Android USB package:** create the exact folder structure for `com.feralinteractive.xcom2_android`.
- **Config generation:** write `XComModOptions.ini` with one `ActiveMods` line per enabled mod.
- **Backup export:** write enabled mod ZIPs plus the enabled-mod manifest.
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

On newer Android versions, normal file managers and apps often cannot directly write to `Android/data`. The app therefore builds a package that you copy with USB file transfer from a computer.

## Android install path

The working Android WOTC path is:

```text
Internal Storage/Android/data/com.feralinteractive.xcom2_android/files/feral_app_support/VFS/Local/my games/XCOM2 War of the Chosen/XComGame
```

Mods go here:

```text
Internal Storage/Android/data/com.feralinteractive.xcom2_android/files/feral_app_support/VFS/Local/my games/XCOM2 War of the Chosen/XComGame/Mods
```

Each mod needs its own folder inside `Mods`.

The mod options file goes here:

```text
Internal Storage/Android/data/com.feralinteractive.xcom2_android/files/feral_app_support/VFS/Local/my games/XCOM2 War of the Chosen/XComGame/Config/XComModOptions.ini
```

The app generates this file in the package:

```ini
[Engine.XComModOptions]
ActiveMods="ExampleModName"
```

You must still manually edit Feral's preference `.ini` under `feral_app_support`:

```xml
<value name="DisableAllMods" type="integer">1</value>
```

Change it to:

```xml
<value name="DisableAllMods" type="integer">0</value>
```

The app does not overwrite that preference file because it may contain other important game settings.

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
- It helps you build `XCOM2_ANDROID_USB_INSTALL_PACKAGE/Android/data/com.feralinteractive.xcom2_android/...`.
- It generates `XComModOptions.ini`.
- It can also back up the original ZIPs into Google Drive.

The remaining hard part is automating the final copy into `Android/data`. On Android 14, the reliable method is still USB file transfer from a computer.

# Roadmap

## Version 0.1 in this branch

- Native Android starter app.
- Import local mod ZIP files with Android's file picker.
- Store mod records in app preferences.
- Enable and disable imported mods for export.
- Delete imported mod copies.
- Open Nexus Mods in the browser.
- Display common console commands.
- Choose an export/staging folder using Android's document picker.
- Export an enabled-mod manifest locally and to the selected Drive folder.
- Export enabled mod ZIP files to the selected Drive folder for backup/sync testing.
- Build an Android USB install package rooted at:
  - `XCOM2_ANDROID_USB_INSTALL_PACKAGE/Android/data/com.feralinteractive.xcom2_android/files/feral_app_support/VFS/Local/my games/XCOM2 War of the Chosen/XComGame`
- Extract enabled mod ZIP files into:
  - `.../XComGame/Mods/<ModName>`
- Generate:
  - `.../XComGame/Config/XComModOptions.ini`
- Attempt to launch XCOM 2 Collection after export.

## Current test result

Importing a ZIP into the manager does not make it work in XCOM. The imported file is stored in this app's private storage, not in XCOM 2 Collection's private game data.

The user confirmed many PC mods can work on Android and provided the working Feral path. The app now builds a USB-copy package for that path and generates ActiveMods entries. The remaining manual step is editing Feral's preference `.ini` so `DisableAllMods` is `0`.

## Research needed on a real Android device

These items require a purchased/installed copy of XCOM 2 Collection and real-device testing:

1. Confirm whether `XComModOptions.ini` casing/location is always accepted on Android.
2. Confirm whether the mod identifier should always be the `.XComMod` filename, or whether some mods need a different ActiveMods value.
3. Find the exact Feral preference `.ini` filename/path across Android versions.
4. Determine whether Storage Access Framework can write the target path on any supported Android versions, or whether USB is always required.
5. Verify whether console commands can be enabled in the Android build.
6. Track compatibility: UI mods may fail; Community Highlander is reported to crash.

## Later safe features

- Mod metadata parser for common XCOM 2 workshop/Nexus package layouts.
- Conflict warnings when two mods include the same game config file.
- Per-mod notes and screenshots.
- Nexus Mods API integration if the user provides an API key and Nexus terms allow the workflow.
- Export profiles, such as "vanilla", "quality of life", and "cheat run".
- A guided setup wizard for USB copying and preference `.ini` editing.
- Optional rooted direct installer for users who knowingly grant root access.

## In-game menu/injection note

A floating in-game menu is not part of this safe starter implementation. It should only be considered after there is a verified, legal, and maintainable method for the Android port. If the only option is root-level process hooking, that should live in a separate research branch with clear warnings and device-specific documentation.

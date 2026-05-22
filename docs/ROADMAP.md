# Roadmap

## Version 0.1 in this branch

- Native Android starter app.
- Import local mod ZIP files with Android's file picker.
- Store mod records in app preferences.
- Enable and disable installed mods.
- Delete installed mod copies.
- Open Nexus Mods in the browser.
- Display common console commands.
- Choose a Google Drive folder using Android's document picker.
- Export an enabled-mod manifest locally and to the selected Drive folder.
- Attempt to launch XCOM 2 Collection after export.

## Research needed on a real Android device

These items require a purchased/installed copy of XCOM 2 Collection and real-device testing:

1. Confirm the Android package name for XCOM 2 Collection.
2. Determine whether the mobile port reads any user-modifiable mod folders.
3. Determine exactly what triggers the built-in Google Drive sync prompt.
4. Inspect the synced Google Drive folder layout before and after a game save sync.
5. Test whether the game imports non-save files from Google Drive or ignores them.
6. Verify whether console commands can be enabled in the Android build.
7. Verify whether any supported mod loader exists for the Android build.

## Later safe features

- Mod metadata parser for common XCOM 2 workshop/Nexus package layouts.
- Conflict warnings when two mods include the same game config file.
- Per-mod notes and screenshots.
- Nexus Mods API integration if the user provides an API key and Nexus terms allow the workflow.
- Export profiles, such as "vanilla", "quality of life", and "cheat run".
- A guided setup wizard for Google Drive folder testing.

## In-game menu/injection note

A floating in-game menu is not part of this safe starter implementation. It should only be considered after there is a verified, legal, and maintainable method for the Android port. If the only option is root-level process hooking, that should live in a separate research branch with clear warnings and device-specific documentation.

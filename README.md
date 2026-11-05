# LocalDex Widget APK build (isolated branch)

This is an **isolated branch** in the existing K5HudAATest2 repository.
The default `main` branch is not modified and no K5 project code is shipped.

The workflow downloads the upstream source from https://github.com/sam1am/localdex
then applies the widget overlay stored in `localdex-overlay.tar.xz.b64`, and builds
`LocalDex-Widget-debug.apk` using GitHub Actions.

**Features:** Samsung DeX session start/stop widget, viewer shortcut, optional PC control
over an authenticated ADB connection on local Wi-Fi.

**Requirements:** Samsung One UI 8+, wireless debugging paired in LocalDex, Wi-Fi.
After workflow success, open Actions → Build LocalDex widget APK → artifacts.

Note: This is an experimental debug APK, not yet tested on a physical handset.
PC launcher and readable overlay files are supplied separately in the conversation ZIP.

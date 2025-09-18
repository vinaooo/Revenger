# Ludere
A LibRetro-powered ROM packager for portable emulation

# Philosophy
The current state of emulation on Android is excellent relative to other methods of emulation. However, the experience is not **seamless** and it is not **universal**. Allow me to elaborate. By seamless, I mean to say that there are very few steps involved between opening the application and actually playing the game. With most emulators from a fresh install, one must open the application, download a core (i.e. RetroArch), locate their ROM, and then begin playing, totally at least two steps of interference. Contrarily, Ludere reduces the process down to one simple step: open the application. The core, ROM, controls, core settings, and everything else are already configured. In terms of universality, one cannot easily duplicate their configuration across devices without repeating the steps for each device. Instead, Ludere is a simple APK with all configuration already prepared, so installing an exact duplicate of the game is as easy as installing any other APK.

# Purpose
The goal of Ludere is to increase the level of abstraction for emulation on Android.

Here's a diagram of how most Android emulators are configured:

```
└── Generic Emulator App
    ├── Roms
    │   ├── rom1.gba
    │   ├── rom2.gba
    │   └── rom3.gba
    ├── Saves
    │   ├── rom1.sav
    │   ├── rom2.sav
    │   └── rom3.sav
    └── States
        ├── rom1.state
        ├── rom2.state
        └── rom3.state
```

Here's how Ludere is configured:

```
└── Ludere
    ├── rom
    ├── save
    ├── state
    └── *other system files*
```

# Features
- LibRetro core is fetched once on the first launch
- ROM is packaged inside the APK, no external importing required
- Save state support (single slot)
- SRAM is saved when the application loses focus
- All-in-one package, can be easily distributed once packaged

# Libraries
- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid): Our LibRetro frontend that interacts with RetroArch cores
- [RadialGamePad](https://github.com/Swordfish90/RadialGamePad): Intuitive touchscreen controls
- [LibRetro](http://buildbot.libretro.com/nightly/): Emulator cores for Android

# Configuration
- Edit `app/src/main/res/values/config.xml` and change your configuration
- Copy your ROM to `app/src/main/res/raw/rom` (where `rom` is the ROM file)

# Building Offline
It is usually best to build a release build to reduce the total file size and improve performance.
- `./gradlew assembleRelease`

This uses the official Ludere keystore to sign the APK. This is available in the root directory of the project. Feel free to use this key for your personal projects.

The output APK is located here: `app/build/outputs/apk/release/app-universal-release.apk`

# Autogen Tool
Ludere has a directory called `autogen` which contains a basic script to batch-generate Ludere packages. To use it, simply navigate to this folder. Place your ROMs in the `input` folder. In this same folder, put a `config.xml` file with your preferred configuration for these ROMs. Ignore the ID and NAME fields, as they will be overwritten. The script also supports nested folders, in which each can contain their own configuration file. Execute the script with `python generate.py`.

# Building Online
I know a lot of users are not experienced in building Android Studio projects but would still like to package their own Ludere packages. I've created a GitHub action to help those people.

**TL;DR: You can build Ludere packages online**

1) Fork this repository by clicking the button in the top right corner of the repository. You may need to be on Desktop Mode for this to show up.

2) Get a direct URL to your autogen payload (everything that would be in the `input` folder). Since GitHub Actions doesn't let us directly select a file to upload, you need to get a direct URL that the workflow can download. The easiest way to do this is by using Google Drive. Upload your ROM to Google Drive, then right click on it and click "Get link". Make sure it's set to "Anyone with the link" and copy it to your clipboard. The share link itself is not a direct download link, so head over to [this site](https://www.wonderplugin.com/online-tools/google-drive-direct-link-generator) to convert it into one. Keep the direct URL handy, we'll need it later.

4) Now we get to build the APK! Navigate to your forked Ludere repository we made in step 1. You should see a tab called "Actions" with a little play button icon next to it. Click on it. If you get a prompt asking you to enable Actions, just hit enable. Now, find the "Autogen" tab. If your browser is zoomed in, you might see a drop-down where you can switch the tab from "All workflows" to "Autogen". Now you should see a button that says "Run workflow" and it will prompt you for your **Payload URL**. Paste it here and click "Run workflow".

You can watch the build in realtime if you'd like. It can take quite a while to build, around 5 minutes per APK. When it finishes, your fork will have a new release with the APK attached. You can find the releases tab from the home page of your fork. And that's it! You can install that APK on any device you'd like.

# Keystore
There is a keystore for signing Ludere packages that is public and free to use. Here are the details you should know when signing with it:

- Keystore Password: `ludere`
- Key Alias: `key0`
- Key password: `ludere`

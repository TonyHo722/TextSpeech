# Build Environment Setup and Application Build Walkthrough

This document records the setup of the Android build environment and the successful build of the TextSpeech application.

## Actions Taken

1.  **Installed Java 17**: Downloaded and configured Amazon Corretto 17 in `$HOME/opt/jdk-17`.
2.  **Installed Android SDK**:
    *   Downloaded Command Line Tools to `$HOME/android-sdk`.
    *   Accepted all Android SDK licenses.
    *   Installed `platform-tools`, `platforms;android-34`, and `build-tools;34.0.0`.
3.  **Installed Gradle 8.4**: Downloaded and configured Gradle 8.4 in `$HOME/opt/gradle`.
4.  **Built the Application**: Executed `gradle clean assembleDebug` with the necessary environment variables.
5.  **Generated APK**: Verified the output and copied the debug APK to the root directory.

## Results

*   **Build Status**: `SUCCESSFUL`
*   **APK Location**: `TextSpeech_20260424_1053.apk`
*   **APK Size**: ~11 MB

## Build Commands Reference

To build the app again in this environment, use the following commands:

```bash
export JAVA_HOME=$HOME/opt/jdk-17
export ANDROID_HOME=$HOME/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$HOME/opt/gradle/bin:$PATH

gradle clean assembleDebug
```

---

## Android Emulator Setup

I have set up a headless Android emulator to test the APK.

### Actions Taken

1.  **Installed Emulator & System Image**: Downloaded the `emulator` package and `system-images;android-34;google_apis;x86_64`.
2.  **Created AVD**: Created a virtual device named `test_device`.
3.  **Started Emulator**: Launched the emulator in headless mode (`-no-window -no-audio`).
4.  **Installed APK**: Installed the generated `TextSpeech` APK onto the running emulator.
5.  **Launched App**: Started the application's main activity.

### Emulator Commands Reference

To manage the emulator in this environment:

```bash
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_AVD_HOME=$HOME/.config/.android/avd
export PATH=$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH

# Start emulator in background
emulator -avd test_device -no-window -no-audio -no-boot-anim -gpu off &

# Check status
adb devices
adb shell getprop sys.boot_completed

# Install/Update APK
adb install -r TextSpeech_YYYYMMDD_HHMM.apk

# View Logs
adb logcat -v time | grep -i "textspeech"
```

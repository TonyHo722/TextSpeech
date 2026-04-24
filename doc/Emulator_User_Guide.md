# Android Emulator User Guide

This guide explains how to manage and interact with the Android Virtual Device (AVD) used for testing the TextSpeech application.

---

## 🟢 1. Running with UI (Recommended for Local Desktop)

If you are working directly on a Linux desktop and want to see the phone interface, follow these steps:

### Step-by-Step Example:
1.  **Kill any background emulator**:
    ```bash
    adb -s emulator-5554 emu kill
    ```
2.  **Start the emulator with UI**:
    ```bash
    emulator -avd test_device -gpu host &
    ```
3.  **Wait for the phone window to appear**: It may take 30-60 seconds for the Android home screen to load.
4.  **Install your APK**:
    ```bash
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```
5.  **Launch the App**: Look for the "TTS Reader" icon on the emulator home screen and click it, or run:
    ```bash
    adb shell am start -n com.example.textspeech/com.example.textspeech.MainActivity
    ```

---

## ⚪ 2. Running in Headless Mode (Background)

Headless mode runs the emulator in the background without a graphical window. Ideal for remote sessions.

### Start Headless
```bash
emulator -avd test_device -no-window -no-audio -no-boot-anim -gpu off &
```

### Stop Emulator
```bash
adb -s emulator-5554 emu kill
```

---

## 🔵 3. Interaction & Testing

### Take a Screenshot (Headless Mode)
```bash
adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png ./screenshot.png
```

### View Live Logs
```bash
adb logcat -v time | grep -i "textspeech"
```

### Simulate Sharing a URL
```bash
adb shell am start -a android.intent.action.SEND -t "text/plain" --es android.intent.extra.TEXT "https://example.com" com.example.textspeech/com.example.textspeech.MainActivity
```

### Simulate Hardware Buttons
- **Home Button**: `adb shell input keyevent 3`
- **Back Button**: `adb shell input keyevent 4`
- **Power Button**: `adb shell input keyevent 26`

---

## 🟡 4. Troubleshooting

| Issue | Solution |
| :--- | :--- |
| **Command 'adb' not found** | Run `source ~/.bashrc` to update your PATH. |
| **Device 'offline'** | Wait for the boot to complete (check with `adb devices`). |
| **Emulator fails to start** | Check KVM: `ls -l /dev/kvm`. |
| **App not appearing** | Reinstall the APK: `adb install -r <filename>.apk`. |

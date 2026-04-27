#!/bin/bash
echo "Updating icon files..."
cp picture/TextSpeech_icon_squared.png app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
cp picture/TextSpeech_icon_squared.png app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png

echo "Setting up environment variables..."
export JAVA_HOME=$HOME/opt/jdk-17
export ANDROID_HOME=$HOME/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$HOME/opt/gradle/bin:$PATH

echo "Building APK..."
gradle clean assembleDebug

if [ $? -eq 0 ]; then
  echo "Build successful! Copying APK..."
  cp app/build/outputs/apk/debug/app-debug.apk TextSpeech_new.apk
  echo "APK is ready at TextSpeech_new.apk"
else
  echo "Build failed!"
  exit 1
fi

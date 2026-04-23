#!/bin/bash
set -e

# 1. Install Java 17
echo "Setting up Java 17..."
mkdir -p $HOME/opt
cd $HOME/opt
if [ ! -d "jdk-17" ]; then
    wget -qO jdk17.tar.gz "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz"
    tar xzf jdk17.tar.gz
    mv amazon-corretto-17* jdk-17
    rm jdk17.tar.gz
fi
export JAVA_HOME=$HOME/opt/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# 2. Install Android Command Line Tools
echo "Setting up Android SDK Command Line Tools..."
ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools
cd $ANDROID_HOME/cmdline-tools
if [ ! -d "latest" ]; then
    # Downloading Android SDK command line tools
    wget -qO cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    unzip -q cmdline-tools.zip
    mv cmdline-tools latest
    rm cmdline-tools.zip
fi

export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

# 3. Accept SDK licenses and install build tools
echo "Accepting Android SDK licenses and downloading build tools..."
yes | sdkmanager --licenses > /dev/null
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null

# 4. Install Gradle (since gradlew is missing)
echo "Setting up Gradle 8.4..."
cd $HOME/opt
if [ ! -d "gradle" ]; then
    # We use 8.4 which is safe for AGP 8.2.0
    wget -qO gradle.zip "https://services.gradle.org/distributions/gradle-8.4-bin.zip"
    unzip -q gradle.zip
    mv gradle-8.4 gradle
    rm gradle.zip
fi
export PATH=$HOME/opt/gradle/bin:$PATH

# 5. Build the application!
echo "Compiling the App..."
cd /home/tonyho/workspace/TextSpeech
gradle clean assembleDebug

# 6. Rename and Copy APK for the user
TIMESTAMP=$(date +"%Y%m%d_%H%M")
APK_NAME="TextSpeech_${TIMESTAMP}.apk"
cp app/build/outputs/apk/debug/app-debug.apk ./"$APK_NAME"

echo "----------------------------------------------------"
echo "Build complete!"
echo "Your new APK is: /home/tonyho/workspace/TextSpeech/$APK_NAME"
echo "----------------------------------------------------"

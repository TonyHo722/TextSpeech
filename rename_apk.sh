#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M)
mv TextSpeech_new.apk TextSpeech_${TIMESTAMP}.apk
echo "Renamed APK to TextSpeech_${TIMESTAMP}.apk"

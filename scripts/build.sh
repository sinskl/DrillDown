#!/bin/bash
set -e

echo "Building DrillDown..."

chmod +x gradlew

./gradlew clean

echo "Building Android..."
./gradlew android:assembleFullRelease

echo "Building Desktop..."
./gradlew desktop:dist

echo "Build complete!"
echo "APK: android/build/outputs/apk/fullRelease/"
echo "Desktop: desktop/build/distributions/"

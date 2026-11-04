#!/bin/sh
# Creates the release keystore used by app/build.gradle.kts (replace the password for any public release).
mkdir -p release
keytool -genkeypair -v -keystore release/sandhimitra-release.jks -alias sandhimitra -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "${SANDHIMITRA_KS_PASS:-sandhimitra-release}" -keypass "${SANDHIMITRA_KS_PASS:-sandhimitra-release}" \
  -dname "CN=Sandhimitra, OU=Sandhimitra, O=Dr. Diddigi Vamsi Krishna, C=IN"

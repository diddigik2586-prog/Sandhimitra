# Build instructions
Requirements: JDK 17 or 21, Android SDK platform 35 + build-tools 35 (set `sdk.dir` in `local.properties`).
```
./gradlew :app:assembleDebug            # debug APK
sh tools/release/make_keystore.sh       # once, creates release/sandhimitra-release.jks (set SANDHIMITRA_KS_PASS to change password)
./gradlew :app:assembleRelease          # signed release APK -> app/build/outputs/apk/release/
./gradlew :app:testDebugUnitTest        # 41 engine + 7 UI + screenshot tests
```
UI tests use Robolectric. If Maven Central is unreachable, download `android-all-instrumented` jars manually and set `ROBOLECTRIC_DEPS_DIR`.
Content/lexicon regeneration: `tools/content/parse_sources.py`, `tools/content/build_content.py`, `tools/lexicon/build_lexicon.py` (inputs: `original_assets/legacy/source_materials_working_extraction.json`, Monier-Williams `mw.txt` from sanskrit-lexicon/csl-orig).

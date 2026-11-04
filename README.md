# सन्धिमित्रम् — Sandhimitra

Native Android Sanskrit grammar learning and Sandhi analysis application.

## Locked master specification (04 Nov 2026)
- Native Android; Kotlin, Jetpack Compose, Material 3; MVVM; offline-first.
- Interface languages: Sanskrit, Kannada and English.
- **Current approved theme: Indigo & Copper** (this specification supersedes the earlier Rosewood theme).
  - Light: background `#F0F2F8`, card `#FFFFFF`, primary `#394B78`, accent `#C17B55`, text `#28334D`.
  - Dark: background `#171E30`, card `#262F49`, primary `#D18B63`, accent `#E6AE88`, text `#F0F2F8`.
- Retain the supplied app icon; no redesign or substitution.
- Splash: five seconds, supplied icon, Sanskrit and English app names, determinate progress bar only, exact creator footer.
- Six home cards: Introduction; Vowel Sandhi; Consonant Sandhi; Visarga Sandhi; Prakṛtibhāva Sandhi; Sandhini Engine.
- Lesson/Practice selector only within Vowel, Consonant and Visarga sections.
- Menu: language, theme, font size, About App, About Dr. DVK, Support Sandhimitra and Feedback.
- Exact global footer: `Compiled and Created by` / `©Dr. Diddigi Vamsi Krishna`.
- No source-document titles or prohibited honorifics in user-facing app content.
- No fabricated grammatical results or sūtra references. Unsupported analysis must not be mislabeled as grammatically impossible.

## Current implementation checkpoint
- Existing Android scaffold retained; theme changed to Indigo & Copper.
- Five-second splash progress is determinate; supplied project icon retained.
- Six-card home and Lesson/Practice capsule placement retained.
- Language, theme and font-size preferences are persisted locally.
- About App, About Dr. DVK, Support and Feedback screens added. Feedback opens an email composer addressed to `dvkrishna92@gmail.com`.
- Unconfirmed payment methods are not displayed. A portrait was not present in the current source asset set; no substitute image has been used.
- Sanskrit and Kannada DOCX material extracted into a working JSON asset. Extraction is not yet edited into final lessons; paragraphs and tables require section mapping and scholarly review.
- The existing unverified demo rule outputs were disabled. The current engine returns no result until a rule has been independently verified and entered in the registry.
- A curriculum coverage checklist has been added at `docs/CURRICULUM_COVERAGE.json`.

## Build requirements
JDK 17/21, Android SDK Platform 35, Android Gradle Plugin 8.7.3 and Gradle 8.9+.

```bash
gradle :app:assembleDebug
```

The current workspace has no Gradle executable, wrapper JAR or Android SDK. APK compilation and installation testing have therefore **not** been completed. Do not represent this checkpoint as an APK.

## Next work
1. Map both source extractions to the five curriculum sections without changing grammatical substance.
2. Curate Sanskrit and Kannada lesson text and author independent scholarly English.
3. Create the verified rule registry with conditions, sūtra traceability, exceptions and every documented vikalpa.
4. Implement deterministic forward and reverse analysis with explicit ambiguity states.
5. Add persistent lesson completion, bookmarks, practice attempts and history.
6. Complete About Dr. DVK portrait integration once the exact photograph is available; add support payment methods only after confirmed details.
7. Add regression tests, build with Android SDK, install and test on a physical Android device.

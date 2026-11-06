# Sandhimitram — Build 20 Session Resume

## Project status

Build 20 (versionName 0.1, versionCode 21 — second revision with the additional question formats) adds the Sandhicakram game, the question bank, bookmarks, the localized application
name and the no-scroll Home page on top of Build 19. Engine, lexicon, lesson content, fonts and the info pages are unchanged.
Deliverables: `dist/Sandhimitra-0.1-build21.apk` (versionCode 21, signed with the upload key) and `dist/Sandhimitra-source-0.1-build21.zip`.

## Architecture (where things live)

| Area | Files |
|---|---|
| Engine (SLP1, rules, forward join, reverse split) | `app/src/main/java/org/sandhimitra/app/engine/*` |
| Lexicon (Monier-Williams + attested forms) | `assets/lexicon/lexicon.db`, `data/LexiconDb.kt`, builder `tools/lexicon/build_lexicon.py` |
| Lesson content | `assets/content/index.json`, `assets/content/topics/*.json`, `data/Content.kt`; pipeline `tools/content/*` |
| Question bank | `assets/quiz/questions.json`, `data/Quiz.kt`; builder `app/src/test/.../QuizBankBuild.kt` (run with `QUIZ_BUILD=1`) |
| Local database (SQLite, schema 2) | `data/Progress.kt` — tables `lesson`, `practice`, `bookmark`, `history` (v1), `quiz_session`, `quiz_done` (v2) |
| UI | `ui/MainApp.kt` (routes), `ui/Components.kt` (header, Page), `ui/Screens1.kt` (splash, Home), `ui/Screens2.kt` (lessons, practice), `ui/Games.kt` (Sandhicakram, quiz, bookmarks), `ui/Sandhatri.kt`, `ui/Info.kt`, `ui/Strings.kt`, `ui/Theme.kt`, `ui/Fonts.kt` |
| Release signing | `release/keystore.properties` + `release/sandhimitra-upload.jks` (not in git) |

### Database changes in Build 20

- `quiz_session(cat TEXT PRIMARY KEY, state TEXT, ts INTEGER)` — one JSON document per category (`QuizSession.toJson`):
  `seq` (question ids), `pos`, `correct`, `wrong`, `score`, `history[{q, chosen, ok}]`, `vik{qid:[option indices]}`, `nav`.
- `quiz_done(cat, qid, ts, PRIMARY KEY(cat, qid))` — questions answered correctly at least once (landing-page progress).
- Migration: `Progress.upgrade(db, old)` creates the two tables with `IF NOT EXISTS`; nothing else is touched.

### Stable ids

Questions `<topic>-<ex|pr><row>-<F|R>`; lessons/practice `tNN` (content index); categories `vowel|consonant|visarga|prakriti`;
answer alternatives = option indices stored per question id; bookmarks keyed by lesson id; sessions keyed by category.

## Completed in this session

1. Localized name (Sandhimitram / ಸಂಧಿಮಿತ್ರಂ / wordmark), manifest label, menu and mail subject.
2. Home page without scrolling; Sandhicakram card (fixed Copper/Indigo, joystick before title, centred, shimmer, reduced-motion aware).
3. Sandhicakram landing (2 × 2 squares with progress) and the mixed quiz (four options, auto-advance, wrong-answer links to the
   exact lesson and practice, vikalpa sequence, full persistence and restore).
4. Question bank: 1,519 engine-verified questions — 906 forward/reverse (vowel 362, consonant 350, visarga 168, prakṛtibhāva 26) plus 613 in the additional formats (identify the Sandhi, identify the sūtra, select an example), audit in
   `SANDHICHAKRAM_QUESTION_AUDIT.md` and `docs/quiz_bank_audit.txt`.
5. Bookmarks: lesson toggle, header icon, My Bookmarks page, persistence without duplicates.
6. Schema migration 1 → 2 with a test proving Build 19 rows survive.
7. Tests, documentation, signed APK and source ZIP.

## Pending / known gaps

- **Prakṛtibhāva has 26 verified questions (requirement 150).** The supplied documents do not contain more usable rows and no
  additional authorised source was available offline. Not filled artificially.
- Engine does not model ऋत्यकः (6.1.128) and the pragṛhya dual in -ए/-ऊ when the stem is not recognised, so three documented
  prakṛtibhāva rows are not admitted.
- Device installation and the device-restart persistence check could not be executed in this environment (no emulator/device);
  the APK was built, signed and inspected only. See `BUILD_20_TEST_REPORT.md`.
- The Build 19 AAB flow is unchanged (`./gradlew :app:bundleRelease`); an AAB was not produced for Build 20 (not requested).

## Exact next steps for a future session

1. If more prakṛtibhāva material is supplied, place it in the lesson JSON (`sa.ex` / `sa.pr` rows of t37 or a new topic),
   run `QUIZ_BUILD=1 ROBOLECTRIC_DEPS_DIR=… ./gradlew :app:testDebugUnitTest --tests '*QuizBankBuild*'`, review
   `app/build/quiz/audit.txt`, copy `app/build/quiz/questions.json` to `app/src/main/assets/quiz/` and `audit.txt` to
   `docs/quiz_bank_audit.txt`, then run the full suite (`QuizBankTest` re-verifies the bank).
2. To extend the engine for 6.1.128 / further pragṛhya cases: add the rule to `Rules.kt` (verified = true only with a sūtra in
   the source and positive/negative tests), implement in `SandhiEngine.vowelVowel`/`pragrhya`, add tests in `SandhiEngineTest`.
3. Device test pass (install APK, restart device during a quiz, verify restore; Kannada locale launcher label).
4. Release: `./gradlew :app:assembleRelease` (signed automatically when `release/keystore.properties` exists); bump
   `versionCode` in `app/build.gradle.kts` for every Play upload.

## Commands

```
ROBOLECTRIC_DEPS_DIR=/path/to/android-all-jars ./gradlew :app:testDebugUnitTest     # full suite (needs offline Robolectric jars when Maven is unreachable)
./gradlew :app:assembleRelease                                                      # signed APK
git archive --format=zip -o dist/Sandhimitra-source-0.1-build21.zip HEAD -- . ':(exclude)dist'
```

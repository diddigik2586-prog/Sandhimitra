# Sandhimitram — Build 20 Test Report

Environment: Linux build container, JDK 17, Android SDK 35, Robolectric 4.14.1 (sdk 35, native graphics), no device or emulator.
Command: `ROBOLECTRIC_DEPS_DIR=… ./gradlew :app:testDebugUnitTest` — **79 tests, 79 passed, 0 failed** (final run before packaging).

## 1. Automated tests actually executed

| Area | Test | Result |
|---|---|---|
| Name / localization | `Build20Test.launcherLabelIsLocalized` — `app_name` is "Sandhimitram" (default) and "ಸಂಧಿಮಿತ್ರಂ" (kn locale); launcher label; package `org.sandhimitra.app` | pass |
| Name / localization | `Build20Test.noOldNameInInterfaceStrings` — no "Sandhimitra" (without -m) in any interface string source | pass |
| Name / localization | APK badging (`aapt2 dump badging`): `application-label:'Sandhimitram'`, `application-label-kn:'ಸಂಧಿಮಿತ್ರಂ'`, versionCode 25, versionName 0.1 | pass |
| Home | `Build20Test.homeFitsWithoutScrollingAndGamesCardIsLast` — Sandhicakram card below Sandhātrī, card and attribution inside the 360×800 dp viewport, joystick icon at the far left of the card, title centred on the card, no "n / m" progress text on Home, Bookmark icon beside the Menu icon | pass |
| Home | `UiTest.splashThenHomeHasExactSixCardsInThreeTiers`, `UiTest.tilesAreSquareAndEqual` (Build 19 layout checks) | pass |
| Home | Screenshots `01_home_sa_light`, `02_home_kn_light`, `03_home_en_light`, `04_home_en_dark`, `30_home_sa_dark` rendered and inspected: Sandhicakram card highlighted differently in light (indigo-violet, gold lettering) and dark (gold, indigo lettering), icon far left, text centred, everything inside the viewport | pass (visual) |
| Sandhicakram landing | `Build20Test.gamesLandingHasFourSquareTilesWithProgress` — four square tiles, 2 × 2, inside the viewport, "0 / total" inside the card | pass |
| Quiz | `Build20Test.quizAnswersPersistAndRestore` — a fresh round alternates forward/reverse with the additional formats spread between them (not bunched); wrong answer shows "Wrong Answer" and the two links; options lock after an answer; "Read the Lesson" opens the exact lesson and returning restores the same question with its wrong-answer view; "Next question" advances; "Previous question" shows the earlier question read-only with its recorded answer (an option tap adds nothing to the history) and Next returns; a correct answer shows Correct! and advances on Next, counting as completed; leaving and re-entering restores the position | pass |
| Quiz (vikalpa) | `Build20Test.vikalpaQuestionNeedsBothForms` — first valid form Correct, same question re-presented, same form again → "Try Again", other form Correct, then next question; practised alternatives persisted | pass |
| Question bank | `QuizBankTest` (4 tests): **noQuestionIsRepeated** (one question per word and format, one forward question per analysis, no look-alike wrong options); **categoryAndLessonFollowTheRulesThatApply** (category equals a category of the applied rules; prakṛtibhāva questions apply only pragṛhya rules and nothing else does; exactly one askable rule of the lesson for name/sūtra/example); **acceptedAnswersAreTheDocumentsOwnForms** (every accepted form and prompt occurs in the lesson row it was made from); **everyShippedQuestionIsVerified** (all 1,032 questions re-derived by the engine: accepted forms produced, no wrong option derivable under any context, no adjacent vowels in wrong options, every wrong reverse part is a real word, name/sūtra distractors are not applied rules or generic entries, example options re-derived from their documented pairs) | pass |
| Question bank | `Build20Test.questionBankLoadsThroughAssets` — bank loads from the APK asset path | pass |
| Bookmarks | `Build20Test.bookmarkToggleListAndRemove` — toggle in a lesson (three taps → on), no duplicate on repeated insert, My Bookmarks lists title + parent category, item opens the exact lesson, removal from the list, empty state | pass |
| Database | `Build20Test.schemaMigrationKeepsBuild19Data` — a schema-1 database with lesson/bookmark/history rows upgraded through `Progress.upgrade`: rows intact, new tables present and empty | pass |
| Regression: engine | `SandhiEngineTest` (51 tests), `CorpusTest`, `ProbeTest`, `AmbiguityDump` — unchanged results (corpus forward 264/281, reverse 64/81, identical to Build 19) | pass |
| Regression: UI | `UiTest` (7 tests: menu order, category → lesson → practice → back, capsule rules, exit dialog, Sandhātrī run) | pass |
| Regression: screens | `ScreenshotTest` (4 tests) renders splash, Home in three languages and dark theme, menu, lesson pages in sa/kn/en, practice, Sandhātrī, info pages, plus the new games/quiz/bookmarks screens (`31_games_sa`, `32_games_kn`, `33_quiz_sa`, `34_quiz_wrong_sa`, `35_quiz_wrong_en`, `36_bookmarks_sa`, `37_quiz_name_sa`, `38_quiz_sutra_sa`, `39_quiz_example_sa`) | pass (visual) |
| Lexicon and content intact | `lexicon.db` in the APK is byte-identical to the source (md5 85eea63b…); content assets untouched (git) | pass |
| Signing | `apksigner verify --print-certs`: signed with the upload key (CN=Dr. Diddigi Vamsi Krishna) | pass |

## 2. Tests not performed, and why

- **Installation on a device, launch, device restart during a game.** No Android device or emulator exists in this build
  environment (no KVM). The APK was built from this source, signed and inspected (badging, signature, assets); the launch path
  is exercised only under Robolectric (`MainActivity` composes in every UI test).
- **Physical-screen sizes other than 360 × 800 dp.** Layout tests run at one phone size; the Home and Sandhicakram layouts are
  computed from the available height, but very small screens (< 560 dp usable height) were not rendered.
- **Reduced-motion check of the shimmer.** The branch (`ANIMATOR_DURATION_SCALE == 0` → static card) is implemented but the
  setting could not be toggled in Robolectric.
- **Script rendering on device fonts.** Devanagari conjuncts, vowel signs, virama, anusvāra, visarga and IAST are rendered in the
  screenshots with the bundled fonts under Robolectric's native text layout; shaping by a device's Android text stack was not observed.

## 3. Known issues

- Forward/reverse questions per category: vowel 220, consonant 167 (≥150 met); **visarga 90 and prakṛtibhāva 20 are below the original 150** — reported, not filled (see `SANDHICHAKRAM_QUESTION_AUDIT.md`). Consonant and visarga reverse questions are few (11 and 9) because honest real-word wrong analyses do not exist for most rows.
- The launcher/header icon is the new supplied lotus artwork (plum tile, gold lotus) with no lettering; the former icon, which carried the old spelling, is gone.
- On tall phones the four Sandhicakram squares are limited by the screen width (two squares across), so free space remains above
  and below the grid; the cards stay square as required.

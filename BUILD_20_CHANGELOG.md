# Sandhimitram — Build 20 Changelog

Baseline: Build 19 (versionName 0.1, versionCode 19). This build: **versionName 0.1, versionCode 25** (Build 20, second
revision: versionCode 20 was the first Build 20 delivery, before the additional question formats), applicationId unchanged
(`org.sandhimitra.app`). Incremental upgrade: no screen, engine rule, lesson text, font or asset of Build 19 was removed or rewritten.

## 1. Application name and localization

| Context | Shown as |
|---|---|
| Launcher / system (English and every other locale) | Sandhimitram |
| Launcher / system (Kannada locale) | ಸಂಧಿಮಿತ್ರಂ |
| Header, splash, Home | the approved Sanskrit wordmark PNG (सन्धिमित्रम्), unchanged artwork |
| English interface strings | Sandhimitram (menu "Support Sandhimitram", feedback mail subject "Sandhimitram App Feedback") |

- `app/src/main/res/values/strings.xml` (new) and `values-kn/strings.xml` (new) hold `app_name`; the manifest label now references it.
- `Strings.kt`: `support`, `mail_subject`, new key `app_name` (सन्धिमित्रम् / ಸಂಧಿಮಿತ್ರಂ / Sandhimitram).
- `MainApp.kt`: feedback e-mail subject.
- Internal identifiers (package `org.sandhimitra.app`, class `SandhimitraApp`, theme `Theme.Sandhimitra`, preferences file) are not
  user-visible and were deliberately left unchanged so that Build 19 data and the Play identity survive.
- Test `Build20Test.noOldNameInInterfaceStrings` guards the interface strings against the old spelling.

## 2. Home page (Screens1.kt)

- Rebuilt as a viewport-sized column: header, Introduction bar, the 2 × 2 category tiles, Sandhātrī bar, **Sandhicakram card**, attribution.
  Sizes are computed from the available height (system bars respected); nothing scrolls and nothing is clipped — tiles shrink
  (remaining square) and the bars use a compact height on short screens.
- Sandhicakram card (`GamesCard`), revised at the author's request after the first Build 20 delivery: its own highlight in each
  theme — light: indigo-violet gradient `#4A3F8F → #2F3E6B` with warm-gold `#FFD88A` lettering and border; dark: gold gradient
  `#F3C77E → #D08A4E` with deep-indigo `#1F2742` lettering — so the card stands out from the neighbouring bars in both themes;
  joystick icon (`res/drawable/ic_gamepad.xml`) pinned at the far left; title and supporting text centred on the card independently
  of the icon; a slow light-sweep (`drawWithContent`, 2.6 s linear, alpha 0.22) switched off when the system animator scale is 0
  (reduced motion); no game progress is shown on Home.

## 3. Sandhicakram (Games.kt — new)

- `GamesScreen`: exactly four square category cards in a 2 × 2 grid sized to the viewport (no scrolling), each with the category
  name, completed / total verified questions, and a progress bar in a theme-oriented accent.
- `QuizScreen`: a category opens its **mixed quiz directly** — forward (yojana) and reverse (viccheda) questions shuffled
  separately and interleaved so the two kinds alternate. Four options per question; correct → "Correct!" with the explanation and a **Next question** button; wrong → "Wrong Answer", the correct answer, the explanation (स्थानी-निमित्त-विधेय note of the document plus the sūtra
  and reference from the rule registry), and the two learning links **Read the Lesson** / **View the Practice** that open the
  exact lesson/practice of the question's lesson id; then "Next question". Options lock after one answer (no double submission). A **Previous question** button re-opens earlier questions of the round read-only (recorded answer, verdict and explanation shown, options disabled), and Next returns to the current question.
- Vikalpa questions (more than one verified valid form): the first valid choice is Correct; the same question is presented again
  for the other form; choosing the same form again shows "Wrong — Try Again (choose the other valid form)"; the other valid form
  is Correct; then the next question. The practised alternatives are persisted per question.
- Session persistence (`QuizSession`, table `quiz_session`): category, question sequence, position, answer history, correct/wrong
  counts, score, vikalpa state and the "left from a wrong answer" flag are saved after every answer and navigation; re-entering
  the category (after leaving, killing the app or restarting the device) restores the same question and, if the user left from a
  wrong answer, the same wrong-answer view with its links. Completed questions are kept in `quiz_done` (primary key prevents
  duplicates) and feed the landing-page progress. "Play again" starts a fresh round when a round is finished.

## 4. Question bank (new asset `assets/quiz/questions.json`, 1,032 questions)

Formats: forward (381), reverse (116), identify the Sandhi (215), identify the sūtra (215), select an example (105). Counts are what
the lesson text supports once accuracy rules are applied (see `SANDHICHAKRAM_QUESTION_AUDIT.md`); they were not a target.
Revision 25 (accuracy pass requested by the author): one question per word (51 rows that repeated an example from another lesson
were dropped, and questions about one word are kept 25 questions apart in a round); category and lesson follow the rules that apply
(the ayādi example `रामौ + अत्र` moved from prakṛtibhāva to the vowel category); only the document's own forms are accepted
(`ना अकः`, `गा अकः` removed); every wrong option is a classic mistake, never a machine-made string (forward) or a non-word (reverse);
composite/generic rule names no longer appear as options. Each record is tagged with `type`, `cat`, `rules`, `lesson`, `practice`.

## 5. Bookmarks

- Every lesson page carries a bookmark control at the top of the lesson (`BookmarkToggle`: outlined `ic_bookmark_outline` /
  filled `ic_bookmark`, one tap toggles, no dialog). The former text button "Bookmark" in the lesson actions was replaced by it.
- Global header (`Components.kt`): logo and the approved wordmark on the left, centre empty, **Bookmark icon** beside the Menu icon
  (opens My Bookmarks via `LocalOpenBookmarks`), Menu at the far right.
- `BookmarksScreen` (route `bookmarks`): lesson title, parent Sandhi category, filled icon to remove; tapping opens the lesson;
  empty state text when there is none. Stored in the existing `bookmark` table (primary key = lesson id, so no duplicates),
  independent of game progress.

## 6. Database (data/Progress.kt)

Schema version 1 → 2. `onUpgrade` only adds `quiz_session(cat, state, ts)` and `quiz_done(cat, qid, ts)` with
`CREATE TABLE IF NOT EXISTS`; the Build 19 tables `lesson`, `practice`, `bookmark`, `history` and their rows are untouched
(test `schemaMigrationKeepsBuild19Data`). New methods: `bookmarks()`, `saveQuizSession`, `quizSession`, `clearQuizSession`,
`markQuizDone`, `quizDoneCount`. `Content.kt`: `groupOf(id)`, `ref(id)`. `SandhimitraApp`: `quiz` (lazy `QuizBank`).

## 7. Strings (Strings.kt)

New keys (Sanskrit / Kannada / English): `games`, `games_sub`, `bookmarks`, `no_bookmarks`, `q_of`, `score`, `correct_ans`,
`wrong_ans`, `try_again`, `vik_next`, `read_lesson`, `view_practice`, `next_q`, `right_answer`, `explanation`, `quiz_done`,
`play_again`, `completed_q`, `no_questions`, `app_name`.

## 8. Navigation (MainApp.kt)

Routes `games`, `quiz/{cat}`, `bookmarks`; header bookmark action provided through a composition local so that every existing
screen keeps its signature.

## 9. Tests (app/src/test)

- `QuizBankBuild.kt` — the bank builder (tool; runs only with `QUIZ_BUILD=1`).
- `QuizBankTest.kt` — verifies every shipped question against the engine and the content index.
- `Build20Test.kt` — localized label, Home fits the viewport and card order, games landing, quiz flow with persistence and
  lesson link round-trip, vikalpa sequence, bookmarks, schema migration, old-name guard, asset loading.
- `UiTest.kt`, `ScreenshotTest.kt` — menu label updated to "Support Sandhimitram".

## 10. Files added / modified

Added: `ui/Games.kt`, `data/Quiz.kt`, `assets/quiz/questions.json`, `res/values/strings.xml`, `res/values-kn/strings.xml`,
`res/drawable/ic_bookmark.xml`, `res/drawable/ic_bookmark_outline.xml`, `res/drawable/ic_gamepad.xml`,
`test/QuizBankBuild.kt`, `test/QuizBankTest.kt`, `test/Build20Test.kt`, `docs/quiz_bank_audit.txt`,
`BUILD_20_CHANGELOG.md`, `BUILD_20_TEST_REPORT.md`, `SANDHICHAKRAM_QUESTION_AUDIT.md`, `BUILD_20_SESSION_RESUME.md`.

Modified: `AndroidManifest.xml`, `app/build.gradle.kts` (versionCode 20), `SandhimitraApp.kt`, `data/Progress.kt`,
`data/Content.kt`, `ui/Components.kt`, `ui/MainApp.kt`, `ui/Screens1.kt`, `ui/Screens2.kt`, `ui/Strings.kt`,
`test/UiTest.kt`, `test/ScreenshotTest.kt`, `test/SandhiEngineTest.kt` (driver loading), `CHANGELOG.md`, `KNOWN_LIMITATIONS.md`.

Unchanged: engine (`engine/*`), lexicon database, lesson content, fonts and licences, themes, Sandhātrī screens, info pages.

## 11. App icon (revision 26)

The supplied lotus icon replaces the previous artwork everywhere: `drawable-nodpi/header_icon.png` (header and splash), legacy launcher icons `mipmap-*/ic_launcher.png` (rounded tile) and `ic_launcher_round.png` (circle), and the adaptive icon (`drawable/ic_launcher_background.xml` plum gradient + `mipmap-*/ic_launcher_foreground.png`, the lotus cut out of the artwork and scaled to the adaptive safe zone). `docs/play_store_icon_512.png` is the 512 × 512 Play Store icon.

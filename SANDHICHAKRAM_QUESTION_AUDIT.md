# Sandhicakram Question Audit — Build 20

All figures below are produced by the bank builder (`app/src/test/java/org/sandhimitra/app/QuizBankBuild.kt`) and re-checked
by the regression test `QuizBankTest`. The raw builder log with every excluded row is in `docs/quiz_bank_audit.txt`.

## 1. Source and method

**Source material.** Only the supplied grammatical documents (the Sanskrit and Kannada lesson texts bundled as
`app/src/main/assets/content/topics/*.json`). Two tables of each lesson were used:

- `ex` — the lesson's example table (विच्छेदः → संहितारूपम्, with the स्थानी-निमित्त-विधेय column and the scholarly note);
- `pr` — the lesson's exercise table (अभ्यासः, with its scholarly answer).

493 rows contained a two-part Sandhi (`पदम् + पदम् → रूपम्`, or the reverse exercise). Rows of the Introduction (t00–t07) and
Practice-set (t40–t43) lessons were assigned to a category by the Sandhi rule that the engine applies.

**No question was composed by hand, no word was invented, and no sūtra reference was written freehand.** Sūtra references in the
explanations come from the engine's rule registry (`Rules.kt`), whose entries carry the sūtra wording and Pāṇinian number as printed
in the supplied Sanskrit source.

**Verification of every playable row (automatic, by the Sandhātrī engine):**

1. the engine must reproduce the documented joined form from the documented parts (forward join). If the document lists two valid
   forms (`A / B`), each reproduced form becomes an accepted answer (a vikalpa question); a documented form the engine cannot
   produce (jihvāmūlīya sign, avagraha spellings of a commentary style) is **not offered and is never used as a distractor**;
2. every distractor is proven wrong: for a join question the engine does not produce it from the parts; for a separation question
   the engine does not join the distractor's two words into the given form. A row with fewer than three provably wrong distractors
   is excluded rather than padded;
3. the lesson id and practice id must exist in the content index;
   - prakṛtibhāva rows: only the documented form is accepted; a form the ordinary rules would give (कवी + इमौ → कवीमौ by
     savarṇadīrgha) is never accepted — pragṛhya blocks the rule — and is used as a distractor instead;
4. the forward question and the reverse question of the same row are independent records with stable ids
   (`<topic>-<table><row>-F|R`).

The regression test `QuizBankTest.everyShippedQuestionIsVerified` repeats checks 1–3 on the shipped
`assets/quiz/questions.json` on every test run.

## 2. Verified question counts (actual, from the shipped bank)

Required coverage (forward + reverse):

| Category | Forward (yojana) | Reverse (viccheda) | F + R | Minimum required | Status |
|---|---|---|---|---|---|
| Vowel Sandhi | 181 | 181 | **362** | 150 | met |
| Consonant Sandhi | 175 | 175 | **350** | 150 | met |
| Visarga Sandhi | 84 | 84 | **168** | 150 | met |
| Prakṛtibhāva | 13 | 13 | **26** | 150 | **shortfall of 124** |
| **Total** | 453 | 453 | **906** | 600 | |

Additional formats (section 5.3, optional; generated from the example rows only, never counted towards the minimum):

| Category | Identify the Sandhi (NAME) | Identify the sūtra (SUTRA) | Select an example (EXAMPLE) | All formats total |
|---|---|---|---|---|
| Vowel Sandhi | 92 | 92 | 47 | **593** |
| Consonant Sandhi | 100 | 100 | 52 | **602** |
| Visarga Sandhi | 44 | 44 | 20 | **276** |
| Prakṛtibhāva | 9 | 9 | 4 | **48** |
| **Total** | 245 | 245 | 123 | **1519** |

How the additional formats are verified:

- **NAME** ("अत्र कः सन्धिः?" — the documented `L + R → form` is shown): the accepted name is the registry name of the rule
  that the lesson teaches and that the engine actually applied in the derivation (exactly one such rule, otherwise the row is
  skipped). Distractors are names of other registry rules that are **not** applied in the derivation, not from the same lesson,
  and not sharing a sūtra with any applied rule — so no distractor can be a valid answer. The traditional name (e.g.
  श्चुत्वम्, सवर्णदीर्घः) and the lesson title (e.g. श्चुत्वसन्धिः) denote the same Sandhi; the lesson title is shown in the
  explanation.
- **SUTRA** ("अत्र किं सूत्रं प्रवर्तते?"): the accepted option is the sūtra + Pāṇinian number of that same rule; generated only
  for rules marked verified in the registry (sūtra wording and number present in the supplied source). Distractors are sūtras of
  rules not applied in the derivation.
- **EXAMPLE** ("अस्य सन्धेः उदाहरणं किम्?" with the Sandhi name as prompt): the accepted option is the row's documented form;
  the three distractors are documented, engine-reproduced forms of other rows in whose derivation no rule of that lesson's
  Sandhi occurs (`osrc` records the documented pair behind each option, and `QuizBankTest` re-derives every option).
- Formats 5 and 6 of the specification (recognise the correct formation / separation) are the existing forward and reverse
  questions.
- 30 example rows yield no additional format (listed in `docs/quiz_bank_audit.txt`): their derivation uses no rule of their
  own lesson (e.g. the "relations among rules" lessons t15/t29/t36, avagraha lesson t38, pararūpa rows derived through a
  vārttika), or more than one named rule of the lesson applies, which would make "which Sandhi" ambiguous.

Every question carries: `type`, `cat`, `rules` (registry ids), `lesson`, `practice`, `src` (document row) — the tags required
by section 5.3.

Vikalpa (more than one accepted answer): consonant 78, vowel 40, visarga 7, prakṛtibhāva 2.

Questions per lesson (lesson id = exact "Read the Lesson" target):

- Vowel: t08 50, t09 50, t10 38, t11 62, t12 48, t13 32, t14 24, t15 8, t38 24, t39 26
- Consonant: t17 22, t18 14, t19 58, t20 28, t21 28, t22 26, t23 26, t24 34, t25 32, t26 26, t27 26, t28 30
- Visarga: t30 10, t31 30, t32 32, t33 26, t34 28, t35 26, t36 16
- Prakṛtibhāva: t37 26

Rules referenced (registry ids, most frequent first): C01 196, C02 112, V02 90, V06 74, C04 74, V05 62, V07 58, H02 58, V01 56,
V09 42, V03 40, C05 36, C08 34, H12 34, C06 32, C09 30, C10 30, C11 28, H03 24, C12 24, H05 24, H07 24, H08 22, V10 18, H06 18,
C03 18, C07 18, V08 14, C13 14, P02 12, H01 10, H04 8, H10 8, H14 8, H09 6, H11 6, H13 6, P03 6, V04 4, P04 4.

## 3. Rows not admitted (40 of 493 rows; the remaining 453 rows yield the 906 questions)

| Reason | Rows |
|---|---|
| The engine does not reproduce the documented form: internal word-formation sandhi (बुध्+तिः → बुद्धिः, पुष्+तः → पुष्टः, हन्+सः → हंसः …), or a rule outside the engine (ऋत्यकः, याच्ञा, उत्थम्भनम्) | 25 |
| The row is a note, not an example (contains →, numbering or Latin text; the "relations among rules" lessons) | 14 |
| No Sandhi operates in the row | 1 |

Sixteen admitted rows offer only the engine-reproducible alternative of a documented pair (listed in `docs/quiz_bank_audit.txt`
under "partially offered": jihvāmūlīya/upadhmānīya signs, commentary-style avagraha spellings, candrabindu forms); the other
documented spelling is withheld and never shown as a wrong option.

## 4. Shortfall — honest statement

**Prakṛtibhāva has 26 verified questions, not 150.** The supplied documents contain one prakṛtibhāva lesson (t37) with 8 examples
and 6 exercises, and a handful of pragṛhya rows elsewhere. One documented row (राजा + ऋषिः → राज ऋषिः, ऋत्यकः 6.1.128) could not be admitted because the engine does not model that
rule. No additional authorised source text was available offline in this environment, and the
requirement forbids manufacturing rows; the shortfall is therefore reported rather than filled. The category is playable with its
26 verified questions. Raising it to 150 needs either a further supplied source with pragṛhya examples or engine support for
6.1.125–6.1.128 variants, after which the builder admits rows automatically.

Visarga Sandhi (168) meets the minimum with a modest margin; its lessons contain fewer example rows than the vowel and consonant lessons.

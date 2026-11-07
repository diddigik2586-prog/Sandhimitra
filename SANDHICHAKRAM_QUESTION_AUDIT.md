# Sandhicakram Question Audit — Build 20 (versionCode 25)

All figures come from the bank builder (`app/src/test/java/org/sandhimitra/app/QuizBankBuild.kt`, run with `QUIZ_BUILD=1`) and are
re-checked on every test run by `QuizBankTest`. The builder log with every dropped, refiled and excluded row is
`docs/quiz_bank_audit.txt`. **Accuracy and one-question-per-word take priority over question counts.**

## 1. Source and method

**Source.** Only the supplied lesson documents (`app/src/main/assets/content/topics/*.json`): the example table (`ex`) and the
exercise table (`pr`) of every lesson. 493 rows contain a two-part Sandhi. Nothing was composed by hand; no word, form or sūtra
reference was invented. Sūtra wording and numbers come from the engine's rule registry (`Rules.kt`), which carries them as printed
in the supplied Sanskrit source.

**Rules of the bank (each enforced by `QuizBankTest`):**

1. **The document is the only authority for a correct answer.** An accepted form is a form printed in the lesson row *and*
   reproduced by the Sandhātrī engine. Forms the engine can derive but the document does not list (e.g. `ना अकः`, `गा अकः` for
   `नै/गै + अकः`, which come from the optional Śākalya rule that does not apply inside a kṛt formation) are neither accepted nor
   offered. A documented form the engine cannot produce (e.g. a jihvāmūlīya sign) is not offered and is never a distractor.
2. **One question per word and format.** The same example is often printed in several lessons (`देव + इन्द्रः` in the guṇa lesson,
   the introduction and a practice set). Rows with the same analysis (देव/देवा + इन्द्रः, तत्/तद् + च, हरिः/हरिस् + शेते) or a shared
   documented form are one word: the copy from the lesson that teaches it is kept, the others are dropped (**51 rows**).
   Questions about the same word (join it, separate it, name its Sandhi, name its sūtra, find it as an example) are kept at least
   25 questions apart within a round.
3. **Category and lesson follow the rules that actually apply.** A row printed in a lesson is filed under the category of the
   rules the engine applies in its derivation. Two rows were refiled: `रामौ + अत्र → रामावत्र` (printed in the prakṛtibhāva lesson
   as a contrast, but the rule is ayādi 6.1.78) is a **vowel** question linked to the ayādi lesson t12, and `हरिस् + शेते`
   (visarga rules, printed in a consonant lesson) was a duplicate of `हरिः + शेते`. A prakṛtibhāva question applies only pragṛhya
   rules (P01–P04); no vowel, consonant or visarga rule can appear in it, and no question outside prakṛtibhāva applies a pragṛhya rule.
4. **Prakṛtibhāva:** only the documented pragṛhya form is accepted; the form the ordinary rules would give (कवीमौ, गुरूपदिशतः,
   फलयेते …) is a wrong option.
5. **Every wrong option is wrong and sensible.**
   - *Forward (join):* wrong options are the classic mistakes — the wrong vowel rule at a vowel junction (dīrgha, guṇa, vṛddhi, yaṇ,
     ayādi, pūrvarūpa, pararūpa, only those defined for the vowels concerned); at a consonant/visarga junction the wrong final
     sound, the final dropped, a wrong assimilation, the visarga kept, `ः→o` where it does not apply, the vowel Sandhi wrongly
     applied across a lost visarga, the correct Sandhi of a mistaken stem. A wrong option is never any form the engine derives from
     the pair under any context (default, non-pada, pragṛhya), never contains machine-made clusters (aspirate + stop, `ṣc`, `sg` …),
     never differs from the accepted form only by avagraha/candrabindu/spacing.
   - *Reverse (separate):* wrong options are other analyses made of **real words** — the part that differs from the documented pair
     must be a dictionary noun/adjective/indeclinable of some weight, a closed-class word or a form attested in the lessons; never a
     verbal root, a compound-only stem or a fragment. No wrong analysis may join to the given form under any context.
   - If fewer than three such options exist, the question is **not generated** (listed in the audit log).
6. **Identify the Sandhi / sūtra / select an example** (example rows only): the answer is the rule of the row's own lesson that the
   engine applies; options are never rules applied in that derivation, never rules sharing its sūtra, never composite or generic
   entries (V04, V10, H02, H04), and never ससजुषो रुः (8.2.66) in a visarga derivation (every visarga starts from it). For the
   example format the three wrong examples are other kept rows in which no rule of that Sandhi operates.

## 2. Question counts (actual)

| Category | Forward | Reverse | F + R | Name | Sūtra | Example | All formats |
|---|---|---|---|---|---|---|---|
| Vowel Sandhi | 133 | 87 | **220** | 76 | 76 | 38 | **410** |
| Consonant Sandhi | 156 | 11 | **167** | 89 | 89 | 45 | **390** |
| Visarga Sandhi | 81 | 9 | **90** | 41 | 41 | 18 | **190** |
| Prakṛtibhāva | 11 | 9 | **20** | 9 | 9 | 4 | **42** |
| **Total** | 381 | 116 | **497** | 215 | 215 | 105 | **1,032** |

Counts are what the lesson text supports once the rules above are applied; they were not a target.

## 3. Shortfall against the original 150 forward/reverse questions per category — honest statement

- **Vowel (220) and consonant (167) meet 150.**
- **Visarga has 90 and prakṛtibhāva 20 forward/reverse questions** — below 150. The lessons contain only so many visarga and
  pragṛhya examples, and the stricter rules remove rows that cannot be offered accurately (see §4). No question was added to reach
  a number; the original 150-per-category figure cannot be met from the supplied material without lowering accuracy.
- **Consonant and visarga reverse questions are few (11 and 9).** A reverse question needs three wrong analyses made of real words;
  for consonant and visarga Sandhi the wrong splits (`उट् + लासः`, `कक् + अपि`) are not words, so honest options do not exist for
  most rows. These rows remain available in the forward format and in the name/sūtra/example formats.

## 4. Rows not offered, and why (493 rows considered, 402 words kept)

| Reason | Rows |
|---|---|
| Same word as a row already kept (one question per word) | 51 |
| Reverse: fewer than three real-word wrong analyses | 286 |
| Forward: fewer than three sound wrong forms | 21 |
| The engine does not reproduce the documented form (internal word-formation Sandhi such as बुध्+तिः, rules outside the engine) | 25 |
| The row is a note, not an example (arrows, numbering, Latin text) | 14 |
| No Sandhi rule operates | 1 |
| Name/sūtra not generated: the lesson's own rule does not apply in the derivation, or more than one named rule of the lesson applies | 30 |

16 offered rows have a documented alternative the engine cannot produce (jihvāmūlīya signs, avagraha spellings of a commentary
style, candrabindu forms); only the reproducible form is offered, the other is never shown, not even as a wrong option.

## 5. Rows refiled by the rule that applies

- `t37 pr row 6` (`रामौ + अत्र → रामावत्र`, printed in the prakṛtibhāva lesson): V06 ayādi → **vowel**, lesson t12.
- `t17 ex row 1` (`हरिस् + शेते`, printed in a consonant lesson): H01/H02/H04 visarga rules → visarga; dropped as the same word as
  `t34 ex row 6` (`हरिः + शेते`).

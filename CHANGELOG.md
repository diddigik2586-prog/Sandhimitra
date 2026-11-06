# Changelog
## 0.1 (build 17)
- Sandhi Viccheda: आत्मन्-compounds are cited as आत्मा (आत्मावलोकनम् → आत्मा + अवलोकनम्, आत्मेति → आत्मा + इति).
- Other possible analyses list only well-formed, meaningful readings.
- Forms joining three words are split fully (आत्मेत्यवधारणम् → आत्मा + इति + अवधारणम्).
## 0.1 (build 16)
- Final full edit of Sanskrit and Kannada lesson text (content, consistency and grammar): 413 Sanskrit and 375 Kannada corrections in total.
- Sandhātrī input boxes taller so Devanagari is shown fully.
## 0.1 (build 15)
- Kannada lessons proof-read in full: 127 corrections (sūtra wording, missing viramas, wrong Sandhi results, Sanskrit agreement inside the Kannada text, stray letters). Listed in docs/SANSKRIT_CORRECTIONS.md.
## 0.1 (build 14)
- Sanskrit: 3 more anusvāra-before-vowel fixes (136 corrections in total).
- Kannada: all stray Arabic, Telugu and Bengali letters replaced with the intended Kannada letters (28 corrections). Listed in docs/SANSKRIT_CORRECTIONS.md.
## 0.1 (release numbering restarted for Play; versionCode 13)
- Fonts: Tiro Devanagari Hindi (Sanskrit), Noto Sans Kannada (Kannada), Andika (English); all SIL OFL 1.1, licence texts bundled. Kohinoor and Kannada MN removed.
## 1.1.2
- Compound first members in their stem form after न-लोप (8.2.7) are preferred: आत्मावलोकनम् → आत्म + अवलोकनम्, कर्मानुसारः → कर्म + अनुसारः (52 such stems: आत्म, कर्म, नाम …). Visarga-sandhi spellings such as मनो, तपो remain non-words (मनः + रथः).
## 1.1.1
- Sandhi Viccheda: the best analysis is shown first; other possible readings are under a collapsed "Other possible analyses".
- Compound-only dictionary forms (आत्म, मनो, तपो …: "in comp. for …" in Monier-Williams) no longer count as words: आत्मावलोकनम् → आत्मा + अवलोकनम्, मनोरथः → मनः + रथः.
- Dictionary compound markers decide the boundary: an intact second member (संहिता—पाठ) names the right part; a fused boundary (हिमालय, गणेश) requires a vowel sandhi.
- Readings that differ only in vowel length or in the word-final consonant (देव/देवा, सत्/सद्/सच्, अन्तर्/अन्तः) are merged, keeping the better-attested citation form.
- Not offered any more: parts with an avagraha, one-letter fragments (श, ग, प), lone-vowel right parts, visarga-loss readings for unspaced input, splits of simple words (बुद्धिः, गङ्गा); oblique-case first members are ranked down; numeral nominatives (सप्त) handled.
- रो रि lengthening limited to a/i/u (6.3.111).
## 1.1.0
- Lessons: the topic-name line (e.g. "1. प्रकरणनाम — सवर्णदीर्घसन्धिः प्रकरणम्।" / "ಪ್ರಕರಣದ ಹೆಸರು") removed in all languages; headings renumbered.
- Removed the label "प्रौढशास्त्रीयव्याख्यानम् —" (and Kannada "ವಿದ್ವತ್ಪೂರ್ಣ ಶಾಸ್ತ್ರೀಯ ವಿವರಣೆ —") from all lessons.
- Sanskrit text proof-read in full; 133 corrections applied (listed with reasons in docs/SANSKRIT_CORRECTIONS.md).
- Paragraphs justified with a 3 mm first-line indent in all languages.
- Engine: प्रशान् + चिनोति → प्रशाञ्चिनोति (ścutva still applies when 8.3.7 is blocked).
## 1.0.8-sample
- About page: removed the dictionary data credit line (author's decision; the licence text remains in the app assets and docs).
## 1.0.7-sample
- Removed the English-mode note about independent composition/IAST everywhere.
- About page replaced with the author's text (plus a one-line dictionary licence credit).
- Home: Introduction and Sandhātrī bars inverted (indigo with light text in light theme; copper with indigo text in dark theme).
## 1.0.6-sample
- Typing ल+ृ / ल+ॄ / ल+ॢ / ल+ॣ (and ಲ+ೃ…) in any Sandhātrī field is converted at once to the vowel ऌ / ॡ (ಌ / ೡ); the engine also reads these forms if pasted.
## 1.0.5-sample
- Input variants: ल+ृ ("लृ") is read as the vowel ऌ (likewise ॡ), IAST l̥/r̥, ':' as visarga (तव + लृकारः now gives तवल्कारः).
- Every example and exercise of the supplied tables was run through the engine (docs/CORPUS_CHECK_REPORT.txt): forward 218→264 of 281, reverse 53→69 of 81; the remainder are source-table defects.
- New: morpheme-level ayādi alternative (ने+अनम्→नयनम्, भो+अनम्→भवनम्) with ṇatva; listed vārttika exceptions (अक्षौहिणी, प्रौढः, स्वैरः, शकन्धुः, कुलटा, सारङ्गः, पतञ्जलिः, गवेन्द्रः…); ओम् (6.1.95); उद्+ल (उल्लासः); उद्+स्तम्भ; ṣ-final stems; नमस्/पुरस् (8.3.40); निः/दुः/आविः…+क/प (8.3.41); radical-r words (अन्तः, पुनः, प्रातः, स्वः, अहः) offered both ways; candrabindu variants (ताँस्तान्); dual ī/ū forms offered as pragṛhya alternative; षट्+नवतिः fix.
- Reverse analysis: no-change splits need a vowel-final left part; guess-based sandhi splits are dropped only when a clearly better no-change reading exists (dictionary-entry weight); prefixes (सु, प्र…) always kept.
## 1.0.4-sample
- Fonts: Kohinoor Devanagari for Sanskrit, Kannada MN for Kannada, Andika for English/IAST.
- Smaller wordmark on the splash (slightly smaller in the header); faint halo around the wordmark removed.
## 1.0.3-sample
- Header: app icon top-left, Sanskrit wordmark centred with its white background removed (derived transparent PNG; recoloured for dark theme), Menu right. Splash uses the same wordmark.
- Rule registry: no rule codes, no Verified chip, no explanatory line.
- Sandhi Yojana: first two fields side by side with a + between.
- Reverse analysis: when a verified no-change split exists (e.g. संहिता + पाठः), guess-based sandhi splits (संहिता + अपाठः) are no longer offered; Kannada input tested.
## 1.0.2-sample
- About App, About Dr. DVK, Support, Feedback and Privacy Policy (pages and menu labels) are English only.
- Support: bank transfer details are a collapsible dropdown.
- Sanskrit interface wording corrected (consistent 2nd-person plural imperatives, removed visarga+colon clashes, fixed case/verb forms, removed unused lexicon wording).
- Menu: Introduction entry removed (Home and Sandhātrī unchanged; Introduction still opens from the Home card).
## 1.0.1-sample
- Centre-aligned About App, About Dr. DVK, Support, Privacy.
- Support: native QR (cropped from supplied image) + payment details as text.
- Removed lexicon page and in-app keyboard (system keyboard only).
- Practice: optional answer reveal, no right/wrong marking.
- Engine: added no-change junction splits (e.g. संहिता + पाठः), nominal forms recognised via stem+ending count as verified, 8.3.35 śarpare fixed (कः त्सरति), 8.3.36 and particle rules now tested and marked verified; spurious splits of dictionary words suppressed; 'unsupported candidates' list removed from the UI.
## 1.0.0
Complete rewrite on the audited legacy scaffold: modular Compose app, 44-topic trilingual content, Sandhātrī engine (forward/reverse), MW lexicon, in-app keyboard, progress DB, About/DVK/Support/Privacy, signed release APK.

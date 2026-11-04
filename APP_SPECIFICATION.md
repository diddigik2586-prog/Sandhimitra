# Implemented specification (summary)
Identity: label "Sandhimitra"; header = supplied wordmark PNG left, empty centre, Menu right; launcher icon from the supplied icon (corner whitening made transparent; adaptive icon on #04062C).
Home: Introduction (full-width) → 2×2 square tiles (Vowel, Consonant / Visarga, Prakṛtibhāva) → Sandhātrī (full-width) → attribution. Lesson|Practice capsule only on Vowel/Consonant/Visarga topics.
Menu (right-hand panel): Language, Theme, Font size capsules; Home, Introduction, Sandhātrī; About App, About Dr. DVK, Support, Feedback (mailto, ACTION_SENDTO); Privacy Policy. Extra copper separator above About App.
Content: 44 topics from the supplied Sanskrit document (primary) and Kannada document (cross-check); independent English layer (`tools/content/en_content.py`).
Placement: topics 0–7 → Introduction; 8–15 + avagraha + y/v-lopa → Vowel; 16–29 → Consonant; 30–36 → Visarga; 37 → Prakṛtibhāva; 40–43 (type-identification, viccheda, yojana, review sets) → "Practice sets" under Sandhātrī.
Engine: SLP1 core, 38-rule registry (`engine/Rules.kt`), forward `join`, reverse `split` (candidates accepted only if forward reproduces the input), statuses Verified/Provisional/No Result.
Persistence: SharedPreferences (language, theme, font) + SQLite `progress.db` (lessons, practice, bookmarks, history).

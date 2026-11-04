# Font integration report
Supplied: Kohinoor Latin (12 OTFs, 379–380 glyphs each, Devanagari 0, Kannada 0; IAST: only ā ī ū ñ ś present — ṛ ṝ ḷ ḹ ṃ ḥ ṅ ṭ ḍ ṇ ṣ missing). Licence file: "Personal Use Only". Used: Book, Book Italic, Medium, Bold.
Added (OFL, notofonts): Noto Sans Devanagari Regular/Bold (virāma, ZWJ, conjunct shaping via HarfBuzz), Noto Sans Kannada Regular/Bold, Noto Sans Regular/Bold (all IAST letters).
Mapping: `ui/Fonts.kt` splits each string into script runs; every text goes through `Sx()` (also text fields via `ScriptTransformation`, PDF via `PdfExport`). Rendering verified by Robolectric screenshots in `docs/screenshots` (Devanagari conjuncts, Kannada, IAST).

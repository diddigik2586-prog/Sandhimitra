# Font integration report (v1.0.4-sample)
Supplied and used:
* Sanskrit (Devanagari): Kohinoor Devanagari Regular / Medium / Semibold / Bold — 507 glyphs, full Devanagari block, virama/ZWJ present. Licence text not included in the supplied files.
* Kannada: Kannada MN (single weight, 204 glyphs, GSUB/GPOS present; conjuncts render correctly in screenshots). Copyright Murasu Systems Sdn. Bhd., "all rights reserved" — confirm redistribution rights. Bold is synthesised.
* English / IAST: Andika 7.000 Regular, Medium, SemiBold, Bold, Italic (SIL OFL; contains all IAST letters). Supplied in the zip.
Earlier Kohinoor Latin and Noto fonts are no longer bundled. Mapping: `ui/Fonts.kt` splits text into script runs (Devanagari -> Kohinoor Devanagari, Kannada -> Kannada MN, all else -> Andika); all UI text, text fields and PDF export use it. Verified by Robolectric screenshots (docs/screenshots).
Kohinoor Devanagari and Kannada MN contain no IAST dot-below letters; IAST therefore always uses Andika.

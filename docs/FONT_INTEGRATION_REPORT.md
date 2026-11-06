# Font integration report (v0.1)
* Sanskrit (Devanagari): Tiro Devanagari Hindi Regular — SIL Open Font License 1.1 (Tiro Typeworks / Indigo Project). Single weight; bold is synthesised.
* Kannada: Noto Sans Kannada Regular, Medium, SemiBold, Bold — SIL OFL 1.1 (Noto Project Authors).
* English / IAST: Andika Regular, Medium, SemiBold, Bold, Italic — SIL OFL 1.1 (SIL International); covers every IAST letter.
Licence texts: app/src/main/assets/licenses/ (shipped in the APK) and original_assets/fonts/release/.
Mapping: ui/Fonts.kt splits each string into script runs (Devanagari → Tiro, Kannada → Noto Sans Kannada, everything else → Andika); text fields and PDF export use the same mapping.
Earlier Kohinoor and Kannada MN files were removed from the app (their licences did not permit redistribution).

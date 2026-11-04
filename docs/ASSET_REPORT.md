# Asset report
| Supplied file | Role | Status |
|---|---|---|
| personal photograph 1536×1024 | About Dr. DVK | integrated unchanged (`drawable-nodpi/dvk_photograph.png`) |
| launcher icon 1254² | launcher + splash | splash uses exact PNG; launcher mipmaps are resized copies with the white corners made transparent |
| wordmark सन्धिमित्रम् 2172×724 | header + splash | exact bytes (`sanskrit_wordmark.png`) |
| support screenshot 1272×4027 | Support page | exact, uncropped; QR decoded with OpenCV → `upi://pay?pa=8970500450@kotakbank&pn=DIDDIGI VAMSI KRISHNA` (matches printed UPI ID) |
| About screenshot 1272×6497 | source of bio text | not displayed (other app's UI); kept in original_assets |
| kohinoor-font-family.zip | fonts | Latin only — see font report |
| Sandhimitra-source-updated.zip | legacy project | audited; replaced by modular code (kept in original_assets/legacy). It contained no dictionary data. |

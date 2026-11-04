# Source discrepancies and defects (recorded, not silently reconciled)
1. Sanskrit document has no section "2"; Kannada has "2. ಪ್ರಸ್ತಾವನೆ".
2. Kannada text uses Bengali ঙ্ (U+0999 U+09CD) 24× where Kannada ಙ್ is meant (e.g. "ಅದೇঙ್"). Display fix applied in `ContentRepo.fix`; stored data unchanged.
3. Stray Arabic letters in "एष आदिशः" example (sa, kn: "आديशः"); one Malayalam and one Telugu character each in sa/kn; jihvāmūlīya shown as "≍क ≍पौ" in sūtra 8.3.37. Left verbatim.
4. Sanskrit §11 states "आद्गुणः — अवर्णात् इकि परे" whereas §5 says "अचि"; both kept.
5. Topic 'छत्वसन्धिः'/'पूर्वसवर्णः': "उत्थम्भनम्" in §9 differs from the usual "उत्तम्भनम्"; engine implements only ud+sthā → utthā.
6. Topic 18 (श्चुत्व) §9 gives "विद् + नः = प्रश्नः", which is not a valid derivation of praśnaḥ; kept verbatim.
7. Several examples are labelled "ङमुडागम" while the sūtra is ṅamuṭ (8.3.32).
Please have these reviewed by the author.

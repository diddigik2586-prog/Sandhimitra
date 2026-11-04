import json, pathlib, re
ROOT = pathlib.Path(__file__).resolve().parents[1]
asset = ROOT / "app/src/main/assets/content/source_materials_working_extraction.json"
data = json.loads(asset.read_text(encoding="utf-8"))
assert data["sa"]["paragraph_count"] > 1000
assert data["kn"]["paragraph_count"] > 1000
assert data["sa"]["table_count"] > 50 and data["kn"]["table_count"] > 50
all_text = []
for lang in ("sa", "kn"):
    for block in data[lang]["blocks"]:
        if block["kind"] == "paragraph":
            all_text.append(block["text"])
        else:
            all_text.extend(cell for row in block["rows"] for cell in row)
joined = "\n".join(all_text)
assert not re.search(r"mahāmahopādhyāya|महामहोपाध्याय|ಮಹಾಮಹೋಪಾಧ್ಯಾಯ", joined, re.I)
source = (ROOT / "app/src/main/java/org/sandhimitra/app/MainActivity.kt").read_text(encoding="utf-8")
for colour in ("0xFFF0F2F8","0xFFFFFFFF","0xFF394B78","0xFFC17B55","0xFF28334D","0xFF171E30","0xFF262F49","0xFFD18B63","0xFFE6AE88"):
    assert colour in source, colour
assert "if (section == Section.VOWEL || section == Section.CONSONANT || section == Section.VISARGA)" in source
assert "delay(5_000)" in source
assert "VerifiedRules.lookup(key,direction)" in source
assert "fun lookup(key:String,direction:Int):String? = null" in source
assert "©Dr. Diddigi Vamsi Krishna" in source
print("PASS: source extraction, prohibited honorific exclusion, Indigo & Copper palette, capsule placement, five-second splash, safe empty rule registry and exact footer")

# -*- coding: utf-8 -*-
"""Build app content assets from the parsed Sanskrit/Kannada extraction + the English layer.
Usage: python3 -I build_content.py <parsed.json> <out_assets_content_dir>
"""
import json, os, re, sys, unicodedata
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from en_content import EN

LABELS = {  # section number -> (sa, kn, en)
 1: ("प्रकरणनाम", "ಪ್ರಕರಣದ ಹೆಸರು", "Topic"),
 2: ("प्रस्तावना", "ಪ್ರಸ್ತಾವನೆ", "Introduction"),
 3: ("स्वरूपम्", "ಸ್ವರೂಪ", "Nature of the rule"),
 4: ("सूत्राणि", "ಸೂತ್ರಗಳು", "Sūtras"),
 5: ("पदच्छेदः अर्थश्च", "ಪದಚ್ಛೇದ ಮತ್ತು ಅರ್ಥ", "Word analysis and meaning"),
 6: ("नियमविवरणम्", "ನಿಯಮ ವಿವರಣೆ", "Explanation of the rule"),
 7: ("प्रक्रियादर्शनम्", "ಪ್ರಕ್ರಿಯಾ ದರ್ಶನ", "Derivations"),
 8: ("उदाहरणानि", "ಉದಾಹರಣೆಗಳು", "Examples"),
 9: ("विकल्पः अपवादाश्च", "ವಿಕಲ್ಪ ಮತ್ತು ಅಪವಾದ", "Options and exceptions"),
 10: ("विशेषावधानानि", "ವಿಶೇಷ ಸೂಚನೆಗಳು", "Special notes"),
 11: ("स्मरणीयांशाः", "ನೆನಪಿಡಬೇಕಾದವು", "To remember"),
 12: ("अभ्यासः", "ಅಭ್ಯಾಸ", "Exercises"),
 13: ("स्वाध्यायप्रश्नाः", "ಸ್ವಾಧ್ಯಾಯ ಪ್ರಶ್ನೋತ್ತರಗಳು", "Self-study questions"),
 14: ("प्रकरणसारः", "ಪ್ರಕರಣಸಾರ", "Summary"),
}
CAPTION = re.compile(r'^(रेखाचित्रम्|ರೇಖಾಚಿತ್ರ)\b')
GROUPS = ['intro', 'vowel', 'consonant', 'visarga', 'prakriti', 'practice']
FORBIDDEN = ['महामहोपाध्याय', 'महोपाध्याय', 'ಮಹಾಮಹೋಪಾಧ್ಯಾಯ', 'ಮಹೋಪಾಧ್ಯಾಯ', 'Sandhini']

def short(t):
    return re.sub(r'\s*\(.*$', '', t).strip()

def clean_paras(ps):
    out = []
    for p in ps:
        if CAPTION.match(p): continue
        out.append(p)
    return out

def main(parsed, outdir):
    d = json.load(open(parsed, encoding='utf8'))
    os.makedirs(os.path.join(outdir, 'topics'), exist_ok=True)
    index = {'groups': {g: [] for g in GROUPS}, 'section_labels': {str(k): v for k, v in LABELS.items()}}
    dropped = 0
    anomalies = []
    for i in range(44):
        tid = 't%02d' % i
        en = EN[i]
        rec = {'id': tid, 'group': en['group'],
               'en': {'title': en['title'], 'overview': en['overview'],
                      'rules': [{'sutra': a, 'ref': b, 'gloss': c} for a, b, c in en['rules']],
                      'notes': en['notes']}}
        for lang in ('sa', 'kn'):
            t = d[lang][i]
            secs = []
            for s in t['sections']:
                ps = clean_paras(s['paras']); dropped += len(s['paras']) - len(ps)
                secs.append({'n': s['num'], 'p': ps})
            rec[lang] = {'title': short(t['title']), 'subtitle': t['title'], 'sections': secs,
                         'ex': t['ex_table'], 'pr': t['pr_table']}
        blob = json.dumps(rec, ensure_ascii=False)
        for w in FORBIDDEN:
            assert w not in blob, (tid, w)
        for ch in set(blob):
            n = unicodedata.name(ch, '')
            if ch in '≍' or 'ARABIC' in n:
                anomalies.append((tid, ch, n))
        json.dump(rec, open(os.path.join(outdir, 'topics', tid + '.json'), 'w', encoding='utf8'), ensure_ascii=False, separators=(',', ':'))
        index['groups'][en['group']].append({'id': tid, 'sa': rec['sa']['title'], 'kn': rec['kn']['title'], 'en': en['title'],
                                             'has_lp': en['group'] in ('vowel', 'consonant', 'visarga')})
    json.dump(index, open(os.path.join(outdir, 'index.json'), 'w', encoding='utf8'), ensure_ascii=False, indent=1)
    print('topics 44; caption paragraphs dropped:', dropped)
    print('source anomalies:', sorted(set(anomalies)))
    for g in GROUPS: print(g, len(index['groups'][g]))

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])

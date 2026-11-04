"""Parse the working extraction JSON into 44 topics x 14 sections (sa, kn).
Usage: python3 -I parse_sources.py <extraction.json> <out_parsed.json>
"""
import json, re, sys

MARK = {'sa': 'बृहत्सन्धिप्रकरणम्', 'kn': 'ಬೃಹತ್ಸಂಧಿಪ್ರಕರಣಮ್'}
DIG = str.maketrans('०१२३४५६७८९೦೧೨೩೪೫೬೭೮೯', '01234567890123456789')
HEAD = re.compile(r'^([०-९೦-೯]+)\.\s+(.*)$')

def parse(lang, blocks):
    marks = [i for i, b in enumerate(blocks) if b['kind'] == 'paragraph' and b['text'].strip() == MARK[lang]]
    tables = [b['rows'] for b in blocks if b['kind'] == 'table']
    topics = []
    marks_topic = marks[1:]  # first marker precedes the division title page
    for n, s in enumerate(marks_topic):
        e = marks_topic[n + 1] if n + 1 < len(marks_topic) else len(blocks)
        seg = [b for b in blocks[s + 1:e] if b['kind'] == 'paragraph']
        title = seg[0]['text'].strip()
        secs, cur = [], None
        for b in seg[1:]:
            t = b['text'].strip()
            m = HEAD.match(t)
            curnum = cur['num'] if cur else 0
            if m and len(t) < 160 and t.endswith(':') and curnum < int(m.group(1).translate(DIG)) <= curnum + 2:
                cur = {'num': int(m.group(1).translate(DIG)), 'heading': m.group(2), 'paras': []}
                secs.append(cur)
            elif cur is not None:
                cur['paras'].append(t)
            else:
                secs.append({'num': 0, 'heading': '', 'paras': [t]}); cur = secs[-1]
        topics.append({'title': title, 'sections': secs,
                       'ex_table': tables[2 * n] if 2 * n < len(tables) else None,
                       'pr_table': tables[2 * n + 1] if 2 * n + 1 < len(tables) else None})
    return topics

if __name__ == '__main__':
    d = json.load(open(sys.argv[1], encoding='utf8'))
    out = {l: parse(l, d[l]['blocks']) for l in ('sa', 'kn')}
    json.dump(out, open(sys.argv[2], 'w', encoding='utf8'), ensure_ascii=False)
    for l in out:
        print(l, len(out[l]), [len(t['sections']) for t in out[l]][:50])

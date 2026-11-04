# -*- coding: utf-8 -*-
"""Build lexicon.db from Monier-Williams (Cologne csl-orig, CC BY-SA 4.0) headwords
plus a closed-class list and forms attested in the supplied grammatical documents.
Usage: python3 -I build_lexicon.py <mw.txt> <content_dir> <out.db> <report.json>
Everything is stored in SLP1 (one ASCII char per phoneme)."""
import json, re, sqlite3, sys, os, glob, collections

mw, content, out, report = sys.argv[1:5]
CAT = {'m.': 'm', 'f.': 'f', 'n.': 'n', 'mfn.': 'a', 'ind.': 'i', 'nf.': 'f', 'fn.': 'n', 'm.f.': 'm'}
heads = collections.OrderedDict(); cur = None
for line in open(mw, encoding='utf8'):
    m = re.match(r'<L>[^<]*<pc>[^<]*<k1>([^<]*)<k2>', line)
    if m:
        cur = m.group(1); heads.setdefault(cur, 'x'); continue
    m2 = re.search(r'<lex>([^<]*)</lex>', line)
    if cur and m2 and heads.get(cur) == 'x' and m2.group(1) in CAT:
        heads[cur] = CAT[m2.group(1)]

# --- Devanagari -> SLP1 (for attested forms in the supplied documents)
V = {'अ':'a','आ':'A','इ':'i','ई':'I','उ':'u','ऊ':'U','ऋ':'f','ॠ':'F','ऌ':'x','ॡ':'X','ए':'e','ऐ':'E','ओ':'o','औ':'O'}
M = {'ा':'A','ि':'i','ी':'I','ु':'u','ू':'U','ृ':'f','ॄ':'F','ॢ':'x','ॣ':'X','े':'e','ै':'E','ो':'o','ौ':'O'}
C = dict(zip('कखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसह', 'kKgGNcCjJYwWqQRtTdDnpPbBmyrlvSzsh'))
C['ळ'] = 'L'
def d2s(s):
    o = []; i = 0
    while i < len(s):
        ch = s[i]
        if ch in C:
            o.append(C[ch]); n = s[i+1] if i+1 < len(s) else ''
            if n == '्': i += 2; continue
            if n in M: o.append(M[n]); i += 2; continue
            o.append('a'); i += 1; continue
        if ch in V: o.append(V[ch])
        elif ch == 'ं': o.append('M')
        elif ch == 'ः': o.append('H')
        elif ch == 'ऽ': o.append("'")
        elif ch == 'ँ': o.append('~')
        else: return None
        i += 1
    return ''.join(o)

attested = set()
for f in sorted(glob.glob(os.path.join(content, 'topics', '*.json'))):
    rec = json.load(open(f, encoding='utf8'))
    tbl = rec['sa'].get('ex') or []
    for row in tbl[1:]:
        for cell in row[:2]:
            for w in re.split(r'[+→,=/()\s]+', cell):
                w = w.strip("'‘’")
                if w and re.fullmatch(r'[ऀ-ॣ]+', w):
                    s = d2s(w)
                    if s: attested.add(s)
closed = {
 'upasarga': 'pra parA apa sam anu ava nis nir dus dur vi A ni aDi api ati su ud aBi prati pari upa'.split(),
 'avyaya': 'iti eva api atra tatra yatra kutra iha ca vA na hi tu atha aTa yadi tadA yadA ekadA sadA sarvadA'.split(),
}
con = sqlite3.connect(out)
con.executescript('''PRAGMA journal_mode=OFF; CREATE TABLE lex(k1 TEXT PRIMARY KEY, g TEXT NOT NULL) WITHOUT ROWID;
CREATE TABLE attested(f TEXT PRIMARY KEY) WITHOUT ROWID; CREATE TABLE closed(f TEXT PRIMARY KEY, c TEXT) WITHOUT ROWID;
CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT) WITHOUT ROWID;''')
con.executemany('INSERT INTO lex VALUES(?,?)', heads.items())
con.executemany('INSERT OR IGNORE INTO attested VALUES(?)', [(a,) for a in sorted(attested)])
cl = [(w, c) for c, ws in closed.items() for w in ws]
con.executemany('INSERT OR IGNORE INTO closed VALUES(?,?)', cl)
cnt = collections.Counter(heads.values())
meta = {'mw_source': 'Monier-Williams Sanskrit-English Dictionary (1899), digitisation by Cologne Digital Sanskrit Dictionaries (sanskrit-lexicon/csl-orig), CC BY-SA 4.0',
        'headwords': str(len(heads)), 'attested_forms': str(len(attested)), 'closed_class': str(len(cl))}
con.executemany('INSERT INTO meta VALUES(?,?)', meta.items()); con.commit(); con.execute('VACUUM'); con.close()
json.dump({'headwords': len(heads), 'by_category': cnt, 'attested_forms_from_documents': len(attested), 'closed_class_entries': len(cl)},
          open(report, 'w'), indent=1)
print(len(heads), dict(cnt), len(attested), os.path.getsize(out))

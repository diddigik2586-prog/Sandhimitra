# Content source map
* `original_assets/legacy/source_materials_working_extraction.json` — paragraph/table extraction of the supplied Sanskrit (`sa`) and Kannada (`kn`) documents; 44 topics × 14 sections (Sanskrit lacks section 2; the Kannada "Prastāvanā" corresponds to the long commentary paragraph inside Sanskrit section 1).
* Each topic's two tables (examples, exercises) follow all paragraphs in the extraction and are paired by order (pair k ↔ topic k).
* Removed from display: document titles, invocation line, 44 diagram captions ("रेखाचित्रम्…") whose diagrams are not in the extraction.
* English: composed independently per topic (`en_content.py`); derivations/examples in English mode are the Sanskrit source in IAST.
* Rules: sūtra numbers follow the Sanskrit document; 129 name→number pairs were extracted and each registry rule checked against them.

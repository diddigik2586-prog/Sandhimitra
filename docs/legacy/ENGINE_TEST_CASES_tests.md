# Initial exact-match regression cases

These are smoke-test cases for the deliberately limited initial lookup. They are not a claim that the engine implements the full grammar.

| Direction | Input | Expected |
|---|---|---|
| Yojana | दधि + अत्र | दध्यत्र; इको यणचि (6.1.77) |
| Yojana | विद्या + आलयः | विद्यालयः; अकः सवर्णे दीर्घः (6.1.101) |
| Yojana | उप + इन्द्रः | उपेन्द्रः; आद्गुणः (6.1.87) |
| Yojana | सत् + चित् | सच्चित्; स्तोः श्चुना श्चुः (8.4.40) |
| Viccheda | दध्यत्र | दधि + अत्र |
| Viccheda | विद्यालयः | विद्या + आलयः |
| Viccheda | उपेन्द्रः | उप + इन्द्रः |
| Viccheda | सच्चित् | सत् + चित् |
| Either | Unsupported input | No output; display no-verified-rule dialog |

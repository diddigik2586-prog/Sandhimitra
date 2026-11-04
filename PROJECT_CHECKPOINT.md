# Sandhimitra — Project Checkpoint
**Date:** 04 November 2026  
**State:** Active implementation; no APK build claimed.

## Specification authority
The user's latest document titled “Master Application Specification” is authoritative. It explicitly sets the **Indigo & Copper** palette and supersedes the previous Rosewood palette. The latest specification also locks the supplied icon, five-second splash, three interface languages, exact global footer, six homepage cards and the Lesson/Practice navigation restriction.

## Changes made in this checkpoint
- Replaced Rosewood colour constants with the exact Indigo & Copper light/dark values.
- Changed splash progress to a determinate five-second progress bar; no loading text is shown.
- Added local persistence for interface language, theme and font size.
- Added menu entries for About App, About Dr. DVK, Support Sandhimitra and Feedback.
- Added English-only About App, About Dr. DVK and Support pages.
- Added Feedback mail action to `dvkrishna92@gmail.com`.
- Kept the current app icon assets; no new icon generated.
- Disabled the earlier demonstration sandhi outputs because they were not yet supported by a fully audited rule registry.
- Distinguished incomplete input from an input that simply has no entry in the current verified registry. The latter is not automatically labelled grammatically impossible.
- Extracted Sanskrit and Kannada source DOCX paragraphs and tables into `app/src/main/assets/content/source_materials_working_extraction.json`.
- Added `docs/CURRICULUM_COVERAGE.json` as a checklist, not a claim of lesson completion.

## Source integrity
The two source documents are preserved as working editorial inputs. Their titles are not placed in the app UI. The extraction asset is not final lesson content: structure, rule mapping, sūtra references, exceptions and vikalpas require editorial review before publication. Prohibited honorific wording must remain excluded from app-facing content.

## Missing user-supplied assets/details
- Exact About Dr. DVK photograph not found in the current Android source archive.
- Confirmed payment/UPI details not supplied in the current build inputs; payment UI must remain non-transactional until confirmed.
- The supplied app icon is retained from the source archive; do not redraw or replace it.

## Build and verification
- Android SDK and Gradle executable are not installed in this execution environment.
- No Gradle wrapper JAR is present in the source checkpoint.
- APK compilation, installation and physical-device testing remain pending.
- Source-level checks can be run locally using `python tests/verify_source_assets.py`.

# AI Document Organizer — Internship Portfolio Version

## Implemented architecture and portfolio features

- Jetpack Compose + Material 3 UI
- Room persistence with user-scoped documents and migrations
- Repository + ViewModel separation
- CameraX scanning and ML Kit OCR
- Offline document classification
- Document intelligence card: summary + detected amounts/dates
- Search across title, OCR text, category and custom tags
- Time/category/expiry filters
- Favorites and sorting (newest, oldest, A-Z, expiry)
- Rename, edit OCR text, delete, share and PDF export
- Document categories and expiry reminders with WorkManager
- Firebase Authentication
- Translation and language identification
- Instrumentation/unit-test structure
- Multi-page document scanning with combined OCR
- Offline "Ask this document" quick-answer engine with amount/date/category/page/summary extraction
- Editable custom tags and favorite controls on document detail
- Page-count tracking and multi-image storage

## Architecture and UX upgrades

- Central Navigation Compose graph with route definitions in `navigation/`.
- Jetpack DataStore for appearance and reminder preferences.
- Three-state appearance setting: System, Light and Dark.
- Explicit Firebase Cloud backup for OCR/metadata and scanned pages.
- Firebase Firestore/Storage ownership rules included under `firebase/`.
- Centralized on-device document intelligence and Q&A engine.
- More interactive document detail with favorites and editable tags.

## Deliberately excluded

- Biometric authentication / app lock is not part of this internship version.

The current DocumentInsightsEngine is intentionally offline and deterministic; it should not be presented as a remote LLM.

## Latest UX upgrades

- Gallery import now supports selecting multiple images in one picker session.
- Camera and gallery use the same multi-page OCR pipeline; one image still works normally.
- Rename validation excludes the current document ID, so keeping the existing title is allowed.
- Duplicate names are still blocked case-insensitively against other documents belonging to the same user.
- Rename errors are shown inline instead of silently failing.

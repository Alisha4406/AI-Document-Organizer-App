# Internship Update 

## Implemented

- Navigation Compose is now the route/state mechanism via `navigation/AppNavGraph.kt`.
- Removed the obsolete duplicate navigation/welcome implementation.
- Added Jetpack DataStore for appearance/reminder preferences.
- Added System / Light / Dark appearance selection.
- Search now includes custom document tags.
- Document detail supports favorite toggling and editable comma-separated tags.
- Centralized deterministic on-device `DocumentQuestionEngine`.
- Expanded document intelligence with labeled fields, amounts, dates and richer summaries.
- Added Firebase Firestore + Storage cloud backup initiated explicitly by the user.
- Added Firebase ownership rules examples in `firebase/`.
- Added unit tests for classification, intelligence extraction and document Q&A.
- Bumped app version to 1.2.0.

## Existing features preserved

CameraX scanning, multi-page scanning, ML Kit OCR/preprocessing, Room migrations,
Firebase Authentication, translation/language detection, expiry reminders,
PDF export/sharing, rename/edit/delete, search/filter/sort, favorites, categories,
and the existing Compose dashboard.

## Build verification

The pure Kotlin intelligence sources compile successfully in the available
environment. A complete Gradle Android build could not be executed because the
sandbox has no network access and the Gradle 9.5 distribution/dependencies were
not locally cached.

## Firebase cloud backup setup

Enable Firestore and Storage in the Firebase project and deploy restrictive
authenticated-user rules. See `FIREBASE_SETUP.md` and the `firebase/` directory.



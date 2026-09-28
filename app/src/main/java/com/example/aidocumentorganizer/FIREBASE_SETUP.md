# Firebase Auth setup

The app is now wired to use **Firebase Authentication (Email/Password)**
for the Login and Sign Up screens (`data/auth/AuthManager.kt`). The code
is ready, but it needs to point at *your own* Firebase project — I can't
generate real Firebase credentials for you since they're tied to a
Google account and project you control.

`app/google-services.json` currently contains **placeholder values**
(`REPLACE_WITH_YOUR_...`). The project will build with it in place, but
every sign-up/login call will fail at runtime until you swap in your
real file. Here's how:

## 1. Create a Firebase project
1. Go to <https://console.firebase.google.com/> and click **Add project**.
2. Give it any name, finish the wizard (Google Analytics is optional).

## 2. Register the Android app
1. In the project overview, click the **Android icon** to add an app.
2. **Package name** must match this project exactly:
   `com.example.aidocumentorganizer`
3. Nickname and SHA-1 are optional (SHA-1 only needed later for Google
   Sign-In, not required for email/password).
4. Click **Register app**.

## 3. Download the real config file
1. On the next step, download **`google-services.json`**.
2. Replace the placeholder file in this project at:
   `app/google-services.json`
3. Skip the "Add Firebase SDK" code snippets — that's already done in
   `app/build.gradle.kts`.

## 4. Turn on Email/Password sign-in
1. In the Firebase console, go to **Build → Authentication**.
2. Click **Get started**, then open the **Sign-in method** tab.
3. Enable **Email/Password** and save.

## 5. Rebuild and run
Once the real `google-services.json` is in place, `Login` and `Sign Up`
will create/authenticate real users under **Authentication → Users** in
the Firebase console, and sessions persist automatically between app
launches (`AuthManager.isLoggedIn()` checks `FirebaseAuth.currentUser`).

## Notes
- Passwords are handled entirely by Firebase — this app never stores or
  sees the raw password beyond the sign-in call itself.
- `AuthManager.currentUserName()` reads the display name set at sign-up;
  `currentUserEmail()` reads the account email.
- If you also want Google/Sign in with Apple, password reset emails, or
  email verification, those are a few extra calls on `FirebaseAuth` and
  I'm happy to add them.


## Cloud Backup (optional but included)

The app keeps Room as its offline source of truth. The **Cloud backup** action
uploads the signed-in user's document metadata/OCR to Cloud Firestore and the
original scanned pages to Firebase Storage.

Before using it in a Firebase project:

1. Enable **Cloud Firestore**.
2. Enable **Firebase Storage**.
3. Deploy rules that restrict both services to the authenticated user's UID.
4. Do not use open `allow read, write: if true` rules.

Recommended ownership model:

- Firestore: `users/{uid}/documents/{documentId}`
- Storage: `users/{uid}/documents/{documentId}/page_N.jpg`

The client deliberately performs backup only after the user taps **Back up**.

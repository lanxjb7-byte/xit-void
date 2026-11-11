# XIT VOID — Android Native Java

Package: `com.xit.voidapp`  
Minimum Android: 10 (API 29)  
Build: GitHub Actions / Gradle 8.9 / JDK 17

## Included

- Native Java Android UI with dark red theme.
- Firebase Anonymous Authentication.
- Firebase Realtime Database with client reads/writes denied.
- Callable Cloud Functions for key redemption, profile lookup and OWNER-only key generation.
- MEMBER and OWNER roles.
- One-key/one-install-session binding using Firebase Auth UID plus Android `ANDROID_ID`.
- Free Fire and Free Fire MAX launch buttons (only launch installed apps).
- App-private cache cleanup.
- Owner bootstrap key configured as a server secret, never embedded in the APK.

The AIMNING and DRAG HS controls are UI presets only. They do not automate aiming, inject input, modify Free Fire, or bypass anti-cheat. Optimize FPS is informational and does not claim to alter game internals.

## 1. Create Firebase project

1. Create a project at https://console.firebase.google.com/
2. Add an Android app with package `com.xit.voidapp`.
3. Download `google-services.json` and put it at `app/google-services.json` for local builds.
4. In Firebase Authentication, enable **Anonymous** sign-in.
5. Create a **Realtime Database**. Choose a region and copy its database URL if your project requires a non-default database URL. If the database is not the default instance, update `initializeApp()` in `functions/index.js` to include `databaseURL`.
6. Keep `database.rules.json` as supplied. Client-side database reads and writes are denied; Cloud Functions use Admin SDK.

## 2. Install Firebase CLI and deploy backend

Install Node.js 20, then:

```bash
npm install -g firebase-tools
firebase login
firebase use --add
cd functions
npm install
cd ..
firebase functions:secrets:set OWNER_BOOTSTRAP_KEY
```

When prompted, enter exactly:

```text
LANX-XITIX-A110
```

Keep this value private. Do not put it in Java, GitHub source, screenshots, or a public repository.

Deploy:

```bash
firebase deploy --only functions,database
```

The first deployment may ask to enable required Google Cloud APIs and billing. Cloud Functions deployment generally requires the Firebase project to be on the Blaze plan. Check current Firebase pricing before enabling billing; set budget alerts.

## 3. Configure GitHub Actions

1. Push this project to a GitHub repository named `XIT-VOID`.
2. Convert the downloaded `google-services.json` to one-line Base64:
   - Linux/macOS: `base64 -w 0 google-services.json`
   - PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("google-services.json"))`
3. In GitHub repository: Settings → Secrets and variables → Actions → New repository secret.
4. Name: `GOOGLE_SERVICES_JSON_BASE64`
5. Value: the Base64 output.
6. Push to `main` or open Actions → BUILD XIT VOID APK → Run workflow.
7. Download artifact `XIT-VOID-debug-apk`.

## 4. Local build

Place `google-services.json` in `app/`, then run:

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 5. Important device-binding behavior

A key is bound to the first Firebase anonymous-auth UID and Android ID that redeems it. It cannot be redeemed by another installation/device. Clearing app data or reinstalling creates a new anonymous UID, so the user may need the OWNER to issue a new key. This is intentional fail-closed behavior for this starter project.

`ANDROID_ID` is an app/device identifier, not hardware attestation; a modified/rooted client can spoof client-supplied values. For stronger production protection, enable Firebase App Check with Play Integrity and add an OWNER-only reset/revoke workflow with audit logs. Do not rely on client-side role checks: role enforcement is performed by Cloud Functions.

## 6. Troubleshooting

- `Default FirebaseApp is not initialized`: missing/incorrect `app/google-services.json`.
- `PERMISSION_DENIED` on login: check that Cloud Functions deployed, Anonymous Auth is enabled, and the bootstrap secret was set correctly.
- `FUNCTIONS_NOT_FOUND`: deploy functions in the `us-central1` region or update the app's `FirebaseFunctions.getInstance(...)` region to match.
- Key already bound: use a new key; this starter has no public device-reset endpoint.
- Free Fire button says not installed: install the corresponding game package from its official store.
- Build fails on GitHub: inspect the first Gradle error in Actions logs; ensure the Firebase JSON secret is valid Base64.

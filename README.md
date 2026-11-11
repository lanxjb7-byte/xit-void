# XIT VOID — Android Native Java (Realtime Database Version)

Package: `com.xit.voidapp`  
Minimum Android: 10 (API 29)  

**Versi ini TIDAK menggunakan Cloud Functions.**  
Semua logic key dilakukan langsung ke Firebase Realtime Database + Security Rules.

## Setup

1. Buat Firebase project & aktifkan **Anonymous Authentication**.
2. Buat **Realtime Database**.
3. Deploy rules dari `database.rules.json`:
   ```bash
   firebase deploy --only database
   ```
4. Download `google-services.json` → taruh di `app/`.
5. Build APK.

## Cara kerja

- Login: baca key → cek valid/expired/bound → bind ke UID + deviceId → simpan profile user.
- Generate key (OWNER): langsung tulis key baru ke `/keys`.
- Durasi key didukung (expiresAt).

**Keamanan lebih lemah** dibanding versi Cloud Functions. User yang paham bisa mencoba bypass. Gunakan hanya kalau tidak mau deploy Functions.
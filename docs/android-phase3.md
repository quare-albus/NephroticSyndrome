# Phase 3 — Android app

## Shared backend decision
The Android client uses the existing Supabase project and public schema:
- Supabase Auth + `profiles`
- `patient_records`
- `doctor_requests` and `doctor_patients`
- `notifications`
- `push_tokens` for Android FCM device tokens

Firebase Auth and Firestore are not the source of truth for the Android app. Firebase Cloud Messaging is retained for push delivery only.

## Notification behavior
- Android FCM token is stored against the signed-in patient's Supabase UUID.
- Push payload includes the notification ID; tapping opens the app to that notification.
- Notification details and acknowledgement are fetched/updated from Supabase under existing RLS.
- Patient-facing notification list uses the same rolling 10-day cutoff as the website.
- Notification content is not treated as emergency/urgent-care messaging.

## Setup remaining for push delivery
1. Apply migration `20261102090000_android_fcm_push_tokens.sql`.
2. Configure Firebase Android app with the package ID in this project and keep `google-services.json` current.
3. Configure a server-side FCM sender (Supabase Edge Function) using Firebase service-account credentials stored as secrets; never embed service-account credentials in the APK.
4. Build and test on a physical Android device: permission prompt (Android 13+), foreground/background/terminated delivery, token rotation, notification tap-through, and patient acknowledgement.

# IGOT-U Feature Expansion — Architecture

## Structure

```
IGot-U/
├── android/          Kotlin Android app
│   └── app/src/main/java/com/igotu/
│       ├── sos/          SOS engine (single state machine)
│       ├── voice/        Voice trigger provider abstraction + enrollment
│       ├── recording/    Emergency camera/audio recording
│       ├── privacy/      Privacy lockdown
│       ├── connections/  IGOT-U user connections
│       ├── messaging/    Multi-channel notification providers
│       ├── service/      Foreground services
│       ├── ui/           Activities + ViewModels
│       └── data/         Retrofit API service
├── backend/          Node.js / Express / PostgreSQL API
│   └── src/
│       ├── routes/       voice, recordings, connections, privacy, messaging
│       ├── services/     S3 storage (signed URLs)
│       ├── middleware/   JWT auth + ownership assertion
│       └── db/           Knex migrations
└── ARCHITECTURE.md
```

---

## 1. SOS Engine

All trigger methods (button, voice, hardware, notification) call `SosEngine.activate()`.
There is one SOS state machine. Each action (location, recording, push, WhatsApp) runs
in an independent coroutine under `SupervisorJob`. Failure of any single action does NOT
cancel the SOS event or other actions.

```
SOS TRIGGER (any method)
        │
        ▼
   SosEngine.activate()
        │
   ┌────┴────┬──────────┬──────────┐
   ▼         ▼          ▼          ▼
Location  Recording  Contacts   WhatsApp
   │         │          │          │
 (each independent — failure isolated)
```

---

## 2. Voice Trigger

### Architecture

`VoiceTriggerProvider` is an interface. `OnDeviceVoiceTriggerProvider` uses Android
`SpeechRecognizer`. The implementation can be replaced (e.g. with a TFLite wake-word
model) without changing `SosEngine` or `VoiceTriggerService`.

### Android Limitations (enforced — not bypassed)

| Requirement | Implementation |
|---|---|
| Background microphone requires foreground service | `VoiceTriggerService` with `foregroundServiceType="microphone"` |
| Persistent notification mandatory | Built in `VoiceTriggerService.buildNotification()` |
| Android 14+ enforces microphone type at runtime | Declared in manifest |
| SpeechRecognizer session timeout | Service restarts session on `onError` / `onResults` |
| Manufacturer battery kill (MIUI, EMUI, One UI) | Documented; user must grant unrestricted battery |

### Voice Profile Privacy

```
Enrollment audio
      │
      ▼ (in-memory only)
Float embedding (RMS per frame)
      │
      ▼
EncryptedSharedPreferences (AES-256-GCM)
      │
      ▼
Never uploaded to backend by default
```

Raw audio is discarded after embedding extraction. The backend `voice_profiles` table
stores only enrollment status metadata, not the embedding.

### False-Positive Protection

- Phrase confidence threshold (configurable sensitivity: LOW/MEDIUM/HIGH)
- Optional voice profile similarity check (cosine similarity, advisory only)
- Cooldown period between triggers
- Optional countdown with cancel
- All trigger events logged to `voice_trigger_events`

**Voice recognition is NOT a guaranteed identity mechanism.** The app communicates
this clearly in the UI.

---

## 3. Emergency Recording

### Android Limitations (enforced — not bypassed)

| Requirement | Implementation |
|---|---|
| Background camera requires foreground service | `SosService` with `foregroundServiceType="location\|microphone\|camera"` |
| System camera-in-use indicator cannot be hidden | Not attempted — OS indicator is respected |
| Screen-off recording | Possible while foreground service active; manufacturer restrictions documented |

### Upload Resilience

```
Recording → local encrypted file
                │
                ▼
         WorkManager job
         (CONNECTED constraint)
                │
         Network available?
         YES → upload chunks → server confirms
         NO  → job retried automatically (exponential backoff, max 5 attempts)
```

### Access Control

- `recording_access_grants` table controls per-user access
- `assertOwnership()` middleware prevents IDOR on all recording endpoints
- Access logged to `recording_access_logs`
- Media served via S3 pre-signed URLs (15-minute expiry) — never public URLs

---

## 4. Privacy Lockdown

### What IGOT-U can do (legitimate Android APIs only)

| Mode | Capability | Available |
|---|---|---|
| Notification privacy | Set IGOT-U notification channel visibility | ✅ |
| Recents protection | `FLAG_SECURE` on EmergencyStatusActivity | ✅ |
| Guided settings | Open `Settings.ACTION_SECURITY_SETTINGS` | ✅ |
| Direct app lock | Lock another app | ❌ — requires device-owner |
| App hide/uninstall | Hide or remove another app | ❌ — requires root / device-owner |

The UI clearly states: *"Android may restrict some application-control features."*
`LockdownResult.actions` carries a `limitation` string for every action that could
not be completed, so the UI never falsely claims success.

---

## 5. Connections

- Bidirectional connection rows created on accept
- Default permissions: SOS alerts only — location NOT granted automatically
- Permissions revocable immediately via `PATCH /connections/:id/permissions`
- Invitation tokens: 48-hour expiry, single-use, contain no PII
- QR code data = token only

---

## 6. WhatsApp Integration

### What is implemented (legitimate only)

| Feature | Method |
|---|---|
| Send emergency alert | Android share intent → user confirms |
| Invite contact | Share URL containing short-lived token |
| Automated background send | NOT implemented — requires Meta WhatsApp Business API approval |

`WhatsAppProvider` implements `MessagingProvider`. When WhatsApp Business API
approval is obtained, a `WhatsAppBusinessProvider` can be added behind the same
interface without changing `MessagingOrchestrator`.

---

## 7. Messaging Provider Abstraction

```
MessagingOrchestrator
        │
   ┌────┼────┬────┐
   ▼    ▼    ▼    ▼
Push  IGOT-U WA  SMS
```

Each provider implements `MessagingProvider`. `DeliveryResult.status` is set to
`SENT` (not `DELIVERED`) for channels where delivery confirmation is unavailable.
`DELIVERED` / `READ` are only set when the underlying provider confirms them.

---

## 8. Security

- All recording endpoints: `assertOwnership()` — prevents IDOR
- Recordings served via signed URLs only — never public S3 URLs
- S3 objects encrypted server-side (AES256)
- Voice embeddings: EncryptedSharedPreferences on-device, never uploaded
- JWT auth on all API routes
- Rate limiting: 60 req/min default, 10 req/min on enrollment and recording endpoints
- `helmet()` sets security headers

---

## 9. Permissions Required (Android)

| Permission | Purpose |
|---|---|
| `RECORD_AUDIO` | Voice trigger + audio recording |
| `CAMERA` | Emergency video recording |
| `ACCESS_FINE_LOCATION` | SOS location sharing |
| `FOREGROUND_SERVICE_MICROPHONE` | Background voice listening |
| `FOREGROUND_SERVICE_CAMERA` | Background camera recording |
| `FOREGROUND_SERVICE_LOCATION` | Background location during SOS |
| `POST_NOTIFICATIONS` | Android 13+ notification permission |
| `RECEIVE_BOOT_COMPLETED` | Restart voice service after reboot |

No root, device-admin, accessibility-abuse, or device-owner permissions are used.

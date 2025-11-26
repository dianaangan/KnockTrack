# KnockTrack

KnockTrack is an Android application built with Kotlin that turns a smart doorbell into a proactive notification hub. It listens for doorbell events coming from Firebase Realtime Database, shows real‑time alerts, and keeps users informed with analytics and history.

## Features

- **Firebase Authentication** – Login/registration flow with improved UX, validation, and clearer error feedback.
- **Doorbell Notifications** – Global alert manager that sends phone notifications, prevents duplicates, and respects Android 13+ notification permissions.
- **Real-time Updates** – Home and History screens subscribe to Firebase events so new doorbell presses appear instantly.
- **History Management** – RecyclerView with per-item delete, `Clear All` action, and analytics summary (total/today/week stats).
- **Settings Controls** – Connect/reset doorbell credentials, toggle editable states, and enforce connection/notification status with visual cues.
- **Consistent UI** – Minimalist sans-serif inputs, shared button styles (`Sign Out`, `Clear All`, landing buttons, etc.), improved spacing, and better state messaging across all screens.
- **MVP Architecture** – Views handle UI, Presenters contain logic, Models talk to Firebase/SharedPreferences, and utilities provide global alert handling.

## Tech Stack

- **Language:** Kotlin
- **Architecture:** Model–View–Presenter (MVP)
- **Firebase:** Authentication + Realtime Database
- **UI:** XML layouts with custom drawables (rounded cards, analytics tiles, alert banners)
- **Coroutines:** For async Firebase calls and analytics calculations
- **Notifications:** `NotificationCompat`, channels, and SharedPreferences-backed duplicate prevention

## Project Structure (high level)

```
app/src/main/java/com/knocktrack/knocktrack/
├── adapter/                 # RecyclerView adapters (doorbell history)
├── model/                   # Business/data models (HomeModel, DoorbellModel, HistoryModel, etc.)
├── presenter/               # MVP presenters (LoginPresenter, HomePresenter, ...)
├── utils/                   # GlobalAlertManager, GlobalFirebaseListener, helpers
└── view/                    # Activities implementing the View interfaces
```

Resources live in `app/src/main/res/` and include layouts for each screen plus drawable assets (icons, buttons, borders).

## Getting Started

1. **Clone the repo:**
   ```bash
   git clone https://github.com/dianaangan/KnockTrack.git
   ```
2. **Open in Android Studio** (Giraffe+). Gradle sync should install dependencies automatically.
3. **Firebase Setup:**
   - Add your `google-services.json` to `app/` if not already present.
   - Ensure the Firebase project has Authentication and Realtime Database configured with the expected schema (`devices/{deviceId}/events`).
4. **Run the app** on an emulator or physical device with internet access.

## Development Notes

- `BaseActivity` wires the global Firebase listener and notification permissions for all screens.
- Home/logout navigation uses task-clearing flags so users cannot back-navigate to pre-login screens.
- Settings inputs change state/appearance when a device is connected vs. reset, relying on SharedPreferences per user.
- Analytics on the Home screen shares logic with History (`HistoryModel.getDoorbellAnalytics`).

## Contributing

1. Create a feature branch (`git checkout -b feat/my-change`).
2. Commit your updates with clear messages.
3. Push and open a pull request describing the change, screenshots for UI tweaks, and any testing steps.

## License

This project is maintained by Diana Angan. Please contact the repository owner regarding licensing or reuse.


# KnockTrack

KnockTrack is a Kotlin-based Android application that transforms a smart doorbell into a proactive security assistant. The app listens to Firebase Realtime Database events, issues mobile notifications, and surfaces actionable analytics so homeowners stay informed anywhere.

## Highlights

- **Secure Access** – Firebase Authentication with validation, error surfacing, and guarded back navigation to keep sessions predictable.
- **Actionable Alerts** – Notification channel with sound/vibration, duplicate suppression, and Android 13+ permission handling managed by `GlobalAlertManager`.
- **Live Data Experience** – Home and History screens subscribe to Firebase listeners, ensuring new doorbell presses and analytics refresh without manual navigation.
- **History Intelligence** – RecyclerView with per-item deletion, `Clear All`, and aggregated metrics for all-time, today, and week-over-week activity.
- **Robust Settings Flow** – Connection state drives editable fields, greyed-out read-only controls, and consistent “Connect / Reset” interactions backed by SharedPreferences.
- **Consistent UI System** – Shared header sizes, minimalist typography, reusable rounded buttons, and bordered cards for recent activity items.

## Architecture & Stack

- **Pattern:** Model–View–Presenter (MVP)
- **Language:** Kotlin (Coroutines + AndroidX)
- **Backend:** Firebase Authentication + Realtime Database
- **UI Layer:** XML layouts with custom drawables for headers, analytics cards, buttons, and borders
- **Notifications:** `NotificationCompat` with dedicated channel creation and persisted suppression flags

```
app/src/main/java/com/knocktrack/knocktrack/
├── adapter/      # RecyclerView adapters (e.g., DoorbellEventAdapter)
├── model/        # Data + Firebase access (HomeModel, HistoryModel, etc.)
├── presenter/    # Business logic per feature (HomePresenter, LoginPresenter…)
├── utils/        # Cross-cutting concerns (GlobalAlertManager, Firebase listener)
└── view/         # Activities implementing MVP contracts
```

Resources live under `app/src/main/res/` and contain the activity layouts, drawable styles, and colors referenced across the UI.

## Getting Started

1. **Clone**
   ```bash
   git clone https://github.com/dianaangan/KnockTrack.git
   ```
2. **Open in Android Studio** (Giraffe or newer) and allow Gradle sync to finish.
3. **Configure Firebase**
   - Place your `google-services.json` inside `app/`.
   - Ensure the Firebase project exposes Authentication and a Realtime Database path such as `devices/{deviceId}/events`.
4. **Run** on an emulator or device running Android 8.0+ with internet access.

## Implementation Notes

- `BaseActivity` coordinates notification permissions and the global Firebase listener so every screen benefits from the same lifecycle handling.
- `GlobalAlertManager` persists notified event IDs per user to block repeat notifications after logout/login cycles.
- Navigation flows clear the activity stack on login/logout and override `onBackPressed()` to match the expected UX described by the product requirements.
- `HistoryModel` powers both historical lists and the analytics summary shown on the Home screen.

## Contribution Workflow

1. Create a feature branch (`git checkout -b feat/short-description`).
2. Commit changes with context-rich messages.
3. Push and open a pull request outlining functional changes, UI screenshots, and validation steps.

## License & Ownership

KnockTrack is maintained by Diana Angan. For licensing inquiries or reuse permissions, please contact the repository owner directly.


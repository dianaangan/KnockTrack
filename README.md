# KnockTrack 🔔

A smart doorbell companion app for Android that delivers real-time notifications, rich activity insights, and secure device control powered by Firebase.

![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-Giraffe+-3DDC84?logo=androidstudio&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Realtime%20DB-FFCA28?logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-Private-informational)

> KnockTrack turns each doorbell press into an actionable alert while keeping homeowners informed with a minimalist dashboard, analytics, and history controls—all backed by a clean MVP codebase.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Highlights](#-highlights)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Usage](#-usage)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [Notes](#-notes)
- [License](#-license)
- [Author](#-author)
- [Acknowledgments](#-acknowledgments)

---

## 🎯 Overview

KnockTrack is a Kotlin-based Android application that monitors a smart doorbell through Firebase Realtime Database. The app delivers phone notifications, mirrors events in real time on the Home and History screens, and offers lightweight analytics so users can quickly gauge recent activity.

### Key Highlights

- 🔐 Secure access via Firebase Authentication with guarded back navigation
- 📲 Mobile notifications with duplicate suppression and Android 13+ permission handling
- ⏱️ Real-time UI refresh across Home and History without manual reloads
- 🗂️ History management featuring per-item delete and `Clear All`
- ⚙️ Settings workflow that disables inputs when connected and enforces consistent button styles
- 🎨 Minimalist design language with shared headers, borders, and typography

---

## ✨ Features

### 🏠 Home Experience
- Welcome header with contextual status, connection indicator, and quick “Sign Out” action
- Analytics overview showing all-time, today, and current-week doorbell counts
- Recent Activity feed with bordered items, timestamps, and empty-state messaging

### 📜 History Insights
- RecyclerView list of all recorded doorbell presses
- Single-entry delete plus `Clear All` button styled consistently with primary actions
- Real-time updates as events arrive or are removed in Firebase

### ⚙️ Settings Control
- Device ID and Auth Key fields that become read-only and greyed out when a device is connected
- “Connect” / “Reset” actions with guardrails to avoid accidental edits
- SharedPreferences persistence so states survive app restarts

### 🔔 Notification System
- `GlobalAlertManager` handles channel creation, vibration, and sound
- Duplicate prevention using persisted event IDs tied to the authenticated user
- Works whether the app is foregrounded or backgrounded

---

## 🛠️ Tech Stack

- **Language:** Kotlin + Coroutines
- **Architecture:** Model–View–Presenter (MVP)
- **Backend:** Firebase Authentication & Realtime Database
- **UI:** Android XML layouts with custom drawables and Material-inspired styling
- **Notifications:** Android `NotificationCompat` with channels and runtime permission checks

---

## 🏗️ Architecture

KnockTrack follows MVP to keep UI, logic, and data concerns isolated:

- **View (Activities):** `HomeActivity`, `HistoryActivity`, `SettingActivity`, etc.
- **Presenter:** Mediates between views and models, e.g., `HomePresenter`, `HistoryPresenter`.
- **Model:** Talks to Firebase and local storage (`HistoryModel`, `DoorbellModel`).
- **Utilities:** `GlobalAlertManager`, `GlobalFirebaseListener`, SharedPreferences helpers.

This structure keeps business rules testable and lets UI layers focus on rendering.

---

## 📦 Installation

### Prerequisites
- Android Studio Giraffe (or newer)
- Android SDK 33+
- JDK 17+
- A Firebase project with Authentication and Realtime Database enabled

### Steps

```bash
git clone https://github.com/dianaangan/KnockTrack.git
cd KnockTrack
```

1. Open the project in Android Studio and let Gradle sync.
2. Add your Firebase `google-services.json` to the `app/` directory.
3. Build and run on an emulator or physical device running Android 8.0+.

---

## ⚙️ Configuration

- **Firebase Database Path:** `devices/{deviceId}/events` (customize in models if needed).
- **SharedPreferences:** Stores connection details and notification history per user.
- **Notification Permissions:** Prompt handled in `BaseActivity` for Android 13+.
- **Environment Secrets:** Keep any API keys or service credentials outside of version control.

---

## 🚀 Usage

**For end users**
1. Log in via Firebase Authentication.
2. Connect a doorbell by entering a device ID and auth key in Settings.
3. Receive push notifications and view analytics plus recent history on the Home screen.
4. Review or delete individual entries in History, or clear the entire list when needed.

**For developers**
1. Adjust presenters or models to customize business logic.
2. Update drawables/layouts to refine the UI.
3. Extend analytics by enhancing `HistoryModel.getDoorbellAnalytics()`.

---

## 📁 Project Structure

```
app/src/main/java/com/knocktrack/knocktrack/
├── adapter/      # RecyclerView adapters (DoorbellEventAdapter, etc.)
├── model/        # Firebase + persistence logic
├── presenter/    # MVP presenters coordinating UI + data
├── utils/        # GlobalAlertManager, Firebase listeners, helpers
└── view/         # Activities implementing MVP contracts
```

Resources are located in `app/src/main/res/` and include layout XMLs, drawables, colors, and strings.

---

## 🤝 Contributing

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-improvement`).
3. Commit with descriptive messages.
4. Push and open a pull request describing changes, screenshots (for UI tweaks), and test steps.

Development guidelines:
- Keep MVP boundaries clear.
- Match the established design tokens (colors, typography, spacing).
- Add or update tests/presenter logic when touching Firebase interactions.

---

## 📝 Notes

- **Notification Duplication:** Prevented through persisted event IDs per user session.
- **Back Navigation:** Login/Home flows clear task stacks and override `onBackPressed()` for predictable UX.
- **Offline Behavior:** The app relies on Firebase; add caching if offline support is required.
- **Security:** Rotate credentials regularly and secure any shipped builds before distribution.

---

## 📄 License

This project is maintained privately by Diana Angan. Contact the owner for licensing or reuse discussions.

---

## 👤 Author

- **Diana Angan**
- GitHub: [@dianaangan](https://github.com/dianaangan)

---

## 🙏 Acknowledgments

- Built with Android best practices and Firebase’s real-time capabilities.
- Inspired by minimalist, utility-first UI patterns for smart-home apps.
- Thanks to everyone who provided feedback on UX and notification behavior.

---

⭐️ _If KnockTrack helps you build better doorbell experiences, consider starring the repository!_


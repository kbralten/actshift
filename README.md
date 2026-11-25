# ActShift

A gentle, local-first Behavioural Activation companion for Android.

ActShift helps you schedule small, doable activities and see how taking action shifts your mood—even before motivation arrives. Built around the Centre for Clinical Interventions (CCI) **Fun & Achievement** Behavioural Activation worksheet, it pairs low-friction scheduling with blind Before & After check-ins, an optional countdown timer, and complete privacy on your device.

---

## Highlights

- **Local-First & Private:** No accounts, sign-ups, analytics, ads, or cloud servers. Your scheduled activities, mood ratings, and reflection notes stay in a private database on your phone.
- **Action Before Motivation:** Designed around 5–15 minute micro-activities so getting started feels manageable on low-energy days.
- **Blind Before & After Check-Ins:** Rate **Depression / Low Mood**, **Pleasant Feelings**, and **Sense of Achievement** on the 0–8 clinical scale before and after an activity without anchoring on your earlier numbers, then see the shift revealed side-by-side.
- **Non-Punitive by Design:** No streaks, failure states, or guilt. Missed activities quietly move to an *Uncompleted / Reschedule* tray where you can reschedule them in one tap or dismiss them.
- **Dedicated Local Calendar (Optional):** Optionally syncs with a dedicated `"Behavioural Activation"` calendar on your device without touching or reading your personal or work calendars.

---

## How It Works

### 1. Plan Small, Balanced Activities
- **Today View:** See what is **In Progress**, **Next Up**, **Later Today**, and **Upcoming**, or tap **Schedule Activity** to add a new one.
- **Fun & Achievement Balance:** Tag activities as **Pleasure / Fun**, **Achievement**, or **Both / Mixed**.
- **Flexible Scheduling:** Pick a date, start time, duration, and optional recurrence (**Once**, **Daily**, **Weekdays**, or **Weekly** on specific days).

### 2. Blind Before & After Check-Ins
1. **Before You Start:** Rate your current **Depression / Low Mood**, **Pleasant Feelings**, and **Sense of Achievement** on the CCI 0–8 scale (`0 = Absolutely None` to `8 = Extreme`), and optionally start a countdown timer.
2. **In Progress Timer (Optional):** Stay anchored with a calm countdown screen and ongoing notification with **+5m** and **I'm Done** actions—or leave the timer off and check in whenever you finish.
3. **After Check-In:** Rate how you feel afterward without seeing your earlier numbers, plus an optional reflection note (*"What did you notice about yourself?"*).
4. **Before vs. After Reveal:** View your Before/After comparison and mood shift immediately after saving.

### 3. Gentle, Non-Punitive Reminders
- Receive start reminders at your scheduled time with one-tap **Snooze 30m** and **Move to Tomorrow** actions directly from the notification.
- If you logged a pre-activity check-in yesterday and never finished, ActShift gently asks if you completed it so you can either log how you felt or discard the entry.

### 4. Dedicated Device Calendar (Optional)
- During onboarding or in **Settings**, you can create a separate local calendar named **`Behavioural Activation`**.
- Your planned activities appear in your phone's calendar app while ActShift reads only that calendar, keeping your other calendars private and clutter-free.
- You can toggle calendar sync off at any time and run ActShift purely in its private local database.

### 5. Worksheet History & Appearance
- **Activity Log:** Browse all completed Before & After check-ins chronologically with category badges, score deltas, and reflection notes.
- **Appearance:** Choose **System**, **Light**, or **Dark** theme, plus optional **Dynamic Color** on Android 12+ to match your wallpaper instead of ActShift's default sage palette.

---

## Platform & Compatibility

- **OS Version:** Android 8.0 (API level 26) or higher.
- **Permissions (all optional):**
  - **Notifications & Exact Alarms:** Used only for scheduled activity reminders and active timer controls.
  - **Calendar (`READ_CALENDAR` / `WRITE_CALENDAR`):** Requested only if you choose to connect the dedicated local `"Behavioural Activation"` calendar.

---

## Building from Source

1. **Prerequisites:**
   - [Android Studio](https://developer.android.com/studio)
   - JDK 17 (Gradle toolchain is configured for Java 17)
   - Android SDK with Platform 36

2. **Clone & build the debug APK:**
   - **Linux / macOS:**
     ```bash
     git clone https://github.com/kbralten/actshift.git
     cd actshift
     ./gradlew assembleDebug
     ```
   - **Windows (PowerShell):**
     ```powershell
     git clone https://github.com/kbralten/actshift.git
     cd actshift
     .\gradlew.bat assembleDebug
     ```

3. **Run unit tests and lint:**
   ```bash
   ./gradlew testDebugUnitTest compileDebugAndroidTestKotlin lintRelease
   ```

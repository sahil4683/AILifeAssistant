# 🤖 AI Life Assistant — Setup Guide (Beginner Friendly)

---

## Step 1: Install Android Studio

1. Go to https://developer.android.com/studio
2. Download and install Android Studio
3. During setup, also install the Android SDK when it asks

---

## Step 2: Create a New Project

1. Open Android Studio
2. Click **"New Project"**
3. Choose **"Empty Views Activity"**
4. Fill in:
   - **Name:** AI Life Assistant
   - **Package name:** com.aiassistant
   - **Language:** Java
   - **Minimum SDK:** API 23 (Android 6.0)
5. Click **Finish**

---

## Step 3: Copy the Files

Copy each file from this project into your Android Studio project.
Match the folder structure exactly:

```
app/
  src/
    main/
      java/com/aiassistant/
        ├── MainActivity.java
        ├── NotificationService.java
        ├── AIClassifier.java
        ├── ClassificationResult.java
        ├── TaskItem.java
        ├── TaskDatabase.java
        ├── TaskAdapter.java
        ├── ActionEngine.java
        └── ReminderReceiver.java
      res/
        layout/
          ├── activity_main.xml
          └── item_task.xml
        values/
          ├── colors.xml
          └── styles.xml
        drawable/
          └── badge_bg.xml
      AndroidManifest.xml
  build.gradle
build.gradle
settings.gradle
```

### How to find the folders in Android Studio:
- On the left panel, click the dropdown that says **"Android"** and switch to **"Project"**
- Now you can see all folders clearly

---

## Step 4: Sync the Project

1. After copying all files, go to **File → Sync Project with Gradle Files**
2. Wait for the sync to complete (bottom bar shows progress)
3. Fix any errors shown in red (usually just missing imports — press Alt+Enter to auto-fix)

---

## Step 5: Run the App

### Option A: On a Real Phone (Recommended)
1. Enable **Developer Options** on your Android phone:
   - Go to Settings → About Phone → tap **Build Number** 7 times
2. Enable **USB Debugging** in Developer Options
3. Connect phone to PC via USB
4. In Android Studio, select your phone from the device dropdown
5. Click the **▶ Run** button

### Option B: On Emulator
1. In Android Studio, click **Device Manager** (right panel)
2. Create a new Virtual Device (Pixel 6, API 33)
3. Start it, then click **▶ Run**

---

## Step 6: Grant Permissions (IMPORTANT)

When the app opens:

1. **Notification Access** — A popup will appear. Tap OK, find "AI Life Assistant" in the list and toggle it ON
2. **Calendar Access** — Allow when prompted
3. **Battery Optimization** — Go to Settings → Battery → find the app → set to "Don't optimize"

---

## How the App Works

```
Your Phone Gets a Notification
        ↓
NotificationService.java reads it
        ↓
AIClassifier.java analyzes the text
  → Detects: Bill / Meeting / Reminder / Personal / Spam
  → Extracts: Amount, Date, Time
        ↓
TaskDatabase.java saves the task
        ↓
MainActivity.java shows it in the list
        ↓
You swipe or tap → Learning system records your action
```

---

## Testing Without Real Notifications

To test the app immediately:

1. Add this method to `MainActivity.java` temporarily:

```java
private void addTestTask() {
    TaskItem task = new TaskItem();
    task.setTitle("💰 Bill Due (₹1,200)");
    task.setOriginalMessage("Your electricity bill of ₹1,200 is due tomorrow. Please pay to avoid disconnection.");
    task.setCategory("bill");
    task.setAmount("₹1,200");
    task.setDate("tomorrow");
    task.setAppSource("SMS");
    task.setSuggestedAction("Set payment reminder");
    task.setPriority(1);
    task.setTimestamp(System.currentTimeMillis());
    task.setStatus(TaskItem.STATUS_PENDING);
    taskDatabase.insertTask(task);
    loadTasks();
}
```

2. Call `addTestTask();` inside `onCreate()` after `loadTasks();`
3. Remove it after testing

---

## Common Errors & Fixes

| Error | Fix |
|-------|-----|
| `Cannot resolve symbol 'R'` | Build → Clean Project, then Rebuild |
| `Gradle sync failed` | Check internet connection, retry sync |
| `App crashes on launch` | Check Logcat (bottom panel) for red error lines |
| `No tasks showing` | Make sure notification access is granted |
| `Notifications not detected` | Disable battery optimization for the app |

---

## Next Steps (Future Features)

- [ ] Add urgency scoring
- [ ] Daily digest notification (morning summary)
- [ ] Recurring pattern detection
- [ ] Personal finance spend tracker
- [ ] Widget for home screen

---

*Built with ❤️ — Offline. Private. Yours.*

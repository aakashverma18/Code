# Simple Todo Android App

This repository now includes a small native Android todo app in `app/`.

## Features

- Add todo items from a single input field.
- Mark tasks complete with checkboxes.
- Delete individual tasks.
- Clear all completed tasks.
- Persist tasks locally with `SharedPreferences` so the list survives app restarts.

## Project layout

- `settings.gradle` configures the Gradle project and repositories.
- `build.gradle` declares the Android Gradle plugin.
- `app/build.gradle` configures the Android app module.
- `app/src/main/java/com/example/simpletodo/MainActivity.java` contains the app UI and todo logic.

## Build

Install the Android SDK with API 35, then run:

```bash
gradle :app:assembleDebug
```

The debug APK will be generated under `app/build/outputs/apk/debug/`.

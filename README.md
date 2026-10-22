# Daymark — Android native student planner

Daymark is an offline-first Android app for student task management and class attendance.

## Features
- Add tasks with optional subject/category and due-date label.
- Mark tasks complete, delete tasks, and clear completed tasks.
- Track attendance by subject with Present, Absent, and Undo.
- See per-subject and overall attendance percentages.
- Saves data locally on the device. No account, ads, analytics, or internet permission.

## Build it using your phone with GitHub Actions
1. Download this ZIP and extract it with your phone's file manager.
2. Create a new repository on GitHub. A private repository is fine.
3. Upload the extracted project files and folders, including `.github/workflows/android-build.yml`.
4. Open the repository's **Actions** tab and select **Build Daymark Android App**.
5. Tap **Run workflow** (or push to `main` to trigger a build automatically).
6. When it completes, open the workflow run and download the `daymark-android-build` artifact.
7. Extract the artifact ZIP. It should contain a debug APK for phone testing and an unsigned release AAB.

## Google Play release
The unsigned AAB is not ready for Play Store upload. Before publishing:
- Configure release signing securely; never put passwords or keystore files in a public repository.
- Set a unique application ID if needed.
- Test the app on a real phone and fix any issues.
- Add app icon, screenshots, store listing, content rating, and privacy/Data safety declarations.
- Check Play Console's current target API and testing requirements.

## Important status
This project includes a GitHub Actions build workflow. The workflow has not been run from this environment, so a successful build is not yet verified. Play Store acceptance is not guaranteed.

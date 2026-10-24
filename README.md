# Daymark — Android native student planner

Daymark is an offline-first native Android app for student tasks and class attendance.

## Features
- Add tasks with an optional subject/category and due-date label.
- Mark tasks complete, delete tasks, and clear completed tasks.
- Track attendance by subject with Present, Absent, and Undo.
- View per-subject and overall attendance percentages.
- Stores data locally on the device. No account, ads, analytics, or internet permission.

## Build an APK using this repository
1. Open this repository on GitHub and select **Actions**.
2. Select **Build Daymark Android App**.
3. Tap **Run workflow** and confirm the `main` branch. A push to `main` also triggers the workflow.
4. Wait for the workflow to finish successfully.
5. Open the completed run and download the `daymark-android-build` artifact.
6. Extract the downloaded ZIP. It should contain the debug APK for phone testing and a release AAB.

If the workflow fails, open the failed run and inspect the first failed step and its logs. A build is not considered verified until the workflow succeeds and the APK is tested on a real device.

## Google Play release
The workflow's release AAB is not signed for publishing. Before publishing:
- Configure release signing securely; never commit passwords or keystore files to this repository.
- Confirm the application ID and version details.
- Test the app on a real phone and fix any issues.
- Prepare the app icon, screenshots, store listing, content rating, and privacy/Data safety declarations.
- Check Google Play Console's current target API and testing requirements.

## Current verification status
The source code and Android build workflow are in this repository. A successful build and real-device test still need to be confirmed. Google Play acceptance is not guaranteed.

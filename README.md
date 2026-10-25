# Sarah Miller Case — Deluxe Android Detective Game

A native Android/Kotlin, offline-first interactive detective thriller project. Package: `com.suchock.sarahmiller`.

## Implemented in this source build
- Animated transitions between screens and a cinematic dark dossier-style interface
- Start New Game, Continue Case, Settings, Case Archive
- Branching story scenes, evidence/clue collection and local save data
- In-game phone UI: messages, simulated calls/dialogue, offline map, camera/evidence archive, notes, voice recorder, case database
- Android Text-to-Speech for dialogue and recovered audio (voice availability depends on the phone's installed TTS engine/language)
- Sound toggle and short UI acknowledgement tones
- Optional side investigations: missing camera card, vanished witness, silver umbrella, Priya's recorder, evidence-room key
- Multiple endings and an evidence journal
- Offline gameplay; no account, network, API or server required
- Custom vector launcher icon
- GitHub Actions workflow to build a debug APK remotely

## Build APK
1. Install Android Studio and JDK 17.
2. Open this folder in Android Studio.
3. Install Android SDK Platform 35 when prompted and allow Gradle sync.
4. Run on an emulator or Android device.
5. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

This ZIP does not contain a compiled APK or Gradle wrapper JAR. This environment has no Android SDK/Gradle installed, so compilation and device testing have not been performed here.

## Build with GitHub Actions
Push this project to a GitHub repository. Open **Actions → Build Sarah Miller Case APK → Run workflow**. After a successful run, download the `Sarah-Miller-Case-Deluxe-APK` artifact from the workflow run. The workflow has not been executed by this project generator, so it must be checked for build errors.

## Important honest limitations
- Character voice is Android system Text-to-Speech, not studio-recorded character voice acting.
- In-game phone apps are fictional/simulated and do not access the real phone's contacts, calls, SMS, camera, or GPS.
- Sound effects are UI acknowledgement tones. A full layered rain/traffic/score soundscape and animated character cutscenes are not included yet.
- This is a 2D interactive narrative game, not a 3D open-world game.

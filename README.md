<div align="center">
  <h1>🚫 Friction Timer ⏳</h1>
  <p><i>Made by <a href="https://www.brunorochamoura.com/about/">BRM</a>.</i></p>
  <br />
</div>

**Friction Timer** is a minimal Android self-discipline app that introduces a short delay whenever you open specified apps, helping you resist impulsive usage.

This application helps reduce impulsive app usage by introducing a configurable delay when opening selected apps. It is designed around three core principles:

- **Friction:** Introduce a configurable delay before the app can be used.
- **Minimal UI:** Motivational messages and simple buttons—no fancy animations required.
- **Customizable:** Add any app you want to target, with independent timers and messages.

## Screenshots

<div align="center" style="display: flex; justify-content: space-around;">
  <img src="https://github.com/BrunoRochaDev/FrictionTimer/raw/main/screenshots/screenshot_1.jpg" width="30%" />
  <img src="https://github.com/BrunoRochaDev/FrictionTimer/raw/main/screenshots/screenshot_2.jpg" width="30%" />
</div>

## Where To Get

You can download the latest APK from the [Releases](https://github.com/BrunoRochaDev/FrictionTimer/releases) page, or build the app yourself by opening the project in Android Studio and selecting **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## How It Works

1. **App Detection:**  
   Uses an `AccessibilityService` to detect when any target app you’ve added is opened in the foreground.

2. **Overlay & Countdown:**  
   An overlay appears on top of the target app, preventing interaction until the countdown finishes. The overlay includes:
   - A configurable motivational message
   - A disabled "Wait X seconds" button that counts down
   - A "Cancel" button to dismiss the overlay

   Once the countdown ends, the button changes to "Proceed," allowing the user to interact with the app again.

3. **Cooldown & Reappearance:**  
   After proceeding, the overlay will reappear the next time the target app is opened **once the configurable cooldown period has elapsed**. This ensures ongoing friction without being permanently intrusive.

4. **Customizable Per App:**  
   Each target app can have its own wait time, cooldown duration, and list of motivational messages. All settings are accessible via the app’s settings screen.


## Thanks

Thanks to [digipaws](https://github.com/nethical6/digipaws) for the inspiration. Their source code served as a reference for the implementation of this app.

## License

This project is distributed under the AGPLv3 License. See the [LICENSE](https://github.com/BrunoRochaDev/FrictionTimer/blob/main/LICENSE) file for details.


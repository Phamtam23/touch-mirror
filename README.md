# Touch Mirror (Android / Kotlin MVP)

Experimental project for a phone with a dead upper third of the touchscreen. It draws a touch-capturing overlay over the lower third and maps that region onto the upper third. The overlay records a complete finger path and dispatches one synthetic Android accessibility gesture **after finger-up**.

## Important limitation

This is a proof-of-concept, not true real-time touch forwarding. Android's public `AccessibilityService.dispatchGesture()` API injects synthetic gestures; it does not expose a general API to forward every raw touch event from an overlay to another app in real time. Therefore a drag or swipe is replayed after the finger is lifted. This may be unsuitable for drawing, games, live dragging, or UI flows that require continuous feedback. Some secure/system screens may block injected gestures. Device-specific MIUI/HyperOS behavior must be tested on the phone.

## Build

Requirements: JDK 17, Android SDK Platform 35, Android Build Tools, and Gradle 8.7 (or Android Studio with Gradle sync). This environment did not include Flutter/Gradle/Android SDK tools, so no APK has been built or device-tested here.

1. Open this folder in Android Studio, or install the requirements above.
2. Sync Gradle.
3. Build > Build Bundle(s) / APK(s) > Build APK(s), or run `gradle assembleDebug` with Gradle 8.7.
4. Debug APK should be at `app/build/outputs/apk/debug/app-debug.apk`.

## Install and use

1. Install the APK and open Touch Mirror.
2. Tap the Accessibility settings button.
3. Enable Touch Mirror under downloaded/installed accessibility services.
4. Return to the app. The overlay should appear at the bottom third of the screen.
5. Touch or draw a short gesture in the bottom panel. On finger-up, Touch Mirror dispatches the mapped gesture to the top third.
6. Stop the overlay using the persistent notification action or the app's stop button.

## Coordinate mapping

The panel occupies the bottom third. X is scaled across the full display width. Y within the panel is scaled from 0..panelHeight to 0..screenHeight/3. This is a normalized 1:1 positional mapping between the two equal-sized regions, not identical absolute screen Y coordinates.

## Known MVP limitations

- Replays gestures only after release; not live passthrough.
- Multiple simultaneous fingers are not supported.
- Touching the bottom overlay does not operate the original bottom-screen UI while the overlay is enabled.
- Accessibility gestures may behave differently across Android versions and manufacturers.
- Screen rotations and unusual display cutouts have not been tested.

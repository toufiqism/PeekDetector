<<<<<<< HEAD
# PeekDetector

## Overview

PeekDetector is an Android application that uses machine learning to detect when multiple faces are present in the camera view. It's designed to alert users when someone might be "shoulder surfing" or looking at their device screen without permission.

The app runs as a foreground service, continuously monitoring the front camera for multiple faces. When more than one face is detected, it triggers both a visual overlay alert and a push notification to warn the user.

## Features

- **Real-time Face Detection**: Uses Google ML Kit Face Detection to identify faces in real-time
- **Foreground Service**: Runs in the background as a foreground service to continuously monitor
- **Multiple Alert Methods**:
  - Screen overlay (semi-transparent black screen) for immediate visual feedback
  - Push notifications to alert users when multiple faces are detected
- **Smart Notification Management**: 
  - 5-second cooldown to prevent notification spam
  - Separate notification channels for service status and alerts
- **Permission Handling**: Gracefully handles multiple Android permissions with user-friendly UI
- **Modern UI**: Built with Jetpack Compose following Material Design 3

## Architecture

The app follows SOLID principles and clean architecture patterns:

### Core Components

#### 1. MainActivity.kt
- Entry point of the application
- Handles permission requests (Camera, Notifications, Overlay)
- Provides UI to start/stop the detection service
- Uses Jetpack Compose for modern, reactive UI

#### 2. PeekDetectionService.kt
- Foreground service that manages camera lifecycle
- Coordinates face detection through CameraX
- Triggers alerts when multiple faces detected
- Manages notification display with cooldown logic

#### 3. PeekDetectorAnalyzer.kt
- Implements ImageAnalysis.Analyzer interface
- Processes camera frames using ML Kit Face Detection
- Reports number of detected faces via callback

#### 4. NotificationHelper.kt
- Utility class for managing all notification operations
- Creates and manages notification channels
- Handles foreground service notification
- Shows alert notifications with proper formatting
- Checks notification permissions for Android 13+

#### 5. ServiceLifecycleOwner.kt
- Custom LifecycleOwner implementation for the service
- Required for CameraX lifecycle binding in a service context

## Permissions Required

### Runtime Permissions

1. **CAMERA** (Required)
   - Used for face detection
   - Must be granted for app to function

2. **POST_NOTIFICATIONS** (Android 13+)
   - Required for showing alert notifications on Android 13 (API 33) and above
   - Optional - app will work without it but won't show notification alerts
   - On older Android versions, this permission is automatically granted

3. **SYSTEM_ALERT_WINDOW** (Required)
   - Allows drawing overlay on top of other apps
   - Used for screen overlay alert
   - Requires manual settings navigation on Android M+

### Other Permissions

4. **VIBRATE**
   - Used for notification vibration

5. **FOREGROUND_SERVICE** & **FOREGROUND_SERVICE_CAMERA**
   - Required for running camera in background
   - Automatically granted at install time

## How It Works

### Detection Flow

```
1. User grants required permissions
2. User starts protection service
3. Service initializes front camera with CameraX
4. Camera frames are analyzed by ML Kit Face Detector
5. When faces.size > 1:
   a. Log detection event
   b. Show screen overlay (semi-transparent, 3 seconds)
   c. Check notification cooldown (5 seconds)
   d. If cooldown passed and permission granted:
      - Show notification alert
      - Update last notification time
6. User can stop service anytime
```

### Notification Strategy

The app uses two separate notification channels:

1. **Peek Detection Service** (IMPORTANCE_LOW)
   - Persistent notification while service is running
   - Shows "Peek Protection is Active"
   - Minimal interruption to user

2. **Peek Detection Alerts** (IMPORTANCE_HIGH)
   - High priority for immediate attention
   - Shows when multiple faces detected
   - Includes vibration and LED
   - Auto-dismissible
   - 5-second cooldown to prevent spam

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Camera**: CameraX
- **Face Detection**: Google ML Kit Vision
- **Architecture Components**: 
  - Lifecycle
  - Compose Runtime (for state management)
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

## Dependencies

Key dependencies used in the project:

```kotlin
// CameraX
implementation("androidx.camera:camera-camera2")
implementation("androidx.camera:camera-lifecycle")
implementation("androidx.camera:camera-view")

// ML Kit Face Detection
implementation("com.google.mlkit:face-detection")

// Jetpack Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose")

// Core Android
implementation("androidx.core:core-ktx")
implementation("androidx.lifecycle:lifecycle-runtime-ktx")
```

## Setup and Installation

### Prerequisites

- Android Studio (latest stable version recommended)
- Android SDK with API 34
- Kotlin plugin
- Device or emulator with front camera
- Android device running Android 7.0 (API 24) or higher

### Building the App

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run on your device/emulator

```bash
./gradlew assembleDebug
```

### Installing

```bash
./gradlew installDebug
```

## Usage

1. **Launch the App**
   - Grant camera permission when prompted
   - Grant notification permission (Android 13+) when prompted
   - Grant overlay permission through system settings

2. **Start Protection**
   - Tap "Start Protection" button
   - Service will start and show persistent notification
   - Camera will begin monitoring in background

3. **When Multiple Faces Detected**
   - Screen will temporarily darken (3 seconds)
   - Notification alert will appear
   - Both alerts indicate potential shoulder surfing

4. **Stop Protection**
   - Tap "Stop Protection" button
   - Service will stop and release camera

## Privacy Considerations

- **No Data Storage**: The app does not store any images or facial data
- **Local Processing**: All face detection happens on-device using ML Kit
- **No Network**: No data is transmitted over the network
- **Minimal Data**: Only face count is tracked, no facial recognition or identification
- **Transparent Operation**: Persistent notification shows when service is active

## Customization

### Adjusting Alert Behavior

In `PeekDetectionService.kt`:

```kotlin
// Notification cooldown (milliseconds)
private val notificationCooldown = 5000L // Change to adjust frequency

// Overlay duration (milliseconds)
Handler(Looper.getMainLooper()).postDelayed({
    hideOverlay()
}, 3000) // Change to adjust overlay duration
```

### Changing Detection Threshold

In `PeekDetectorAnalyzer.kt`:

```kotlin
if (numFaces > 1) { // Change threshold here
    // Alert logic
}
```

### Notification Appearance

Customize notifications in `NotificationHelper.kt`:

```kotlin
// Change notification text, icons, vibration pattern, etc.
fun showMultipleFacesAlert(faceCount: Int) {
    // Customize notification builder here
}
```

## Recent Updates

### Version 1.1.0 (Current)

#### Added
- **Notification Alerts**: Push notifications when multiple faces detected
- **NotificationHelper Class**: Centralized notification management
- **Permission Handling**: 
  - Added POST_NOTIFICATIONS permission for Android 13+
  - Graceful permission request flow in MainActivity
  - Optional skip for notification permission
- **Smart Cooldown**: 5-second cooldown prevents notification spam
- **Dual Alert System**: Both overlay and notification alerts
- **Better Error Handling**: Null-safe operations and permission checks

#### Improved
- **Code Organization**: Separated notification logic into helper class
- **SOLID Principles**: Single responsibility for each component
- **User Experience**: 
  - Step-by-step permission requests
  - Clear messaging about each permission
  - Skip option for optional permissions
- **Documentation**: Comprehensive README and inline code documentation

#### Technical Details
- Notification channels properly configured for Android O+
- Permission checks for Android 13+ (TIRAMISU)
- PendingIntent flags updated for Android 12+
- Vibration patterns for alert notifications

## Known Limitations

1. **Front Camera Only**: Currently only monitors front camera
2. **Battery Usage**: Continuous camera usage impacts battery life
3. **False Positives**: May detect faces in photos/posters on screen
4. **Lighting Dependency**: Detection accuracy varies with lighting conditions
5. **Android Version**: Full notification features require Android 13+

## Future Enhancements

- [ ] Configurable detection threshold
- [ ] Detection history/statistics
- [ ] Power-saving modes
- [ ] Customizable alert sounds
- [ ] Whitelist for known faces
- [ ] Settings screen for user preferences
- [ ] Dark/Light theme support
- [ ] Localization support

## iOS Compatibility Notes

⚠️ **This is an Android-only application**

If you plan to port this to iOS, you'll need:

1. **Swift/SwiftUI Implementation**: Rewrite UI in Swift/SwiftUI
2. **AVFoundation**: Replace CameraX with AVFoundation for camera
3. **Vision Framework**: Replace ML Kit with iOS Vision framework for face detection
4. **Background Modes**: Request camera background mode permission
5. **Local Notifications**: Use UNUserNotificationCenter for alerts
6. **Different Permissions**: 
   - Camera access (NSCameraUsageDescription)
   - Notification permission (UNAuthorizationOptions)
   - No overlay permission equivalent (different approach needed)

## Troubleshooting

### Notifications Not Showing

1. Check if POST_NOTIFICATIONS permission is granted (Android 13+)
2. Verify notification channels are enabled in system settings
3. Check if Do Not Disturb mode is enabled
4. Ensure notification cooldown period has passed (5 seconds)

### Camera Not Working

1. Verify CAMERA permission is granted
2. Check if another app is using the camera
3. Restart the app
4. Ensure device has a working front camera

### Overlay Not Appearing

1. Grant SYSTEM_ALERT_WINDOW permission from system settings
2. Check if app has overlay permission in Settings > Apps > Special Access
3. Restart the service after granting permission

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Follow Kotlin coding conventions
2. Maintain SOLID principles
3. Add tests for new features
4. Update documentation
5. Handle errors gracefully
6. Check for null safety
7. Request appropriate permissions

## License

[Add your license here]

## Contact

[Add your contact information here]

## Acknowledgments

- **Google ML Kit**: For providing face detection capabilities
- **CameraX**: For simplified camera implementation
- **Jetpack Compose**: For modern UI development
- **Material Design**: For UI/UX guidelines

---

**Last Updated**: October 16, 2025  
**Version**: 1.1.0  
**Minimum Android Version**: 7.0 (API 24)  
**Target Android Version**: 14 (API 34)

=======
A peek detection service that notifies you when someone is peeking at your phone
>>>>>>> a31483791fa91e61d015e7ba830c0520980266b0

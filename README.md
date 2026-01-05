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
- **Detection Tracking & Statistics**:
  - Real-time counter showing total shoulder surfer detections
  - Persistent database storage of all detection events
  - Detailed detection history with timestamps and face counts
  - Visual indicators showing detection severity (2 faces ⚠️, 3+ faces 🚨)
- **Comprehensive Reporting System**:
  - **Weekly Reports**: View detections from the current week
  - **Monthly Reports**: View detections from the current month
  - **Yearly Reports**: View detections from the current year
  - Statistics dashboard showing total, average, and maximum faces detected
  - Beautiful tabbed interface for easy navigation between time periods
  - Data management with clear all data option
- **Automatic Report Export**:
  - Daily automatic export of detection reports to Downloads folder
  - Reports exported in multiple formats (JSON, CSV, and summary text)
  - All reports compressed into a single ZIP file
  - Background processing using WorkManager for efficient battery usage
  - Notification alerts when export completes or fails
  - No manual intervention required - runs automatically every 24 hours
- **Permission Handling**: Gracefully handles multiple Android permissions with user-friendly UI
- **Modern UI**: 
  - Built with Jetpack Compose following Material Design 3
  - Beautiful gradient background with subtle pattern overlay
  - Custom app icon with surveillance/security theme
  - Professional blue color scheme for security and trust
  - Responsive cards and intuitive layout

## Architecture

The app follows SOLID principles and clean architecture patterns:

### Core Components

#### 1. MainActivity.kt
- Entry point of the application
- Handles permission requests (Camera, Notifications, Overlay)
- Provides UI to start/stop the detection service
- Displays real-time detection counter
- Navigation to report screen
- Uses Jetpack Compose for modern, reactive UI

#### 2. PeekDetectionService.kt
- Foreground service that manages camera lifecycle
- Coordinates face detection through CameraX
- Triggers alerts when multiple faces detected
- Manages notification display with cooldown logic
- Saves detection events to database asynchronously

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

#### 6. ReportActivity.kt
- Dedicated activity for viewing detection reports
- Tabbed interface for weekly, monthly, yearly views
- Statistics dashboard with key metrics
- Detection history with chronological list
- Data management capabilities

#### 7. Data Layer (data/ package)
- **DetectionEvent.kt**: Entity class for database storage
- **DetectionEventDao.kt**: Data Access Object with query methods
- **AppDatabase.kt**: Room database configuration (Singleton)
- **DetectionRepository.kt**: Single source of truth for detection data
- Implements SSOT (Single Source of Truth) pattern
- Provides Flow-based reactive data streams

#### 8. Report Export System
- **ReportExportHelper.kt**: Utility class for exporting and zipping reports
  - Fetches all detection events from database
  - Creates JSON, CSV, and text summary reports
  - Zips reports into single archive
  - Saves to Downloads folder using MediaStore API
  - Handles cleanup of temporary files
- **ReportExportWorker.kt**: WorkManager worker for scheduled exports
  - Runs daily (every 24 hours) automatically
  - Shows progress, success, and failure notifications
  - Graceful error handling with retry logic
  - Battery-efficient background processing

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
   e. Save detection event to Room database with:
      - Face count
      - Timestamp
      - Auto-generated ID
   f. Update real-time counter on main screen
6. User can view reports at any time:
   - Weekly view: Current week detections
   - Monthly view: Current month detections
   - Yearly view: Current year detections
7. WorkManager automatically exports reports every 24 hours:
   - Fetches all detection events from database
   - Creates JSON, CSV, and summary reports
   - Zips all reports into single archive
   - Saves ZIP to Downloads folder
   - Shows notification when complete
8. User can stop service anytime
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
- **Database**: Room (SQLite)
- **Async Operations**: Kotlin Coroutines & Flow
- **Architecture Components**: 
  - Lifecycle
  - Compose Runtime (for state management)
  - Room Persistence Library
  - Flow for reactive data streams
- **Architecture Pattern**: SSOT (Single Source of Truth)
- **Design Principles**: SOLID, Clean Architecture
- **Minimum SDK**: API 33 (Android 13)
- **Target SDK**: API 36 (Android 15)

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

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

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
   - View total detections counter on main screen

2. **Start Protection**
   - Tap "Start Protection" button
   - Service will start and show persistent notification
   - Camera will begin monitoring in background
   - Detection counter updates in real-time

3. **When Multiple Faces Detected**
   - Screen will temporarily darken (3 seconds)
   - Notification alert will appear
   - Event is automatically saved to database
   - Detection counter increments
   - Both alerts indicate potential shoulder surfing

4. **View Reports**
   - Tap "View Reports" button on main screen
   - Navigate between Weekly, Monthly, Yearly tabs
   - View detailed statistics:
     - Total detections in period
     - Total faces detected
     - Average faces per detection
     - Maximum faces in single detection
   - Browse chronological detection history
   - See severity indicators (⚠️ for 2 faces, 🚨 for 3+ faces)

5. **Manage Data**
   - Tap delete icon in reports screen
   - Confirm to clear all detection history
   - Database will be reset to zero

6. **Automatic Report Export**
   - Reports are automatically exported every 24 hours to Downloads folder
   - No manual action required
   - Notification shown when export completes
   - ZIP file contains JSON, CSV, and summary text files
   - Find exported files in: `Downloads/PeekDetector_Reports_[timestamp].zip`

7. **Stop Protection**
   - Tap "Stop Protection" button
   - Service will stop and release camera
   - Historical data is preserved

## Privacy Considerations

- **No Image Storage**: The app does not store any images or facial data
- **Local Processing**: All face detection happens on-device using ML Kit
- **No Network**: No data is transmitted over the network
- **Minimal Data**: Only detection events stored (face count + timestamp)
- **No Facial Recognition**: No identification or biometric data collected
- **Local Database**: All data stored locally in Room database on device
- **User Control**: Clear all data option available in reports screen
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

## Design & UI

### App Icon
The app features a custom-designed icon that represents the core functionality:

- **Eye Symbol**: Central eye represents surveillance and monitoring
- **Security Frame**: Red corner brackets symbolize camera viewfinder and security scanning
- **Scanning Lines**: Horizontal lines create a tech/scanning effect
- **Color Scheme**:
  - Deep blue gradient background (#1A237E → #3949AB) represents trust and security
  - White eye with blue iris for clarity and visibility
  - Red accents for alert and attention

### Screen Background
The main screen features a professional multi-layer background:

- **Gradient Background**: Deep blue gradient (135° angle) creates depth and visual interest
  - Start: #0D47A1 (Dark Blue)
  - Center: #1565C0 (Medium Blue)
  - End: #1976D2 (Light Blue)
- **Pattern Overlay**: Subtle eye pattern at 5% opacity for texture without distraction
- **White Text**: All text and UI elements use white/light colors for excellent contrast

### UI Color Palette
- **Primary Actions** (Start Button): #4CAF50 (Green) - Go/Safe
- **Destructive Actions** (Stop Button): #FF5252 (Red) - Stop/Alert
- **Information** (Permission Buttons): #64B5F6 (Light Blue) - Info/Action
- **Status Active**: #4CAF50 (Green) - Service running
- **Status Inactive**: #FF5252 (Red) - Service stopped
- **Text**: White with varying opacity (0.7-1.0) for hierarchy

## Recent Updates

### Version 1.5.0 (Current)

#### Added - Battery Optimizations
- **Reduced Camera Resolution**: ImageAnalysis now uses 640x480 resolution for ~75% less processing
- **Frame Skipping**: Processes every 3rd frame (~10fps effective) reducing CPU usage by ~66%
- **Optimized Image Format**: YUV_420_888 format for efficient ML Kit processing
- **WorkManager Constraints**: Report export only runs when battery is not low
- **Improved Resource Management**: Better cleanup of camera executors and coroutines

#### Technical Improvements
- **ImageAnalysis Optimization**: Target resolution and output format configured for battery efficiency
- **Frame Throttling**: Intelligent frame skipping mechanism in analyzer
- **Battery-Aware Background Tasks**: WorkManager constraints prevent unnecessary battery drain
- **Proper Resource Cleanup**: Ensures all resources are released when service stops

### Version 1.4.0

#### Added - Automatic Report Export
- **Daily Report Export**: Automatic export every 24 hours using WorkManager
- **Multi-Format Reports**: Exports in JSON, CSV, and text summary formats
- **ZIP Compression**: All reports compressed into single ZIP file
- **Downloads Folder Integration**: Saves directly to Downloads folder using MediaStore API
- **Export Notifications**: Shows progress, success, and failure notifications
- **Background Processing**: Uses WorkManager for efficient battery usage
- **No Manual Intervention**: Runs automatically in the background
- **Graceful Error Handling**: Handles missing data and failures appropriately

#### Technical Improvements
- **WorkManager Integration**: Periodic background task scheduling (24-hour interval)
- **MediaStore API**: Android 10+ compatible storage access
- **JSON/CSV Export**: Multiple format support for data portability
- **File Compression**: ZIP file creation for efficient storage
- **Notification System**: Progress and status notifications for user feedback
- **Error Recovery**: Retry logic for transient failures
- **Battery Optimization**: Deferred background work execution
- **Scoped Storage**: API 33+ compliant storage access without special permissions

### Version 1.3.0

#### Added - Detection Tracking & Reporting
- **Detection Counter**: Real-time counter on main screen showing total shoulder surfer detections
- **Database Storage**: Room database integration for persistent detection event storage
- **Report Screen**: Comprehensive reporting with tabbed interface:
  - Weekly reports view
  - Monthly reports view
  - Yearly reports view
- **Statistics Dashboard**: Shows total detections, average faces, max faces per detection
- **Detection History**: Chronological list of all detection events with timestamps
- **Severity Indicators**: Visual indicators (⚠️ for 2 faces, 🚨 for 3+ faces)
- **Data Management**: Clear all data functionality with confirmation dialog
- **Beautiful UI**: Cards, tabs, and modern Material Design 3 components

#### Technical Improvements
- **Room Database**: Full database layer with DAO, Repository pattern
- **SSOT Pattern**: Single source of truth for detection data
- **Flow Integration**: Reactive data streams with Kotlin Flow
- **Coroutines**: Async database operations don't block UI
- **Repository Pattern**: Clean architecture with data layer separation
- **Error Handling**: Graceful handling of database operations
- **Null Safety**: Proper null checking throughout data layer

### Version 1.2.0

#### Added - UI/UX Design
- **Custom App Icon**: Professional security-themed icon with eye and camera frame elements
- **Gradient Background**: Beautiful deep blue gradient background for modern look
- **Pattern Overlay**: Subtle repeating pattern adds texture and depth
- **Color-Coded UI**: 
  - Green for start/active states
  - Red for stop/alert states
  - Blue for information/actions
- **Enhanced Contrast**: White text on dark blue background for excellent readability
- **Consistent Theming**: All buttons and text follow the new color scheme

#### Improved
- **Visual Hierarchy**: Better contrast and color coding improves user understanding
- **Brand Identity**: Custom icon and consistent color scheme create professional appearance
- **Accessibility**: High contrast ratios ensure readability for all users
- **User Experience**: Beautiful gradients and patterns make the app more engaging

### Version 1.2.0 (Previous)

#### Added - UI/UX Design
- **Custom App Icon**: Professional security-themed icon with eye and camera frame elements
- **Gradient Background**: Beautiful deep blue gradient background for modern look
- **Pattern Overlay**: Subtle repeating pattern adds texture and depth
- **Color-Coded UI**: 
  - Green for start/active states
  - Red for stop/alert states
  - Blue for information/actions
- **Enhanced Contrast**: White text on dark blue background for excellent readability
- **Consistent Theming**: All buttons and text follow the new color scheme

#### Improved
- **Visual Hierarchy**: Better contrast and color coding improves user understanding
- **Brand Identity**: Custom icon and consistent color scheme create professional appearance
- **Accessibility**: High contrast ratios ensure readability for all users
- **User Experience**: Beautiful gradients and patterns make the app more engaging

### Version 1.1.0

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

## Battery Optimizations

The app has been optimized for minimal battery consumption while maintaining detection accuracy:

### Camera Processing Optimizations
1. **Reduced Resolution**: ImageAnalysis uses 640x480 resolution instead of full camera resolution (~75% reduction in processing)
2. **Frame Skipping**: Processes every 3rd frame (~10fps effective vs 30fps), reducing CPU usage by ~66%
3. **Optimized Image Format**: Uses YUV_420_888 format optimized for ML Kit processing
4. **Fast Performance Mode**: ML Kit face detection configured for speed over accuracy (sufficient for security monitoring)
5. **Backpressure Strategy**: KEEP_ONLY_LATEST strategy prevents frame backlog and memory buildup

### Background Processing Optimizations
1. **WorkManager Constraints**: Report export only runs when battery is not low
2. **Efficient Scheduling**: Daily reports run during optimal conditions to minimize battery impact
3. **Proper Resource Cleanup**: Camera executor and coroutines properly shut down when service stops

### Expected Battery Impact
- **Active Monitoring**: Approximately 5-10% battery drain per hour (varies by device)
- **Idle State**: Minimal battery usage when no detections occurring
- **Background Reports**: Negligible impact - runs once daily with battery-aware constraints

**Note**: Continuous camera monitoring inherently requires battery power. These optimizations minimize the impact while maintaining security functionality.

## Known Limitations

1. **Front Camera Only**: Currently only monitors front camera
2. **Battery Usage**: Continuous camera usage impacts battery life (optimized but cannot be eliminated)
3. **False Positives**: May detect faces in photos/posters on screen
4. **Lighting Dependency**: Detection accuracy varies with lighting conditions
5. **Android Version**: Requires Android 13+ (API 33)
6. **Database Growth**: Database size grows with detections (cleared manually by user)
7. **Export Schedule**: WorkManager minimum interval is 15 minutes, but daily (24 hours) is recommended
8. **Storage Space**: Exported ZIP files accumulate in Downloads folder (manual cleanup recommended)

## Future Enhancements

- [ ] Configurable detection threshold
- [x] Detection history/statistics (Completed v1.3.0)
- [x] Export reports to CSV/PDF (Completed v1.4.0 - CSV, JSON, TXT formats)
- [x] Battery optimizations (Completed v1.5.0 - reduced resolution, frame skipping, optimized processing)
- [ ] Additional power-saving modes (e.g., pause detection when screen off)
- [ ] Customizable alert sounds
- [ ] Whitelist for known faces
- [ ] Settings screen for user preferences
- [ ] Dark/Light theme support
- [ ] Localization support
- [ ] Charts and graphs for visualization
- [ ] Auto-delete old detections (configurable retention period)
- [ ] Detection time patterns analysis (peak hours/days)
- [ ] Configurable export schedule (hourly, daily, weekly options)
- [ ] Cloud backup integration (Google Drive, Dropbox)
- [ ] Email report sending option

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

### Detection Counter Not Updating

1. Ensure detection service is running
2. Check that Room database is properly initialized
3. Restart the app to refresh the database connection
4. Check logcat for database errors

### Reports Not Showing Data

1. Ensure detections have been recorded (counter > 0)
2. Check selected time period (Weekly/Monthly/Yearly)
3. Verify current date/time settings on device
4. Try clearing and re-recording a detection

### Exported Reports Not Found

1. Check Downloads folder on your device
2. Look for files named `PeekDetector_Reports_[timestamp].zip`
3. Ensure app has been running for at least 24 hours (first export)
4. Check notification tray for export status
5. Verify device storage space is available

### Export Failing

1. Check device storage space
2. Verify Downloads folder is accessible
3. Check notification for specific error message
4. Try clearing app cache and restarting app
5. Ensure at least one detection has been recorded

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

## Customizing the Design

### Changing App Colors

You can customize the color scheme by modifying the colors in `MainActivity.kt`:

```kotlin
// Background gradient colors
Color(0xFF0D47A1),  // Start color
Color(0xFF1565C0),  // Center color
Color(0xFF1976D2)   // End color

// Button colors
Color(0xFF4CAF50)   // Green - Start/Active
Color(0xFFFF5252)   // Red - Stop/Alert
Color(0xFF64B5F6)   // Light Blue - Information
```

### Customizing the App Icon

The app icon consists of two drawable files:

1. **ic_launcher_background.xml**: Contains the gradient background
2. **ic_launcher_foreground.xml**: Contains the eye and security frame design

You can modify these vector drawable files to customize the icon appearance. The files are located in:
```
app/src/main/res/drawable/
```

### Changing Background Pattern

The background pattern is defined in `pattern_overlay.xml`. You can:
- Adjust the opacity in `MainActivity.kt` (currently 0.05f)
- Modify the pattern design in the drawable file
- Remove the pattern by commenting out the Image composable

---

**Last Updated**: October 29, 2025  
**Version**: 1.5.0  
**Minimum Android Version**: 13 (API 33)  
**Target Android Version**: 15 (API 36)

## Report Export File Structure

When reports are exported, they are saved as a ZIP file in your Downloads folder with the following structure:

```
PeekDetector_Reports_20251029_143025.zip
├── detection_report_20251029_143025.json    # JSON format (machine-readable)
├── detection_report_20251029_143025.csv     # CSV format (spreadsheet-compatible)
└── detection_summary_20251029_143025.txt    # Human-readable summary
```

### JSON Format
Contains detailed detection data in JSON format for programmatic access:
```json
{
  "exportDate": "20251029_143025",
  "totalDetections": 42,
  "totalFaces": 97,
  "detections": [
    {
      "id": 1,
      "faceCount": 2,
      "timestamp": 1698589200000,
      "dateTime": "2025-10-29 14:30:00"
    }
  ]
}
```

### CSV Format
Compatible with Excel, Google Sheets, and other spreadsheet applications:
```csv
ID,Face Count,Timestamp (ms),Date Time
1,2,1698589200000,"2025-10-29 14:30:00"
2,3,1698589800000,"2025-10-29 14:40:00"
```

### Summary Text
Human-readable summary with statistics and recent detections:
```
PeekDetector - Detection Report Summary
==================================================

Export Date: 2025-10-29 14:30:25
Report Period: 2025-10-01 08:15:00 to 2025-10-29 14:30:00

Statistics:
--------------------------------------------------
Total Detections: 42
Total Faces Detected: 97
Average Faces per Detection: 2.31
Maximum Faces in Single Detection: 4
Minimum Faces in Single Detection: 2

Detection Severity Breakdown:
--------------------------------------------------
⚠️  Low Severity (2 faces): 30 detections
🚨 High Severity (3+ faces): 12 detections

Recent Detections (Last 10):
--------------------------------------------------
ID 42: 2 faces at 2025-10-29 14:30:00
...
```

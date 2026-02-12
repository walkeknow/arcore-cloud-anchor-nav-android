# AR Cloud Anchor Navigation

An Android augmented reality application that enables users to create, store, and resolve persistent cloud anchors with visual navigation paths between anchor points.

![AR Navigation Demo](https://img.shields.io/badge/ARCore-Enabled-green) ![Firebase](https://img.shields.io/badge/Firebase-Integrated-orange) ![Android](https://img.shields.io/badge/Platform-Android-brightgreen)

## Overview

This application leverages Google's ARCore Cloud Anchors API and Firebase to create a persistent AR navigation system. Users can place virtual anchors in real-world locations, store them in the cloud, and later resolve them from different devices. The app features an advanced line rendering system that draws translucent purple navigation paths with animated white arrowheads between anchor points.

## Features

### 1. **Cloud Anchor Management**

- **Host Anchors**: Create and host new cloud anchors by tapping on detected planes in AR
- **Persistent Storage**: All anchors are stored in Firebase Realtime Database with unique IDs
- **Cloud Synchronization**: Anchors are synced across devices through Google Cloud Anchor API
- **Privacy Controls**: Privacy notice dialog for user consent on data collection

### 2. **Anchor Resolution**

- **Multi-Anchor Selection**: Select multiple anchors from a dropdown list to resolve simultaneously
- **Cross-Device Resolution**: Resolve anchors created on different devices
- **Real-time Tracking**: Continuous tracking of resolved anchor positions in 3D space
- **Visual Anchor Models**: 3D anchor models rendered at resolved positions

### 3. **AR Navigation Lines**

- **Path Visualization**: Draw thick, translucent purple lines between selected anchor points
- **Animated Arrowheads**: Recurring white chevron arrows indicating direction of travel
- **3D Line Rendering**: Lines rendered as volumetric quads with proper depth handling
- **Customizable Appearance**:
  - Line width: 0.15 meters (configurable)
  - Line color: Translucent purple (RGB: 0.55, 0.25, 0.75, Alpha: 0.60)
  - Arrow color: White with high opacity (Alpha: 0.95)
  - Arrow spacing: 0.4 meters apart

### 4. **Feature Map Quality Indicator**

- **Real-time Quality Assessment**: Visual bar showing the quality of the environment for anchor hosting
- **Hosting Guidance**: Helps users understand when the environment is suitable for creating stable anchors
- **Color-coded Feedback**: Green, yellow, and red indicators for feature map quality

### 5. **User Interface**

- **Main Lobby**:
  - Host new anchors button
  - Resolve existing anchors button
  - Privacy notice and settings
- **Cloud Anchor Activity**:
  - AR camera view with plane detection
  - Tap-to-place anchor functionality
  - Feature map quality indicator
  - Save anchor dialog with custom naming
- **Resolve Anchors Lobby**:
  - List of all available anchors
  - Multi-select checkboxes for anchor selection
  - "Resolve" button to view selected anchors in AR
  - "Draw Lines Between Anchors" button for navigation visualization

### 6. **Advanced Rendering**

- **Background Renderer**: Renders camera feed with optional depth visualization
- **Plane Renderer**: Visualizes detected horizontal and vertical planes
- **Point Cloud Renderer**: Shows feature points detected by ARCore
- **Object Renderer**: Renders 3D models at anchor positions
- **Line Renderer**: Custom shader-based line rendering with:
  - Thick volumetric lines (rendered as textured quads)
  - Translucent appearance with alpha blending
  - Directional arrow decorations
  - Proper depth testing and Z-fighting prevention

### 7. **Camera and Tracking**

- **6DOF Tracking**: Full six degrees of freedom tracking
- **Plane Detection**: Automatic detection of horizontal and vertical surfaces
- **Camera Permission Handling**: Graceful permission request and handling
- **Display Rotation Support**: Handles device rotation correctly
- **Full-screen Immersive Mode**: Distraction-free AR experience

### 8. **Firebase Integration**

- **Realtime Database**: Stores anchor IDs and metadata
- **Cloud Functions Ready**: Extensible for server-side logic
- **Authentication**: Google Services integration for user management
- **GPS Coordinates**: Captures and stores location data for map integration
- **Data Structure**:
  ```
  anchors/
    └── {anchorId}/
        ├── cloudAnchorId: string
        ├── name: string (optional)
        ├── timestamp: long
        ├── latitude: number (optional)
        └── longitude: number (optional)
  ```

### 9. **GPS Location Capture & Wayfinder Integration** 🆕

- **Automatic GPS Capture**: Records device location when hosting anchors
- **WGS84 Coordinates**: Standard lat/lng format compatible with mapping systems
- **Wayfinder Export**: Export anchor coordinates to GeoJSON or Wayfinder POI format
- **Map Integration**: Combine precise AR anchors with indoor/outdoor navigation
- **Hybrid Positioning**: Cloud Anchors for cm-level AR + GPS for map integration
- See [GPS_INTEGRATION.md](GPS_INTEGRATION.md) for detailed integration guide

## Technical Stack

- **ARCore**: Google's augmented reality SDK
- **OpenGL ES 2.0**: Low-level graphics rendering
- **Firebase Realtime Database**: Cloud data storage
- **Google Cloud Anchor API**: Persistent cloud anchor hosting and resolution
- **Google Play Services Location**: GPS capture for map integration
- **Java**: Primary development language
- **Android SDK**: Minimum SDK 24 (Android 7.0)

## Requirements

- Android device with ARCore support
- Android 7.0 (API Level 24) or higher
- Active internet connection for cloud anchor operations
- Camera permissions (required)
- Location permissions (recommended for GPS capture and map integration)

## Getting Started

### Prerequisites

1. **Android Studio**: Latest version recommended
2. **ARCore supported device**: Check [ARCore supported devices](https://developers.google.com/ar/devices)
3. **Firebase Project**: Set up at [Firebase Console](https://console.firebase.google.com)
4. **Google Cloud Project**: Enable Cloud Anchor API

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/walkeknow/google-ar-cloud-anchor-navigation.git
   cd google-ar-cloud-anchor-navigation
   ```

2. Add your `google-services.json`:
   - Download from Firebase Console
   - Place in `app/` directory

3. Configure API Keys:
   - Copy `local.properties.example` to `local.properties`
   - Add your Cloud Anchor API key to `local.properties`:
     ```properties
     CLOUD_ANCHOR_API_KEY=your_api_key_here
     ```
   - Note: `local.properties` is gitignored and won't be committed

4. Build and run:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Usage

### Hosting a Cloud Anchor

1. Launch the app and tap "Host Cloud Anchor"
2. Point your device at a flat surface
3. Wait for plane detection (blue grid appears)
4. Tap on the detected plane to place an anchor
5. Move your device around to improve feature map quality
6. When quality is sufficient, tap "Save Anchor"
7. Enter a name for your anchor (optional)
8. The anchor will be uploaded to the cloud

### Resolving and Navigating

1. From main lobby, tap "Resolve Anchors"
2. Select multiple anchors from the list using checkboxes
3. Tap "Resolve" to view anchors in AR view
4. OR tap "Draw Lines Between Anchors" for navigation mode
5. In navigation mode:
   - Purple translucent lines connect the anchors
   - White arrow chevrons indicate the path direction
   - Move around to see the 3D navigation path

## Customization

### Modifying Line Appearance

Edit `LineRenderer.java`:

```java
// Line color (RGBA)
private float[] lineColor = {0.55f, 0.25f, 0.75f, 0.60f}; // Purple with transparency

// Arrow color
private float[] arrowColor = {1.0f, 1.0f, 1.0f, 0.95f}; // White

// Line width in meters
private float lineWidthMeters = 0.15f;

// Distance between arrows
private float arrowSpacing = 0.4f;
```

### Custom Shaders

Shader files located in `app/src/main/assets/shaders/`:

- `line.vert`: Vertex shader for line rendering
- `line.frag`: Fragment shader for line rendering
- Modify for custom visual effects

## Architecture

```
app/
├── src/main/java/com/google/ar/core/examples/java/
│   ├── common/
│   │   ├── helpers/          # Permission, display, tracking helpers
│   │   └── rendering/        # OpenGL rendering classes
│   │       ├── BackgroundRenderer.java
│   │       ├── LineRenderer.java      # Navigation line rendering
│   │       ├── ObjectRenderer.java
│   │       ├── PlaneRenderer.java
│   │       └── PointCloudRenderer.java
│   └── persistentcloudanchor/
│       ├── CloudAnchorActivity.java        # Main AR activity
│       ├── CloudAnchorManager.java         # Anchor lifecycle management
│       ├── MainLobbyActivity.java          # Entry point
│       └── ResolveAnchorsLobbyActivity.java # Anchor selection
├── assets/
│   ├── models/              # 3D models and textures
│   └── shaders/             # GLSL shader programs
└── res/
    ├── layout/              # UI layouts
    ├── values/              # Strings, colors, API keys
    └── drawable/            # Icons and graphics
```

## Security & Privacy

- Users must accept privacy notice before using cloud anchors
- Anchor data is stored securely in Firebase with proper access rules
- Camera data is processed locally and not transmitted
- Cloud anchors contain only spatial data, no personal information

## Known Issues

- Line rendering may have minor z-fighting on some devices (mitigated with depth mask handling)
- Arrow density may need adjustment based on real-world scale
- Very long distances between anchors may require performance optimization

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License

Copyright 2024 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Acknowledgments

- Built on Google ARCore SDK
- Uses Firebase for cloud data storage

## Support

For issues and questions:

- GitHub Issues: [Report a bug](https://github.com/walkeknow/google-ar-cloud-anchor-navigation/issues)
- ARCore Documentation: [developers.google.com/ar](https://developers.google.com/ar)
- Firebase Documentation: [firebase.google.com/docs](https://firebase.google.com/docs)

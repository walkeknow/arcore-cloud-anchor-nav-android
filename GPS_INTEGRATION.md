# GPS Integration & Wayfinder Export Guide

## Overview

Your Cloud Anchors app now captures GPS coordinates (latitude/longitude) when hosting anchors. This enables seamless integration with Wayfinder WebEngine and other mapping systems.

## What Was Added

### 1. **GPS Capture on Anchor Creation**
- Location permissions (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
- Automatic GPS capture when hosting cloud anchors
- Fallback to save without GPS if location unavailable

### 2. **Enhanced Data Model**
- `AnchorItem.java`: Added `latitude` and `longitude` fields
- `FirebaseManager.java`: Updated to store/fetch GPS coordinates
- Backwards compatible with existing anchors (nullable GPS fields)

### 3. **Wayfinder Export Utility**
- `WayfinderExporter.java`: Export anchors to GeoJSON or Wayfinder POI format
- Ready for integration with Wayfinder WebEngine

## Firebase Data Structure

Anchors are now stored with GPS coordinates:

```json
{
  "cloudAnchors": {
    "ua-abc123...": {
      "anchorId": "ua-abc123...",
      "name": "Gate 24",
      "timestamp": 1707700000000,
      "latitude": 36.081058,
      "longitude": -115.136111
    }
  }
}
```

## Coordinate System

All coordinates use **WGS84** standard:
- ✅ Same as Google Maps
- ✅ Same as Wayfinder WebEngine  
- ✅ Same as GPS receivers
- ✅ Direct compatibility - no conversion needed!

## Exporting to Wayfinder

### Option 1: GeoJSON Format

```java
// Fetch anchors from Firebase
firebaseManager.fetchAnchorsFromFirebase(new FirebaseManager.AnchorFetchListener() {
  @Override
  public void onAnchorsRetrieved(List<AnchorItem> anchors) {
    String geoJson = WayfinderExporter.exportToGeoJSON(anchors);
    Log.d("Export", geoJson);
    // Save to file or send to server
  }
  
  @Override
  public void onError(String errorMessage) {
    Log.e("Export", "Error: " + errorMessage);
  }
});
```

**Output:**
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [-115.136111, 36.081058]
      },
      "properties": {
        "anchorId": "ua-abc123...",
        "name": "Gate 24",
        "minutesSinceCreation": "45m ago"
      }
    }
  ]
}
```

### Option 2: Wayfinder POI Format

```java
String wayfinderPOIs = WayfinderExporter.exportToWayfinderFormat(anchors);
```

**Output:**
```json
[
  {
    "lat": 36.081058,
    "lng": -115.136111,
    "name": "Gate 24",
    "poiId": "ua-abc123...",
    "category": "ar-anchor",
    "isNavigable": true
  }
]
```

### Option 3: Log Coordinates

```java
WayfinderExporter.logAnchorCoordinates(anchors);
```

**Logcat Output:**
```
D/WayfinderExporter: === Cloud Anchor GPS Coordinates ===
D/WayfinderExporter: ua-abc123...: [36.081058, -115.136111] - Gate 24
D/WayfinderExporter: ua-def456...: [36.081200, -115.136300] - Security Checkpoint
D/WayfinderExporter: ===================================
```

## Integration with Wayfinder WebEngine

### Step 1: Export Anchor Coordinates

Add export functionality to `ResolveAnchorsLobbyActivity.java`:

```java
Button exportButton = findViewById(R.id.export_button);
exportButton.setOnClickListener(v -> {
  firebaseManager.fetchAnchorsFromFirebase(new FirebaseManager.AnchorFetchListener() {
    @Override
    public void onAnchorsRetrieved(List<AnchorItem> anchors) {
      String wayfinderData = WayfinderExporter.exportToWayfinderFormat(anchors);
      // Share via clipboard, file, or API call to server
      shareExportedData(wayfinderData);
    }
    
    @Override
    public void onError(String errorMessage) {
      Toast.makeText(ResolveAnchorsLobbyActivity.this, 
          "Export failed: " + errorMessage, Toast.LENGTH_SHORT).show();
    }
  });
});
```

### Step 2: Import to Wayfinder

In your Wayfinder WebEngine configuration, add AR anchors as POIs:

```javascript
// wayfinder-webengine/src/configs/your-venue.json
{
  "plugins": {
    "poiDataManager": {
      "customPOIs": [
        {
          "lat": 36.081058,
          "lng": -115.136111,
          "name": "AR Anchor: Gate 24",
          "poiId": "ar-ua-abc123",
          "category": "ar-anchor",
          "isNavigable": true,
          "floorId": "terminal-2-level-3"
        }
      ]
    }
  }
}
```

### Step 3: Navigate from Wayfinder to AR

Users can:
1. Select AR anchor POI on wayfinder map
2. Navigate to the location
3. Launch AR view when nearby
4. See AR content anchored at that exact location

## Location Permission Flow

1. App requests location permissions on first launch
2. User grants/denies permissions
3. If granted: GPS captured automatically when hosting anchor
4. If denied: Anchor saved without GPS (still works for AR, just no map integration)

## Best Practices

### Indoor Accuracy
- GPS accuracy indoors: ±5-50 meters
- Better near windows/atriums
- Best for general area marking, not precise indoor positioning
- Consider adding manual floor/area tags for better indoor context

### Outdoor Accuracy
- GPS accuracy outdoors: ±3-10 meters
- Excellent for outdoor AR navigation
- Perfect for campus, parks, outdoor venues

### Hybrid Approach
- **Cloud Anchors**: Precise AR positioning (cm-level accuracy)
- **GPS Coordinates**: General location & map integration
- **Best of both worlds**: Accurate AR + Wayfinder navigation

## Troubleshooting

### No GPS Data Captured
- Check location permissions are granted
- Ensure device has GPS signal (go near window or outside)
- Check logcat for permission/location errors

### GPS Coordinates Inaccurate
- Indoor GPS is inherently less accurate (5-50m)
- Move to area with better sky visibility
- Consider adding manual coordinate adjustment

### Firebase Data Not Updating
- Check Firebase rules allow writes
- Verify internet connection
- Check logcat for Firebase errors

## Example Use Cases

1. **Airport Navigation**: Create AR anchors at gates, link to wayfinder map
2. **Museum Tour**: AR exhibits with wayfinder floor plans
3. **Campus Wayfinding**: AR directional signs + interactive map
4. **Event Spaces**: AR information points + venue map

## Technical Details

- **Coordinate Format**: WGS84 (latitude, longitude in decimal degrees)
- **GPS Provider**: FusedLocationProviderClient (Google Play Services)
- **Permissions**: ACCESS_FINE_LOCATION (required), ACCESS_COARSE_LOCATION (fallback)
- **Storage**: Firebase Realtime Database
- **Export Formats**: GeoJSON, Wayfinder POI JSON

## Next Steps

1. ✅ GPS capture is working
2. ⏭️ Test outdoor anchor creation
3. ⏭️ Export coordinates to wayfinder
4. ⏭️ Add manual floor/building assignment
5. ⏭️ Implement bi-directional navigation (Map ↔ AR)

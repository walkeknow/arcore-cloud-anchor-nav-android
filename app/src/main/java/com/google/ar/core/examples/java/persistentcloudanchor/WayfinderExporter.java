/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.persistentcloudanchor;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

/**
 * Utility class for exporting cloud anchor data to formats compatible with Wayfinder WebEngine.
 * Converts stored anchors with GPS coordinates to GeoJSON or JSON formats that can be used
 * for integration with mapping systems like Wayfinder.
 */
public class WayfinderExporter {
  private static final String TAG = WayfinderExporter.class.getSimpleName();
  
  /**
   * Exports anchors to GeoJSON format for use in mapping applications.
   * Compatible with Wayfinder WebEngine coordinate system (WGS84).
   * 
   * @param anchors List of AnchorItem objects with GPS coordinates
   * @return GeoJSON string representation
   */
  public static String exportToGeoJSON(List<AnchorItem> anchors) {
    try {
      JSONObject geoJson = new JSONObject();
      geoJson.put("type", "FeatureCollection");
      
      JSONArray features = new JSONArray();
      
      for (AnchorItem anchor : anchors) {
        if (anchor.getLatitude() != null && anchor.getLongitude() != null) {
          JSONObject feature = new JSONObject();
          feature.put("type", "Feature");
          
          // Geometry (Point with WGS84 coordinates)
          JSONObject geometry = new JSONObject();
          geometry.put("type", "Point");
          JSONArray coordinates = new JSONArray();
          coordinates.put(anchor.getLongitude()); // [longitude, latitude] format
          coordinates.put(anchor.getLatitude());
          geometry.put("coordinates", coordinates);
          feature.put("geometry", geometry);
          
          // Properties
          JSONObject properties = new JSONObject();
          properties.put("anchorId", anchor.getAnchorId());
          properties.put("name", anchor.getAnchorName());
          properties.put("minutesSinceCreation", anchor.getMinutesSinceCreation());
          feature.put("properties", properties);
          
          features.put(feature);
        }
      }
      
      geoJson.put("features", features);
      return geoJson.toString(2); // Pretty print with indent
      
    } catch (JSONException e) {
      Log.e(TAG, "Error creating GeoJSON: " + e.getMessage());
      return null;
    }
  }
  
  /**
   * Exports anchors to simple JSON array format for Wayfinder integration.
   * Format matches Wayfinder POI structure with {lat, lng, name, poiId}.
   * 
   * @param anchors List of AnchorItem objects with GPS coordinates
   * @return JSON array string
   */
  public static String exportToWayfinderFormat(List<AnchorItem> anchors) {
    try {
      JSONArray wayfinderPOIs = new JSONArray();
      
      for (AnchorItem anchor : anchors) {
        if (anchor.getLatitude() != null && anchor.getLongitude() != null) {
          JSONObject poi = new JSONObject();
          poi.put("lat", anchor.getLatitude());
          poi.put("lng", anchor.getLongitude());
          poi.put("name", anchor.getAnchorName());
          poi.put("poiId", anchor.getAnchorId());
          poi.put("category", "ar-anchor");
          poi.put("isNavigable", true);
          
          wayfinderPOIs.put(poi);
        }
      }
      
      return wayfinderPOIs.toString(2);
      
    } catch (JSONException e) {
      Log.e(TAG, "Error creating Wayfinder format: " + e.getMessage());
      return null;
    }
  }
  
  /**
   * Prints anchor GPS coordinates to logcat for debugging and manual export.
   * 
   * @param anchors List of AnchorItem objects
   */
  public static void logAnchorCoordinates(List<AnchorItem> anchors) {
    Log.d(TAG, "=== Cloud Anchor GPS Coordinates ===");
    for (AnchorItem anchor : anchors) {
      if (anchor.getLatitude() != null && anchor.getLongitude() != null) {
        Log.d(TAG, String.format("%s: [%f, %f] - %s",
            anchor.getAnchorId(),
            anchor.getLatitude(),
            anchor.getLongitude(),
            anchor.getAnchorName()));
      } else {
        Log.d(TAG, String.format("%s: NO GPS DATA - %s",
            anchor.getAnchorId(),
            anchor.getAnchorName()));
      }
    }
    Log.d(TAG, "===================================");
  }
}

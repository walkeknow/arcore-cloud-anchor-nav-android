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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to manage Firebase Realtime Database operations for cloud anchors.
 */
public class FirebaseManager {
  private static final String TAG = FirebaseManager.class.getSimpleName();
  private static final String CLOUD_ANCHORS_PATH = "cloudAnchors";
  private final DatabaseReference databaseRef;

  public interface AnchorFetchListener {
    void onAnchorsRetrieved(List<AnchorItem> anchors);
    void onError(String errorMessage);
  }

  public FirebaseManager() {
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    databaseRef = database.getReference();
  }

  /**
   * Saves an anchor to Firebase Realtime Database.
   *
   * @param anchorId The cloud anchor ID
   * @param anchorName The name/nickname of the anchor
   */
  public void saveAnchorToFirebase(String anchorId, String anchorName) {
    saveAnchorToFirebase(anchorId, anchorName, null, null);
  }
  
  /**
   * Saves an anchor with GPS coordinates to Firebase Realtime Database.
   *
   * @param anchorId The cloud anchor ID
   * @param anchorName The name/nickname of the anchor
   * @param latitude GPS latitude (nullable)
   * @param longitude GPS longitude (nullable)
   */
  public void saveAnchorToFirebase(String anchorId, String anchorName, Double latitude, Double longitude) {
    Map<String, Object> anchorData = new HashMap<>();
    anchorData.put("anchorId", anchorId);
    anchorData.put("name", anchorName);
    anchorData.put("timestamp", ServerValue.TIMESTAMP);
    
    if (latitude != null && longitude != null) {
      anchorData.put("latitude", latitude);
      anchorData.put("longitude", longitude);
      Log.d(TAG, "Saving anchor with GPS: " + latitude + ", " + longitude);
    }

    databaseRef.child(CLOUD_ANCHORS_PATH).child(anchorId).setValue(anchorData)
        .addOnSuccessListener(aVoid -> 
            Log.d(TAG, "Successfully saved anchor " + anchorId + " to Firebase"))
        .addOnFailureListener(e -> 
            Log.e(TAG, "Failed to save anchor to Firebase: " + e.getMessage()));
  }

  /**
   * Fetches all cloud anchors from Firebase Realtime Database.
   *
   * @param listener Callback listener for receiving the fetched anchors
   */
  public void fetchAnchorsFromFirebase(AnchorFetchListener listener) {
    databaseRef.child(CLOUD_ANCHORS_PATH).addListenerForSingleValueEvent(
        new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            List<AnchorItem> anchors = new ArrayList<>();
            long currentTimeMillis = System.currentTimeMillis();

            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
              try {
                String anchorId = snapshot.child("anchorId").getValue(String.class);
                String name = snapshot.child("name").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                Double latitude = snapshot.child("latitude").getValue(Double.class);
                Double longitude = snapshot.child("longitude").getValue(Double.class);

                if (anchorId != null && name != null && timestamp != null) {
                  // Calculate time since creation in minutes
                  long timeSinceCreation = 
                      TimeUnit.MILLISECONDS.toMinutes(currentTimeMillis - timestamp);

                  // Only include anchors less than 24 hours old
                  if (timeSinceCreation < 24 * 60) {
                    anchors.add(new AnchorItem(anchorId, name, timeSinceCreation, latitude, longitude));
                    if (latitude != null && longitude != null) {
                      Log.d(TAG, "Loaded anchor with GPS: " + latitude + ", " + longitude);
                    }
                  } else {
                    // Optionally delete old anchors
                    deleteAnchorFromFirebase(anchorId);
                  }
                }
              } catch (Exception e) {
                Log.e(TAG, "Error parsing anchor data: " + e.getMessage());
              }
            }

            listener.onAnchorsRetrieved(anchors);
          }

          @Override
          public void onCancelled(DatabaseError databaseError) {
            Log.e(TAG, "Failed to fetch anchors from Firebase: " + databaseError.getMessage());
            listener.onError(databaseError.getMessage());
          }
        });
  }

  /**
   * Deletes an anchor from Firebase Realtime Database.
   *
   * @param anchorId The cloud anchor ID to delete
   */
  public void deleteAnchorFromFirebase(String anchorId) {
    databaseRef.child(CLOUD_ANCHORS_PATH).child(anchorId).removeValue()
        .addOnSuccessListener(aVoid -> 
            Log.d(TAG, "Successfully deleted anchor " + anchorId + " from Firebase"))
        .addOnFailureListener(e -> 
            Log.e(TAG, "Failed to delete anchor from Firebase: " + e.getMessage()));
  }
}

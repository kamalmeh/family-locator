package com.smiansh.familylocator;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LocationUpdates extends BroadcastReceiver {

    public static final String PROCESS_LOCATION_UPDATES =
            "com.smiansh.familylocator.action.PROCESS_UPDATES";
    private static final String TAG = "LocationUpdates";
//    private Context Ctx;

    @Override
    public void onReceive(Context context, Intent intent) {
//        Ctx = context;
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                if (PROCESS_LOCATION_UPDATES.equals(action)) {
                    LocationResult result = LocationResult.extractResult(intent);
                    if (result != null) {
                        Location location = result.getLastLocation();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
//                            Log.i(TAG,"current user: " + userId);
//                            Toast.makeText(Ctx, userId + "\n" + location.getLatitude() + "," + location.getLongitude(), Toast.LENGTH_SHORT).show();
                            UserNote.sendNotification(context, "Location Details Updated: "
                                    + location.getLatitude() + "," + location.getLongitude());
                            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            DocumentReference documentReference;
                            documentReference = db.collection("users").document(userId);
                            Map<String, Object> data = new HashMap<>();
                            data.put("location", geoPoint);
                            documentReference.set(data, SetOptions.merge());
                        }
                    } else {
                        Log.i(TAG, "Location result is null");
                    }
                } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                    UserNote.sendNotification(context, "BOOT_COMPLETED");
                    Intent locationIntent = new Intent(context, LocationUpdates.class);
                    locationIntent.setAction(LocationUpdates.PROCESS_LOCATION_UPDATES);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    LocationRequest locationRequest = LocationRequest.create();
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    locationRequest.setInterval(1000);             //1 Seconds
                    locationRequest.setFastestInterval(1000);       //1 Second
                    locationRequest.setMaxWaitTime(300 * 1000);       //300 Seconds = 5 Minutes
                    FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "Permissions are not granted");
                    }
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, pendingIntent);
                } else {
                    Log.i(TAG, "Action Issue:" + action);
                }
            }
        }
    }
}

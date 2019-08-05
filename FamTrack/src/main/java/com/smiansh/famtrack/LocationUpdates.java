package com.smiansh.famtrack;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationUpdates extends BroadcastReceiver {

    public static final String PROCESS_LOCATION_UPDATES =
            "com.smiansh.famtrack.action.PROCESS_UPDATES";
    private static final String TAG = "LocationUpdates";
    private String userId = null;
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            updateToDatabase(locationResult);
        }
    };
    private SharedPreferences sp;
    private Double lastLocationLat = 0.0;
    private Double lastLocationLong = 0.0;
    private SimpleDateFormat sd = new SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault());

    private void updateToDatabase(LocationResult locationResult) {
        Location location = locationResult.getLastLocation();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user.getUid();
        if (user != null) {
            String userId = user.getUid();
//                            Log.i(TAG,"current user: " + userId);
//                            Toast.makeText(Ctx, userId + "\n" + location.getLatitude() + "," + location.getLongitude(), Toast.LENGTH_SHORT).show();
//                            UserNote.sendNotification(context, "Location Details Updated: ");
//                                    + location.getLatitude() + "," + location.getLongitude());
            final GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference documentReference;
            documentReference = db.collection("users").document(userId);
            SimpleDateFormat format = new SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault());
            Map<String, Object> data = new HashMap<>();
            data.put("location", geoPoint);
            data.put("location_timestamp", format.format(new Date()));
            documentReference.set(data, SetOptions.merge());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            lastLocationLat = Double.parseDouble(sp.getString("Latitude", null));
            lastLocationLong = Double.parseDouble(sp.getString("Longitude", null));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                if (PROCESS_LOCATION_UPDATES.equals(action)) {
                    LocationResult result = LocationResult.extractResult(intent);
                    if (result != null) {
                        Location location = result.getLastLocation();
                        if ((location.getLatitude() != lastLocationLat) || (location.getLongitude() != lastLocationLong)) {
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("lastLocationTime", sd.format(location.getTime()));
                            editor.putString("Latitude", String.valueOf(location.getLatitude()));
                            editor.putString("Longitude", String.valueOf(location.getLongitude()));
                            editor.apply();
                            updateToDatabase(result);
                        }
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            Intent mapActivityIntent = new Intent(context, MapsActivity.class);
//                            mapActivityIntent.putExtra("userId", userId);
//                            context.startForegroundService(intent);
//                        }
                    }
//                    startLocationReporting(context);
                } else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                    startLocationReporting(context);
                } else {
                    Log.i(TAG, "Action Issue:" + action);
                }
            }
        }
    }

    private void startLocationReporting(Context context) {
        Intent locationIntent = new Intent(context, LocationUpdates.class);
        locationIntent.setAction(LocationUpdates.PROCESS_LOCATION_UPDATES);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);             //10 Seconds
        locationRequest.setFastestInterval(5000);       //5 Second
        locationRequest.setSmallestDisplacement(10);    //10 Meters
        locationRequest.setMaxWaitTime(30000);           //1 Second
        locationRequest.setNumUpdates(120);             //120 updates in a minute
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permissions are not granted");
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, pendingIntent);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
}

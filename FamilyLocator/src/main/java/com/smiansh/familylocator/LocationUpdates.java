package com.smiansh.familylocator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;
import com.google.firebase.auth.FirebaseAuth;
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
    private Context Ctx;

    @Override
    public void onReceive(Context context, Intent intent) {
        Ctx = context;
        if (intent != null) {
            final String action = intent.getAction();
            if (PROCESS_LOCATION_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    Location location = result.getLastLocation();
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                    Toast.makeText(Ctx, userId + "\n"+location.getLatitude()+","+location.getLongitude(), Toast.LENGTH_SHORT).show();
//                    UserNote.sendNotification(context,"Location Details Updated: "
//                        + location.getLatitude()+","+location.getLongitude());
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference documentReference = db.collection("users").document(userId);
                    Map<String, Object> data = new HashMap<>();
                    data.put("location", geoPoint);
                    documentReference.set(data, SetOptions.merge());
                } else {
                    Toast.makeText(Ctx, "Location Result is null", Toast.LENGTH_SHORT).show();
                }
            } else {
//                Toast.makeText(Ctx, "Action Issue: "+action, Toast.LENGTH_SHORT).show();
            }
        }
    }
}

package com.smiansh.familylocator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationResult;

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
                    //UserNote.setLocationUpdatesResult(context, location);
//                    UserNote.sendNotification(context,"Location Details Updated: "
//                        + location.getLatitude()+","+location.getLongitude());
                }
            }
        }
    }
}

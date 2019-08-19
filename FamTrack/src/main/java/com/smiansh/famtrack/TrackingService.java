package com.smiansh.famtrack;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TrackingService extends Service {
    //    Class Variable Declaration
    private static final String TAG = "Tracking Service";
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient client;
    private String userId;
    private SimpleDateFormat sd;
    private SharedPreferences sp;
    private Helper myHelper;
    private Double lastLocationLat = 0.0;
    private Double lastLocationLong = 0.0;
    private long requestInterval = 60000;
    private String activity = "Stationary";
    private int serviceId = 1;

    public TrackingService() {
        mLocationRequest = createLocationRequest();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if ((location.getLatitude() != lastLocationLat) || (location.getLongitude() != lastLocationLong)) {
                        Log.i(TAG, "Location: " + location.getLatitude() + "," + location.getLongitude());
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("lastLocationTime", sd.format(location.getTime()));
                        editor.putString("Latitude", String.valueOf(location.getLatitude()));
                        editor.putString("Longitude", String.valueOf(location.getLongitude()));
                        editor.apply();
                        updateToDatabase(location);
                    } else {
                        Log.i(TAG, location.getLatitude() + " = " + lastLocationLat + "," + location.getLongitude() + " = " + lastLocationLong);
                    }
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.removeLocationUpdates(locationCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        requestInterval = intent.getLongExtra("requestInterval", 30000);
        activity = intent.getStringExtra("activity");
        mLocationRequest = createLocationRequest();
        client = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return START_STICKY;
        }
        client.requestLocationUpdates(mLocationRequest, locationCallback, null);
//        myHelper.sendNote(getString(R.string.app_name), getString(R.string.notification_message, activity));
//        myHelper.sendNote(getString(R.string.app_name), getString(R.string.notification_message));
        return START_STICKY;
    }

    private void updateToDatabase(Location location) {
        FirebaseUser user = null;
        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (user != null) {
            userId = user.getUid();
        }
        if (user != null) {
            Log.i(TAG, "Location updated for current user: " + userId);
            final GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference documentReference = db.collection("users").document(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("location", geoPoint);
            data.put("location_timestamp", sd.format(new Date()));
            documentReference.update(data);
        }
    }

    private LocationRequest createLocationRequest() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(requestInterval);
        if (requestInterval == 1000)
            request.setFastestInterval(1000);
        else
            request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return request;
    }

//    private Notification getNotification() {
//        String CHANNEL_ID = getString(R.string.app_name);
//        String CHANNEL = getString(R.string.channel);
//        Notification myNotification;
//        NotificationChannel channel;
//        NotificationManager notificationManager = getSystemService(NotificationManager.class);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            channel = new NotificationChannel(CHANNEL_ID, CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
//            channel.setDescription("It's My Circle for my dear family members, relatives and friends");
//            channel.enableLights(true);
//            if (notificationManager != null) {
//                notificationManager.createNotificationChannel(channel);
//            }
//        }
//        Bitmap bmp = myHelper.getBitmap();
//        // Add action button in the notification
//        Intent intent = new Intent(this, DetectActivityBroadcastReceiver.class);
//        intent.setAction(DetectActivityBroadcastReceiver.ACTION_PANIC);
//        intent.putExtra("EXTRA_NOTIFICATION_ID", serviceId);
//        PendingIntent pIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
//
//        builder.setSmallIcon(R.drawable.ic_map_marker_point)
//                .setLargeIcon(bmp)
//                .addAction(R.drawable.ic_paper_plane, "Panic Button", pIntent)
//                .setStyle(new NotificationCompat.InboxStyle()
//                        .setBigContentTitle("Your Current Location")
//                        .setSummaryText(getString(R.string.notification_message))
//                )
//                .setPriority(NotificationManager.IMPORTANCE_MAX)
////                .setAutoCancel(true)
////                .setTimeoutAfter(10000)
//                .setOngoing(true)
//                .setPriority(NotificationCompat.PRIORITY_LOW);
//        myNotification = builder.build();
//
//        return myNotification;
//    }

    @Override
    public void onCreate() {
        super.onCreate();
        myHelper = new Helper(getApplicationContext());
        startForeground(serviceId, myHelper.getNotification());
        sd = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            lastLocationLat = Double.parseDouble(sp.getString("Latitude", null));
            lastLocationLong = Double.parseDouble(sp.getString("Longitude", null));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

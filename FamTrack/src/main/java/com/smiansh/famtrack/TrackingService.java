package com.smiansh.famtrack;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

public class TrackingService extends Service {
    //    Class Variable Declaration
    private static final String TAG = "TRACKING_SERVICE";
    public static final int EMEARGENCY_MESSAGE_ID = 2;
    private static final float GEOFENCE_RADIUS_IN_METERS = 100;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient client;
    private String userId = FirebaseAuth.getInstance().getUid();
    private SimpleDateFormat sd;
    private PrefManager prefManager;
    private SharedPreferences sp;
    private Helper myHelper;
    private Double lastLocationLat = 0.0;
    private Double lastLocationLong = 0.0;
    private static String activity = "Stationary";
    private String lastActivity = "Stationary";
    private long requestInterval = 600000;
    private NotificationManager notificationManager;
    private GeofencingClient geofencingClient;
    private List<Geofence> geofenceList = null;
    private PendingIntent geofencePendingIntent;
    //    private Task<Void> geoFencingTask;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public TrackingService() {
        mLocationRequest = createLocationRequest();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    Location curr_location = locationResult.getLastLocation();
                    Location last_location = new Location("");
                    last_location.setLatitude(lastLocationLat);
                    last_location.setLongitude(lastLocationLong);
//                  check if distance is greate than 10 meters, if yes, update it to database
                    float distance = last_location.distanceTo(curr_location);
                    if (distance > 5) {
                        Log.i(TAG, "Distance Travelled:" + distance);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("lastLocationTime", sd.format(curr_location.getTime()));
                        editor.putString("Latitude", String.valueOf(curr_location.getLatitude()));
                        editor.putString("Longitude", String.valueOf(curr_location.getLongitude()));
                        editor.putString("lastActivity", activity);
                        editor.apply();
                        updateToDatabase(curr_location);
                        lastLocationLat = Double.parseDouble(sp.getString("Latitude", "1"));
                        lastLocationLong = Double.parseDouble(sp.getString("Longitude", "1"));
                        lastActivity = sp.getString("lastActivity", "Stationary");
                    } else {
                        Log.i(TAG, "Distance Travelled:" + distance);
                        lastActivity = sp.getString("lastActivity", "Stationary").trim();
                        if (activity.equals("Stationary") && !activity.equals(lastActivity)) {
                            Log.i(TAG, "Stopping as activity is 'Stationary'");
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("lastActivity", activity);
                            editor.apply();
                            updateToDatabase(curr_location);
                            stopSelf();
                        } else if (activity.equals("Stationary")) {
                            stopSelf();
                        }
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
        try {
            requestInterval = intent.getLongExtra("requestInterval", 600000);
            activity = intent.getStringExtra("activity");
            if (activity != null) {
                activity = activity.trim();
            }
            startForeground(Helper.NOTIFICATION_SERVICE_ID, myHelper.getNotification(activity));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        mLocationRequest = createLocationRequest();
        client = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return START_STICKY;
        }
        client.requestLocationUpdates(mLocationRequest, locationCallback, null);
//        geoFencingTask.addOnSuccessListener()

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
            final FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data != null) {
                                data.put("location", geoPoint);
                                data.put("location_timestamp", sd.format(new Date()));
                                data.put("last_activity", activity);
                                db.collection("users").document(userId).set(data, SetOptions.merge());
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    });
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

    private Notification getNotification(String title, String sender, String message, GeoPoint location, String address) {
        String CHANNEL_ID = "TRACKING_SERVICE";
        String CHANNEL = "EMERGENCY_MESSAGE";
        Notification myNotification;
        NotificationChannel channel;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID, CHANNEL, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("This channel is for Emergency Messages from Family Members");
            channel.enableLights(true);
            channel.setLightColor(R.color.colorPrimary);
            channel.setShowBadge(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        Person person = new Person.Builder()
                .setName(sender)
                .setImportant(true)
                .build();
        Intent markMsgRead = new Intent(this, UpdateEmergencyMessage.class);
        markMsgRead.putExtra("userId", userId);
        markMsgRead.setAction(UpdateEmergencyMessage.ACTION_MARK_READ);
        PendingIntent markReadIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, markMsgRead, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent directionIntent = new Intent(this, UpdateEmergencyMessage.class);
        directionIntent.setAction(UpdateEmergencyMessage.ACTION_GET_DIRECTION);
        directionIntent.putExtra("data", "google.navigation:q=" + location.getLatitude() + "," + location.getLongitude());
        directionIntent.setPackage("com.google.android.apps.maps");
        PendingIntent dIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, directionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_map_marker_point)
                .addAction(new NotificationCompat.Action(R.drawable.ic_map_marker_point, "Mark Read", markReadIntent))
                .addAction(new NotificationCompat.Action(R.drawable.ic_message_black_24dp, "Get Direction", dIntent))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        builder.setSound(notificationSound);
        builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
        String finalMessage = message + "\n" + "My Geo Location: " + location.getLatitude() + "," + location.getLongitude() + "\n"
                + "My Address: " + address;
        new NotificationCompat.MessagingStyle(person)
                .setConversationTitle(title)
                .addMessage(finalMessage, new Date().getTime(), person)
                .setBuilder(builder);
        myNotification = builder.build();
        myNotification.flags |= Notification.FLAG_AUTO_CANCEL;

        return myNotification;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myHelper = new Helper(getApplicationContext());
        startForeground(Helper.NOTIFICATION_SERVICE_ID, myHelper.getNotification(activity));
        sd = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
        prefManager = new PrefManager(getApplicationContext());
        sp = prefManager.getApplicationDefaultSharedPreferences();
        try {
            lastLocationLat = Double.parseDouble(sp.getString("Latitude", "1"));
            lastLocationLong = Double.parseDouble(sp.getString("Longitude", "1"));
            lastActivity = sp.getString("lastActivity", "Stationary");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
//            Temporarily disabled the Geofence feature
//            defineGeofences();
            subscribeToEmergencyMessages();
        }
    }

    private void defineGeofences() {
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceList = new ArrayList<>();
        final Context ctx = getApplicationContext();
        db.document("/users/" + userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        //noinspection unchecked
                        Map<String, String> family = (Map<String, String>) documentSnapshot.get("family");
                        if (family != null) {
                            for (Map.Entry entry : family.entrySet()) {
                                final String uid = (String) entry.getKey();
                                db.collection("/users/" + entry.getKey() + "/places").get()
                                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                            @Override
                                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                List<DocumentSnapshot> places = queryDocumentSnapshots.getDocuments();
                                                for (DocumentSnapshot place : places) {
                                                    GeoPoint geoPointForPlace = place.getGeoPoint("location");
                                                    String name = place.getString("name");
                                                    if (geofenceList != null && geoPointForPlace != null) {
                                                        geofenceList.add(new Geofence.Builder()
                                                                .setRequestId(place.getId())
                                                                .setCircularRegion(
                                                                        geoPointForPlace.getLatitude(),
                                                                        geoPointForPlace.getLongitude(),
                                                                        GEOFENCE_RADIUS_IN_METERS
                                                                )
                                                                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                                                        Geofence.GEOFENCE_TRANSITION_EXIT)
                                                                .setLoiteringDelay(300000)
                                                                .build()
                                                        );
                                                        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent(name))
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        Log.i(TAG, "Geofencing added for : " + uid);
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        e.printStackTrace();
                                                                        Log.i(TAG, e.getMessage() + " ");
                                                                    }
                                                                });
                                                    }
                                                }
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                e.printStackTrace();
                                                Toast.makeText(TrackingService.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(String memberName) {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        intent.putExtra("memberName", memberName);
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private void subscribeToEmergencyMessages() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.document("/emergency/" + userId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null)
                    e.getMessage();
                else {
                    String msg;
                    GeoPoint geoPoint;
                    String address;
                    if (documentSnapshot != null) {
                        msg = documentSnapshot.getString("emergency_message");
                        String sender = documentSnapshot.getString("sender");
                        geoPoint = documentSnapshot.getGeoPoint("location");
                        address = documentSnapshot.getString("address");
                        String read = documentSnapshot.getString("read");
                        if (geoPoint != null) {
                            if (read != null && read.equals("no"))
                                myHelper.sendNotification(EMEARGENCY_MESSAGE_ID, getNotification("Emergency Message", sender, msg, geoPoint
                                        , address));
                        }
                    }
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

package com.smiansh.famtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetectActivityBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_DETECT = "com.smiansh.famtrack.action.DETECT_ACTIVITY";
    public static final String ACTION_PANIC = "com.smiansh.famtrack.action.PANIC_ACTION";
    private static final String TAG = "DETECT_ACTIVITY_SERVICE";
    private Context mContext;
    SmsManager sms = SmsManager.getDefault();

    public void startTrackingService(long requestInterval, String activity) {
            Intent trackingService = new Intent(mContext, TrackingService.class);
        mContext.stopService(trackingService);
            trackingService.putExtra("requestInterval", requestInterval);
            trackingService.putExtra("activity", activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(trackingService);
        } else {
            mContext.startService(trackingService);
        }
    }

    public void sendSMS(final String message) {
        String userId = FirebaseAuth.getInstance().getUid();
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.document("users/" + userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        final String firstName = documentSnapshot.getString("firstName");
                        final GeoPoint location = documentSnapshot.getGeoPoint("location");
                        if (location != null) {
                            String address = "Address";
                            List<Address> addressList;
                            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
                            try {
                                addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                if (addressList.size() > 0) {
                                    address = addressList.get(0).getAddressLine(0);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            final String add = address;
                            //noinspection unchecked
                            Map<String, String> members = (Map<String, String>) documentSnapshot.get("family");
                            if (members != null) {
                                for (Map.Entry entry : members.entrySet()) {
                                    db.document(entry.getValue().toString()).get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    String number = documentSnapshot.getString("phone");
                                                    String msg = message + "Address: " + add + " Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude();
                                                    sms.sendTextMessage(number, null, msg + firstName, null, null);
                                                    Log.i(TAG, "Message sent to " + number);
                                                }
                                            });
                                }
                            }
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

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (intent != null) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            final String action = intent.getAction();
//            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
//            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
//            int percentage = (int)((level/ (float) scale)*100);
//            if(percentage <= 20){
//                sendSMS("My Battery is low and less than 20%, ");
//            }
            if (ACTION_DETECT.equals(action)) {
                @SuppressWarnings("unchecked") ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
                // Log each activity.
                Log.i(TAG, "activities detected");
                long requestInterval;
                for (DetectedActivity da : detectedActivities) {
                    String msg;
                    if (da.getConfidence() > 95) {
                        switch (da.getType()) {
                            case DetectedActivity.STILL:
                                msg = "Stationary ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 600000;
                                startTrackingService(requestInterval, msg);
                                break;
                            case DetectedActivity.WALKING:
                                msg = "Walking ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 30000;
                                startTrackingService(requestInterval, msg);
                                break;
                            case DetectedActivity.RUNNING:
                                msg = "Running ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 15000;
                                startTrackingService(requestInterval, msg);
                                break;
                            case DetectedActivity.ON_BICYCLE:
                                msg = "On Bicycle ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 5000;
                                startTrackingService(requestInterval, msg);
                                break;
                            case DetectedActivity.IN_VEHICLE:
                                msg = "On Vehicle ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 1000;
                                startTrackingService(requestInterval, msg);
                                break;
                            case DetectedActivity.ON_FOOT:
                                msg = "On Foot ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 30000;
                                startTrackingService(requestInterval, msg);
                                break;
                            case DetectedActivity.TILTING:
                                msg = "Tilting ";
                                requestInterval = 1000;
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                startTrackingService(requestInterval, msg);
                                break;
                        }
                    }
                }
            } else if (ACTION_PANIC.equals(action)) {
                Log.i(TAG, "Panic Action received.");
                sendSMS("Please Help!!!");
            }
        }
    }
}

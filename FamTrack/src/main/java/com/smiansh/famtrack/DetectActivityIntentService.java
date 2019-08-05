package com.smiansh.famtrack;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class DetectActivityIntentService extends IntentService {
    private static final String ACTION_DETECT = "com.smiansh.famtrack.action.DETECT_ACTIVITY";
    private static final String TAG = "DETECT_ACTIVITY_SERVICE";

    public DetectActivityIntentService() {
        super("DetectActivityIntentService");
    }

    public void startTrackingService(long requestInterval) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent trackingService = new Intent(getApplicationContext(), TrackingService.class);
            stopService(trackingService);
            trackingService.putExtra("requestInterval", requestInterval);
            startForegroundService(trackingService);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            final String action = intent.getAction();
            Helper myHelper = new Helper(getApplicationContext());
            if (ACTION_DETECT.equals(action)) {
                @SuppressWarnings("unchecked") ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
                // Log each activity.
                Log.i(TAG, "activities detected");
                long requestInterval = 60000;
                for (DetectedActivity da : detectedActivities) {
                    String msg = "Stationary";
                    if (da.getConfidence() > 95) {
                        switch (da.getType()) {
                            case DetectedActivity.STILL:
                                msg = "Stationary ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 300000;
                                startTrackingService(requestInterval);
                                break;
                            case DetectedActivity.WALKING:
                                msg = "Walking ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 30000;
                                startTrackingService(requestInterval);
                                break;
                            case DetectedActivity.RUNNING:
                                msg = "Running ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 15000;
                                startTrackingService(requestInterval);
                                break;
                            case DetectedActivity.ON_BICYCLE:
                                msg = "On Bicycle ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 5000;
                                startTrackingService(requestInterval);
                                break;
                            case DetectedActivity.IN_VEHICLE:
                                msg = "On Vehicle ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 1000;
                                startTrackingService(requestInterval);
                                break;
                            case DetectedActivity.ON_FOOT:
                                msg = "On Foot ";
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                requestInterval = 30000;
                                startTrackingService(requestInterval);
                                break;
                            case DetectedActivity.TILTING:
                                msg = "Tilting ";
                                requestInterval = 3000;
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                startTrackingService(requestInterval);
                                break;
                        }
                        sp.edit().putLong("requestInterval", requestInterval).apply();
                        myHelper.sendNote(getString(R.string.app_name), "Detected Activity " + msg + da.getConfidence() + "%\nInterval updated to " + requestInterval);
                    }
                }
            }
        }
    }
}

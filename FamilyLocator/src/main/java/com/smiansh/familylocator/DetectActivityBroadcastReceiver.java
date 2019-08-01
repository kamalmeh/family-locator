package com.smiansh.familylocator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class DetectActivityBroadcastReceiver extends BroadcastReceiver {
    private static final String ACTION_DETECT = "com.smiansh.familylocator.action.DETECT_ACTIVITY";
    private static final String TAG = "DETECT_ACTIVITY_SERVICE";
    private Context mContext;

    public void startTrackingService(long requestInterval, String activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent trackingService = new Intent(mContext, TrackingService.class);
//            mContext.stopService(trackingService);
            trackingService.putExtra("requestInterval", requestInterval);
            trackingService.putExtra("activity", activity);
            mContext.startForegroundService(trackingService);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (intent != null) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            final String action = intent.getAction();
            Helper myHelper = new Helper(context);
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
                                requestInterval = 3000;
                                Log.i(TAG, msg + da.getConfidence() + "%");
                                startTrackingService(requestInterval, msg);
                                break;
                        }
//                        myHelper.sendNote(context.getString(R.string.app_name), msg + da.getConfidence() + "%\nInterval updated to " + requestInterval);
                    }
                }
            }
        }
    }
}

package com.smiansh.famtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class DetectActivityBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_DETECT = "com.smiansh.famtrack.action.DETECT_ACTIVITY";
    public static final String ACTION_PANIC = "com.smiansh.famtrack.action.PANIC_ACTION";
    public static final String ACTION_PANIC_MSG_SENT = "com.smiansh.famtrack.action.PANIC_MSG_SENT";
    public static final String ACTION_PANIC_MSG_DELIVERED = "com.smiansh.famtrack.action.PANIC_MSG_DELIVERED";
    private static final String TAG = "DETECT_ACTIVITY_SERVICE";
    private Context mContext;

    public void startTrackingService(long requestInterval, String activity) {
        Intent trackingService = new Intent(mContext, TrackingService.class);
//        mContext.stopService(trackingService);
        trackingService.putExtra("requestInterval", requestInterval);
        trackingService.putExtra("activity", activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(trackingService);
        } else {
            mContext.startService(trackingService);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        Helper myHelper = new Helper(context);
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
                myHelper.sendSMS("Please Help!!!");
            } else if (ACTION_PANIC_MSG_SENT.equals(action)) {
                Log.i(TAG, "Panic Message Sent");
                Toast.makeText(context, "Message Sent", Toast.LENGTH_SHORT).show();
            } else if (ACTION_PANIC_MSG_DELIVERED.equals(action)) {
                Log.i(TAG, "Panic Message Delivered");
                Toast.makeText(context, "Message Delivered", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

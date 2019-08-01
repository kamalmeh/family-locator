package com.smiansh.familylocator;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class AlarmHandler extends BroadcastReceiver {
    private static final String TAG = "AlarmHandler";
    private Context mContext;

    public AlarmHandler() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        boolean stopAlarmFlag = false;
        Log.i(TAG, "Alarm Event Received");
        String action = intent.getAction();
        Helper myHelper = new Helper(context);
        long miliseconds = intent.getLongExtra("miliseconds", 60000);
        if (action != null) {
            switch (action) {
                case "com.smiansh.familylocator.UPDATE_ALARM":
                    myHelper.createAlarm(miliseconds);
                    break;
                case "android.intent.action.BOOT_COMPLETED":
                    myHelper.createAlarm(60000);
                    break;
                case "android.intent.action.STOP_ALARM":
                    myHelper.destroyAlarm();
                    stopAlarmFlag = true;
                    break;
            }
            if (!stopAlarmFlag) {
                ActivityRecognitionClient activityRecognitionClient = ActivityRecognition.getClient(context);
                Task<Void> task = activityRecognitionClient.requestActivityUpdates(1000, getPendingIntent());
                task.addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Activity Update Requested");
                    }
                });
                task.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    PendingIntent getPendingIntent() {
        Intent detectActivityService = new Intent(mContext, DetectActivityBroadcastReceiver.class);
        detectActivityService.setAction("com.smiansh.familylocator.action.DETECT_ACTIVITY");
        return PendingIntent.getBroadcast(mContext, 10, detectActivityService, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}

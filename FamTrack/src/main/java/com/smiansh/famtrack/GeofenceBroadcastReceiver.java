package com.smiansh.famtrack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.Date;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver implements TextToSpeech.OnInitListener {
    public static final int NOTIFICATION_ID = 3;
    private static final String TAG = "GEOFENCE_RECEIVER";
    private Context mContext;
    private String msg = null;

    // ...
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String memberName = intent.getStringExtra("memberName");
        if (memberName == null || memberName.length() == 0)
            memberName = "Your Family Member";
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(context,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Get the geofences that were triggered. A single event can trigger
        // multiple geofences.
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            msg = " reached ";
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            msg = " left ";
        } else {
            // Log the error.
            Log.e(TAG, context.getString(R.string.geofence_transition_invalid_type,
                    geofenceTransition));
        }

        for (int i = 0; i < geofenceTransition; i++) {
            try {
                sendNotification(memberName, msg + " at " + triggeringGeofences.get(i).getRequestId());
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        // Get the transition details as a String.
        String geofenceTransitionDetails = getGeofenceTransitionDetails(
                geofenceTransition,
                triggeringGeofences
        );
        Log.i(TAG, geofenceTransitionDetails);
    }

    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {
        String tGeos = "";
        for (int i = 0; i < triggeringGeofences.size(); i++)
            try {
                tGeos = tGeos.concat(triggeringGeofences.get(i).toString());
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        return geofenceTransition + "," + tGeos;
    }

    void sendNotification(String sender, String message) {
        String CHANNEL_ID = "TRACKING_SERVICE";
        String CHANNEL = "EMERGENCY_MESSAGE";
        NotificationChannel channel;

        Intent speechIntent = new Intent();
        speechIntent.setClass(mContext, ReadTheMessage.class);
        speechIntent.putExtra("MESSAGE", sender + message);
        speechIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        mContext.startService(speechIntent);

        Intent dismissNotification = new Intent(mContext, AlarmHandler.class);
        dismissNotification.setAction("android.intent.action.DISMISS_NOTIFICATION");
        dismissNotification.putExtra("NOTIFICATION_ID", NOTIFICATION_ID);
        PendingIntent dismissIntent = PendingIntent.getBroadcast(mContext, 0, dismissNotification, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_map_marker_point)
//                .setTimeoutAfter(10000)
//                .setAutoCancel(true)
                .addAction(new NotificationCompat.Action(android.R.drawable.ic_menu_mylocation, "Dismiss", dismissIntent))
//                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        builder.setSound(notificationSound);
        builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
        new NotificationCompat.MessagingStyle(person)
                .setConversationTitle(sender)
                .addMessage(message, new Date().getTime(), person)
                .setBuilder(builder);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    @Override
    public void onInit(int i) {

    }
}
package com.smiansh.famtrack;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.MediaStore;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

import static android.content.Context.ALARM_SERVICE;

class Helper {
    private static String CHANNEL;
    private static String CHANNEL_ID;
    private static AlarmManager am;
    private static PendingIntent alarmIntent;
    private Context context;
    private DocumentReference userData;
    private CollectionReference userColl;
    private CollectionReference authColl;
    private FirebaseUser currUser;

    Helper(Context ctx) {
        context = ctx;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currUser = mAuth.getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        userColl = db.collection("users");
        authColl = db.collection("authcodes");
        try {
            userData = db.collection("users").document(currUser.getUid());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static void setChannel(String channel) {
        Helper.CHANNEL = channel;
    }

    public static void setChannelId(String channelId) {
        CHANNEL_ID = channelId;
    }

    void createAlarm(long miliseconds) {
        if (miliseconds < 60000) {
            miliseconds = 60000;
        }
        am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent service = new Intent(context, AlarmHandler.class);
        service.setAction("com.smiansh.famtrack.UPDATE_ALARM");
        alarmIntent = PendingIntent.getBroadcast(context, 10, service, PendingIntent.FLAG_UPDATE_CURRENT);
        if (am != null) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), miliseconds, alarmIntent);
        }
    }

    void destroyAlarm() {
        am.cancel(alarmIntent);
    }

    void stopTrackingService(Context context) {
        context.stopService(new Intent(context, TrackingService.class));
    }

    BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable != null) {
            vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        }
        Bitmap bitmap = null;
        if (vectorDrawable != null) {
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = null;
        if (bitmap != null) {
            canvas = new Canvas(bitmap);
        }
        if (canvas != null) {
            vectorDrawable.draw(canvas);
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    Bitmap getBitmap(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable != null) {
            vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        }
        Bitmap bitmap = null;
        if (vectorDrawable != null) {
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    Bitmap getBitmap(Uri imageUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(imageUri, projection, null, null, null);
        String path = null;
        if (cursor == null)
            return null;
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            path = cursor.getString(column_index);
        }
        cursor.close();
        if (path == null)
            return null;
        File file = new File(path);
        if (file.canRead()) {
            return BitmapFactory.decodeFile(file.getPath());
        }
        return null;
    }

    CollectionReference getUserColl() {
        return userColl;
    }

    FirebaseUser getCurrUser() {
        return currUser;
    }

    CollectionReference getAuthColl() {
        return authColl;
    }

    DocumentReference getUserData() {
        return userData;
    }

    public void sendNote(String notificationTitle, String notificationMessage) {
        setChannel(context.getString(R.string.channel));
        setChannelId(context.getString(R.string.channel_id));
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID, CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
        }
        NotificationManager notificationManager = null;
        notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_map_marker_point)
                .setContentText(notificationMessage)
                .setContentTitle(notificationTitle)
                .setAutoCancel(true)
                .setTimeoutAfter(10000)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        }
    }
}

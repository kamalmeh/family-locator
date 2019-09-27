package com.smiansh.famtrack;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.ALARM_SERVICE;

class Helper {
    private static String CHANNEL;
    private static String CHANNEL_ID;
    private static AlarmManager am;
    private static PendingIntent alarmIntent;
    private Context context;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private DocumentReference userData;
    private CollectionReference userColl;
    private CollectionReference authColl;
    private FirebaseUser currUser;
    private boolean isAdsEnabled = false;
    private NotificationManager notificationManager;
    static int NOTIFICATION_SERVICE_ID = 1;

    Helper(Context ctx) {
        context = ctx;
        userColl = db.collection("users");
        authColl = db.collection("authcodes");

        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            setAdsEnabled(sp.getBoolean("adsEnabled", true));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private static void setChannel(String channel) {
        Helper.CHANNEL = channel;
    }

    private static void setChannelId(String channelId) {
        CHANNEL_ID = channelId;
    }

    Context getContext() {
        return context;
    }

    @SuppressWarnings("unused")
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

    Bitmap getBitmap() {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_map_marker_point);
        if (vectorDrawable != null) {
            vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        }
        Bitmap bitmap = null;
        if (vectorDrawable != null) {
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    @SuppressWarnings("unused")
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

    Bitmap createCustomMarker(Context context, Bitmap bmp) {

        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);

        CircleImageView markerImage = marker.findViewById(R.id.user_dp);
        if (bmp == null)
            markerImage.setImageResource(R.drawable.ic_boy);
        else
            markerImage.setImageBitmap(bmp);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        marker.draw(canvas);

        return bitmap;
    }

    Notification getNotification() {
        String CHANNEL_ID = context.getString(R.string.channel);
        String CHANNEL = context.getString(R.string.channel_id);
        Notification myNotification;
        NotificationChannel channel;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID, CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("It's My Circle for my dear family members, relatives and friends");
            channel.enableLights(true);
            channel.setLightColor(R.color.colorPrimary);
            channel.setShowBadge(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        Bitmap bmp = getBitmap();
        // Add action button in the notification
        Intent openIntent = new Intent(context, LoginActivity.class);
        if (currUser != null) {
            openIntent.putExtra("userId", currUser.getUid());
        }
//        Intent messageIntent = new Intent(context, MessageActivity.class);
//        messageIntent.putExtra("userId", currUser.getUid());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(openIntent);
//        stackBuilder.addNextIntentWithParentStack(messageIntent);
        PendingIntent pIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent msgIntent = stackBuilder.getPendingIntent(1,PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_map_marker_point)
                .setLargeIcon(bmp)
                .setContentIntent(pIntent)
                .addAction(R.drawable.ic_paper_plane, "Open", pIntent)
//                .addAction(R.drawable.ic_message_black_24dp,"Message",msgIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .setAutoCancel(true)
                .setTimeoutAfter(10000)
//                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        builder.setContentText(context.getString(R.string.notification_message));
        myNotification = builder.build();
        return myNotification;
    }

    void sendNote() {
        setChannel(context.getString(R.string.channel));
        setChannelId(context.getString(R.string.channel_id));
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_SERVICE_ID + 1, this.getNotification());
        }
    }

    void sendSMS(final String message) {
        String userId = currUser.getUid();
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
                            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
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
                                    final String userId = entry.getKey().toString();
                                    final Map<String, Object> emergency_message = new HashMap<>();
                                    emergency_message.put("emergency_message", message);
                                    emergency_message.put("sender", firstName);
                                    emergency_message.put("location", location);
                                    emergency_message.put("address", add);
                                    emergency_message.put("read", "no");
                                    db.document("/emergency/" + userId).update(emergency_message)
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    e.printStackTrace();
                                                    db.document("/emergency/" + userId).set(emergency_message);
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

    CollectionReference getUserColl() {
        return userColl;
    }

    void setCurrUser(FirebaseUser user) {
        currUser = user;
    }

    FirebaseUser getCurrUser() {
        return currUser;
    }

    CollectionReference getAuthColl() {
        return authColl;
    }

    DocumentReference getUserData() {
        try {
            userData = db.collection("users").document(currUser.getUid());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return userData;
    }

    boolean isAdsEnabled() {
        return isAdsEnabled;
    }

    private void setAdsEnabled(boolean adsEnabled) {
        isAdsEnabled = adsEnabled;
    }
}

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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.ALARM_SERVICE;

class Helper {
    static final int ALLOWED_MEMBERS = 1;
    private static AlarmManager am;
    private static PendingIntent alarmIntent;
    private Context context;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference userData;
    private FirebaseUser currUser;
    private NotificationManager notificationManager;
    static int NOTIFICATION_SERVICE_ID = 1;
    SharedPreferences sharedPreferences;

    Helper(Context ctx) {
        context = ctx;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        sharedPreferences = new PrefManager(context).getApplicationDefaultSharedPreferences();
    }

    Context getContext() {
        return context;
    }

    BitmapDescriptor bitmapDescriptorFromDrawable(int vectorResId) {
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

    @SuppressWarnings("unused")
    private Bitmap getBitmap(Uri imageUri) {
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

    Bitmap getBitmapFromPreferences(SharedPreferences sharedPreferences, String title) {
        Bitmap bmp = null;
        try {
            String path = sharedPreferences.getString(title + "_profileImage", null);
            try {
                FileInputStream is;
                if (path != null) {
                    is = new FileInputStream(new File(path));
                    bmp = BitmapFactory.decodeStream(is);
                    is.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bmp;
    }

    Bitmap createCustomMarker(Context context, int vectorResId, Bitmap bmp) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View marker;
        Bitmap bitmap = null;
        if (layoutInflater != null) {
            marker = layoutInflater.inflate(vectorResId, new LinearLayoutCompat(context));

            CircleImageView markerImage = marker.findViewById(R.id.user_dp);
            if (bmp == null)
                markerImage.setImageResource(R.drawable.ic_person_profile_24dp);
            else
                markerImage.setImageBitmap(bmp);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
            marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
            marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
            marker.buildDrawingCache();
            bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            marker.draw(canvas);
        }
        return bitmap;
    }

    Bitmap getBitmapFromDrawable(int vectorResId) {
        Resources res = context.getResources();
        Drawable vectorDrawable = res.getDrawable(vectorResId);
//        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable != null) {
            vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
            vectorDrawable.setAlpha(255);
        }
        Bitmap bitmap = null;
        Canvas canvas = null;
        if (vectorDrawable != null) {
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            Paint alphaPaint = new Paint();
            alphaPaint.setAlpha(255);
// now lets draw using alphaPaint instance
            canvas.drawBitmap(bitmap, 0, 0, alphaPaint);
            vectorDrawable.draw(canvas);
        }
        return bitmap;
    }

    Notification getNotification(String detectedActivity) {
        String CHANNEL_ID = context.getString(R.string.channel);
        String CHANNEL = context.getString(R.string.channel_id);
        Notification myNotification;
        NotificationChannel channel;
        NotificationCompat.Builder notificationCompatBuilder;
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
        Bitmap bmp = getBitmapFromDrawable(R.drawable.ic_map_marker_point);
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
        notificationCompatBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationCompatBuilder.setSmallIcon(R.drawable.ic_map_marker_point)
                .setLargeIcon(bmp)
                .setContentIntent(pIntent)
                .addAction(R.drawable.ic_paper_plane, "Open", pIntent)
//                .addAction(R.drawable.ic_message_black_24dp,"Message",msgIntent)
//                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.notification_message))
                        .setSummaryText(detectedActivity)
                )
                .setAutoCancel(true)
                .setTimeoutAfter(1000)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
//                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN);
        notificationCompatBuilder.setContentText(context.getString(R.string.notification_message));
        myNotification = notificationCompatBuilder.build();
        return myNotification;
    }

    void sendNotification(int id, Notification notification) {
        notificationManager.notify(id, notification);
    }

//    void sendNote() {
//        setChannel(context.getString(R.string.channel));
//        setChannelId(context.getString(R.string.channel_id));
//        if (notificationManager != null) {
//            notificationManager.notify(NOTIFICATION_SERVICE_ID + 1, this.getNotification());
//        }
//    }

    void sendMessage(final String message) {
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

    void setCurrUser(FirebaseUser user) {
        currUser = user;
    }

    DocumentReference getUserData() {
        try {
            userData = db.collection("users").document(currUser.getUid());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return userData;
    }

    String getAddressFromLocation(Location location) {
        String address = "Address: Not Available";
        List<Address> addressList;
        if (location != null) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addressList.size() > 0) {
                    address = addressList.get(0).getAddressLine(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return address;
    }

    InfoWindowData createCustomInfoWindow(GoogleMap mMap, String address, Context ctx) {
        InfoWindowData info = new InfoWindowData();
        info.setAddress(address + "\nClick Me to get Direction");
        CustomInfoWindowGoogleMap customInfoWindow = new CustomInfoWindowGoogleMap(ctx);
        mMap.setInfoWindowAdapter(customInfoWindow);
        return info;
    }

    class CustomInfoWindowGoogleMap implements GoogleMap.InfoWindowAdapter {

        private Context infoWindowContext;

        CustomInfoWindowGoogleMap(Context ctx) {
            infoWindowContext = ctx.getApplicationContext();
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            LinearLayoutCompat linearLayoutCompat = new LinearLayoutCompat(infoWindowContext);
            linearLayoutCompat.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = null;
            if (layoutInflater != null) {
                view = layoutInflater.inflate(R.layout.map_custom_infowindow, linearLayoutCompat);
            }

            TextView name_tv;
            TextView address;
            if (view != null) {
                name_tv = view.findViewById(R.id.name);
                address = view.findViewById(R.id.address);
                InfoWindowData infoWindowData = (InfoWindowData) marker.getTag();
                name_tv.setText(marker.getTitle());
                if (infoWindowData != null) {
                    address.setText(infoWindowData.getAddress());
                }
            }

            return view;
        }
    }
}

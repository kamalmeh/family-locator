package com.smiansh.famtrack;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private boolean isAdsEnabled = false;
    private NotificationManager notificationManager;

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

    Subscription getSubscription() {
        return new Subscription();
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

    private Bitmap getBitmap() {
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

    Notification getNotification() {
        String CHANNEL_ID = context.getString(R.string.app_name);
        String CHANNEL = context.getString(R.string.channel);
        Notification myNotification;
        NotificationChannel channel;
        notificationManager = context.getSystemService(NotificationManager.class);
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
        Intent intent = new Intent(context, DetectActivityBroadcastReceiver.class);
        intent.setAction(DetectActivityBroadcastReceiver.ACTION_PANIC);
        intent.putExtra("EXTRA_NOTIFICATION_ID", 1);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_map_marker_point)
                .setLargeIcon(bmp)
                .setContentIntent(pIntent)
                .addAction(R.drawable.button_style, "Panic Button", pIntent)
                .setStyle(new NotificationCompat.InboxStyle()
                        .addLine("This is your Panic Button.")
                        .addLine("In case you feel unsafe,")
                        .addLine("press it to inform your added members.")
                        .setBigContentTitle("IMPORTANT NOTICE!!!")
                )
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(10000)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        myNotification = builder.build();
        return myNotification;
    }

    void sendNote(String notificationTitle, String notificationMessage) {
        setChannel(context.getString(R.string.channel));
        setChannelId(context.getString(R.string.channel_id));
        if (notificationManager != null) {
            notificationManager.notify(1, getNotification());
        }
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

    boolean isAdsEnabled() {
        return isAdsEnabled;
    }

    private void setAdsEnabled(boolean adsEnabled) {
        isAdsEnabled = adsEnabled;
    }

    public class Subscription implements PurchasesUpdatedListener {
        private final String TAG = "SUBSCRIPTION";
        //        private final String PREMIUM = "android.test.purchased";
        private final String PREMIUM = "premium";
        private BillingClient.Builder builder;
        private BillingClient client;
        private List<SkuDetails> skuDetails;

        Subscription() {
            super();
            billingClient();
        }

        BillingClient getClient() {
            return client;
        }

        @Override
        public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
                Toast.makeText(context, "User cancelled the payment", Toast.LENGTH_SHORT).show();
            } else {
                // Handle any other error codes.
                Toast.makeText(context, billingResult.getDebugMessage() + "\nUnknown error: " + billingResult.getResponseCode(), Toast.LENGTH_SHORT).show();
            }
        }

        void handlePurchase(final Purchase purchase) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                // Acknowledge purchase and grant the item to the user
                final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                final FirebaseFirestore db = FirebaseFirestore.getInstance();
                if (user != null) {
                    db.document("users/" + user.getUid()).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if (documentSnapshot != null) {
                                        Map<String, Object> userData = documentSnapshot.getData();
                                        if (userData != null) {
                                            userData.put("purchaseLicence", purchase.getPurchaseToken());
                                            db.document("users/" + user.getUid()).update(userData);
                                            if (!purchase.isAcknowledged()) {
                                                AcknowledgePurchaseParams acknowledgePurchaseParams =
                                                        AcknowledgePurchaseParams.newBuilder()
                                                                .setPurchaseToken(purchase.getPurchaseToken())
                                                                .build();
                                                client.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                                                    @Override
                                                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                            Log.i(TAG, "Purchase Successful");
                                                        }
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
            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                // Here you can confirm to the user that they've started the pending
                // purchase, and to complete it, they should follow instructions that
                // are given to them. You can also choose to remind the user in the
                // future to complete the purchase if you detect that it is still
                // pending.
            }
        }

        void createBuilder() {
            builder = BillingClient.newBuilder(context).setListener(this);
        }

        void billingClient() {
            createBuilder();
            client = builder
                    .enablePendingPurchases()
                    .setListener(this)
                    .build();
            client.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(final BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                        Toast.makeText(context, "Billing Setup Finished", Toast.LENGTH_SHORT).show();
                        List<String> skuList = new ArrayList<>();
                        skuList.add(PREMIUM);
                        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
                        client.querySkuDetailsAsync(params.build(),
                                new SkuDetailsResponseListener() {
                                    @Override
                                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                                     List<SkuDetails> skuDetailsList) {
                                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                                            skuDetails = skuDetailsList;
                                        }
                                    }
                                });
                    }
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    Toast.makeText(context, "Billing Service Disconnected", Toast.LENGTH_SHORT).show();
//                    TODO: Add Retry billing connection setup
                }
            });
        }

        List<SkuDetails> getSkuDetails() {
            //getBillingClient();
//            List<String> skuList = new ArrayList<>();
//            skuList.add(PREMIUM);
//            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
//            params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
//            billingClient.querySkuDetailsAsync(params.build(),
//                    new SkuDetailsResponseListener() {
//                        @Override
//                        public void onSkuDetailsResponse(BillingResult billingResult,
//                                                         List<SkuDetails> skuDetailsList) {
//                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
//                                skuDetails = skuDetailsList;
//                            }
//                        }
//                    });
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            return skuDetails;
        }
    }
}

package com.smiansh.famtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class UpdateEmergencyMessage extends BroadcastReceiver {
    public static final String ACTION_MARK_READ = "com.smiansh.famtrack.MARK_MSG_READ";
    public static final String ACTION_GET_DIRECTION = "com.smiansh.famtrack.GET_DIRECTION";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Context myContext;

    public UpdateEmergencyMessage() {
        super();
    }

    void markMessageRead(final String userId) {
        db.document("/emergency/" + userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            data.remove("read");
                            data.put("read", "yes");
                            final Map<String, Object> readOnlyData = data;
                            db.document("/emergency/" + userId).update(data)
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            e.printStackTrace();
                                            db.document("/emergency/" + userId).set(readOnlyData);
                                        }
                                    })
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            NotificationManagerCompat.from(myContext).cancel(TrackingService.EMEARGENCY_MESSAGE_ID);
                                        }
                                    });
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
    public void onReceive(final Context context, Intent intent) {
        myContext = context;
        if (intent != null) {
            String action = intent.getAction();
            String userId = intent.getStringExtra("userId");
            if (action != null) {
                if (action.equals(ACTION_MARK_READ)) {
                    markMessageRead(userId);
                } else if (action.equals(ACTION_GET_DIRECTION)) {
                    Uri gmmIntentUri = Uri.parse(intent.getStringExtra("data"));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    context.startActivity(mapIntent);
                    markMessageRead(userId);
                }
            }
        }
    }
}
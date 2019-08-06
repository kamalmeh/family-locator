package com.smiansh.famtrack;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddMemberActivity extends AppCompatActivity {
    String memberId = "test";
    private EditText authCode;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference authRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);
        try {
            MobileAds.initialize(this, getString(R.string.ads));
            AdView mAdView = findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Button addButton = findViewById(R.id.addButton);
        authCode = findViewById(R.id.authCode);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String userId = getIntent().getStringExtra("userId");
                if (userId == null) {
                    Toast.makeText(AddMemberActivity.this, "Invalid User Session", Toast.LENGTH_SHORT).show();
                    return;
                }
                final String memberAuthCode = authCode.getText().toString();
                if (memberAuthCode.isEmpty()) {
                    authCode.setHintTextColor(Color.RED);
                    return;
                }
                authRef = db.collection("authcodes").document(memberAuthCode);
                authRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                memberId = documentSnapshot.getString("userId");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(AddMemberActivity.this,
                                        "Invalid Authentication Code",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final DocumentReference docRef = db.collection("users").document(userId);
                docRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot != null) {
                                    Map<String, String> family;
                                    //noinspection unchecked
                                    family = (Map<String, String>) documentSnapshot.get("family");
                                    if (family == null) {
                                        family = new HashMap<>();
                                    }
                                    family.put(memberId, "/users/" + memberId);
                                    Map<String, Object> familyData = new HashMap<>();
                                    familyData.put("family." + memberId, "/users/" + memberId);
                                    docRef.update(familyData);
//                                    Intent mapActivity = new Intent(getApplicationContext(), MapsActivity.class);
//                                    mapActivity.putExtra("userId", userId);
//                                    startActivity(mapActivity);
                                }
                            }
                        });
                finish();
            }
        });
    }
}

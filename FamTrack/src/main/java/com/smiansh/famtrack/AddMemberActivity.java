package com.smiansh.famtrack;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.Purchase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddMemberActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    Helper myHelper;
    String memberId = "test";
    private EditText authCode;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static int allowedMembers = 0;
    DocumentReference authRef;
    private String userId = FirebaseAuth.getInstance().getUid();
    Button renewButton, upgradeButton, addButton;
    TextView error;
    BillingManager subscription;
    View.OnClickListener renewUpgradeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            subscription.launchBillingWorkflow();
        }
    };
    private Map<String, String> family;
    private boolean licencedProduct = false;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        renewButton.setOnClickListener(renewUpgradeClickListener);
        upgradeButton.setOnClickListener(renewUpgradeClickListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);
        subscription = new BillingManager(this).build();
        licencedProduct = subscription.getMyPurchases().size() > 0;
        myHelper = new Helper(this);
        if (!licencedProduct) {
            try {
                MobileAds.initialize(this, getString(R.string.ads));
                AdView mAdView = findViewById(R.id.adView);
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        addButton = findViewById(R.id.addButton);
        authCode = findViewById(R.id.authCode);
        renewButton = findViewById(R.id.renewButton);
        renewButton.setOnClickListener(renewUpgradeClickListener);
        upgradeButton = findViewById(R.id.upgradeButton);
        upgradeButton.setOnClickListener(renewUpgradeClickListener);
        error = findViewById(R.id.error);
        error.setVisibility(View.GONE);

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
                                if (memberId == null) {
                                    TextView error = findViewById(R.id.error);
                                    error.setText("Invalid Authentication Code");
                                    error.setTextColor(Color.RED);
                                    error.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                    error.setVisibility(View.VISIBLE);
                                } else {
                                    error.setVisibility(View.GONE);
                                    final DocumentReference docRef = db.collection("users").document(userId);
                                    docRef.get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    if (documentSnapshot != null) {
                                                        String purchaseLicence = documentSnapshot.getString("purchaseLicence");
                                                        try {
                                                            String temp = documentSnapshot.getString("allowedMembers");
                                                            if (temp != null) {
                                                                if (temp.length() == 0) {
                                                                    allowedMembers = 0;
                                                                } else {
                                                                    allowedMembers = Integer.parseInt(temp);
                                                                }
                                                            }
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                        //noinspection unchecked
                                                        family = (Map<String, String>) documentSnapshot.get("family");
                                                        if (family == null) {
                                                            family = new HashMap<>();
                                                        }

                                                        List<Purchase> purchaseList = subscription.getMyPurchases();
                                                        if (purchaseList != null) {
                                                            if (purchaseList.size() == 0) {
//                                            Toast.makeText(AddMemberActivity.this, "It seems you don't have any subscription. If you have already subscribed, allow to reflect in the system. You may try after sometime.", Toast.LENGTH_SHORT).show();
//                                            error.setText("It seems you don't have any subscription.\nIf you have already subscribed, it may be expired. \nPlease renew.");
                                                                error.setText("Subscription is deactivated. Please renew.");
                                                                error.setTextColor(Color.RED);
                                                                error.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                                                error.setVisibility(View.VISIBLE);
                                                                authCode.setVisibility(View.GONE);
                                                                addButton.setVisibility(View.GONE);
                                                                renewButton.setVisibility(View.VISIBLE);
                                                                renewButton.setOnClickListener(renewUpgradeClickListener);
//                                            upgradeButton.setVisibility(View.VISIBLE);
                                                            } else if (purchaseList.get(0).getPurchaseToken().equals(purchaseLicence)) {
                                                                if (family.size() >= allowedMembers) {
//                                                Toast.makeText(AddMemberActivity.this, "Maximum Members Allowed: "+ allowedMembers, Toast.LENGTH_LONG).show();
                                                                    error.setText("Maximum Members Allowed: " + allowedMembers);
                                                                    error.setVisibility(View.VISIBLE);
                                                                    upgradeButton.setOnClickListener(renewUpgradeClickListener);
                                                                } else {
                                                                    if (memberId != null) {
                                                                        family.put(memberId, "/users/" + memberId);
                                                                        Map<String, Object> familyData = new HashMap<>();
                                                                        familyData.put("family." + memberId, "/users/" + memberId);
                                                                        docRef.update(familyData);
                                                                        finish();
                                                                    }
                                                                }
                                                            } else {
                                                                subscription.launchBillingWorkflow();
                                                            }
                                                        } else {
                                                            subscription.launchBillingWorkflow();
                                                        }
                                                    }
                                                }
                                            });
                                }
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
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        boolean isSkuDetailsRetrieved = sharedPreferences.getBoolean("isSkuDetailsRetrieved", false);
//        if (isSkuDetailsRetrieved) {
//            List<Purchase> myPurchases = subscription.getMyPurchases();
//            if (myPurchases != null && myPurchases.size() <= 0) {
//                hide.setVisibility(View.GONE);
//                Toast.makeText(this, "Your subscription is expired or not valid. Please purchase or renew.", Toast.LENGTH_LONG).show();
//                if (mMap != null) {
//                    if (mMarkers != null) {
//                        mMap.clear();
//                        mMarkers.clear();
//                        DocumentReference docRef = db.collection("users").document(userId);
//                        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
//                            @Override
//                            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
//                                if (documentSnapshot != null) {
//                                    self = documentSnapshot.getString("firstName");
//                                    if (!hideMyLocation)
//                                        setMarker(documentSnapshot);
//                                }
//                            }
//                        });
//                    }
//                }
//            } else {
//                locateFamily();
//                subscribeToLocations();
//                hide.setVisibility(View.VISIBLE);
//            }
//        }
//        sharedPreferences.edit().putBoolean("isSkuDetailsRetrieved", false).apply();
    }
}
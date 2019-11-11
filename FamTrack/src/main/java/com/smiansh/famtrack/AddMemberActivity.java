package com.smiansh.famtrack;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.smiansh.famtrack.LoginActivity.isTestUser;

public class AddMemberActivity extends AppCompatActivity {
    Helper myHelper;
    String memberId = "test";
    private EditText authCode;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static int allowedMembers = Helper.ALLOWED_MEMBERS;
    DocumentReference authRef;
    Button renewButton, upgradeButton, addButton;
    TextView error;
    BillingManager subscription;
    View.OnClickListener renewUpgradeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            subscription.launchBillingWorkflow();
            finish();
        }
    };
    private Map<String, String> family;

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
        try {
            subscription = new BillingManager(this).build();
            PrefManager prefManager = new PrefManager(this);
            boolean licencedProduct = prefManager.getApplicationDefaultSharedPreferences().getBoolean("isLicenced", false);
            myHelper = new Helper(this);
            if (!licencedProduct || isTestUser) {
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

            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String userId = getIntent().getStringExtra("userId");
                    if (userId == null) {
                        Toast.makeText(AddMemberActivity.this, "Invalid User Session", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String memberAuthCode = "";
                    try {
                        memberAuthCode = authCode.getText().toString();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
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
                                        error.setText(getString(R.string.invalid_auth_code));
                                        error.setTextColor(Color.RED);
                                        error.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                        error.setVisibility(View.VISIBLE);
                                    } else {
                                        if (memberId.equals(userId)) {
                                            TextView error = findViewById(R.id.error);
                                            error.setText(getString(R.string.own_auth_add_error));
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
                                                                    if (isTestUser)
                                                                        allowedMembers = 1;
                                                                    else
                                                                        allowedMembers = Helper.ALLOWED_MEMBERS;
                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                }
                                                                //noinspection unchecked
                                                                family = (Map<String, String>) documentSnapshot.get("family");
                                                                if (family == null) {
                                                                    family = new HashMap<>();
                                                                }

                                                                List<Purchase> purchaseList = subscription.getMyPurchases();
                                                                if (purchaseList != null && !isTestUser) {
                                                                    if (purchaseList.size() == 0) {
//                                            Toast.makeText(AddMemberActivity.this, "It seems you don't have any subscription. If you have already subscribed, allow to reflect in the system. You may try after sometime.", Toast.LENGTH_SHORT).show();
//                                            error.setText("It seems you don't have any subscription.\nIf you have already subscribed, it may be expired. \nPlease renew.");
                                                                        if (family.size() >= allowedMembers) {
                                                                            error.setText(getString(R.string.max_member_error, allowedMembers));
                                                                            error.setVisibility(View.VISIBLE);
                                                                            error.setText(R.string.buy_subs_msg);
                                                                            error.setTextColor(Color.RED);
                                                                            error.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                                                            error.setVisibility(View.VISIBLE);
                                                                            authCode.setVisibility(View.GONE);
                                                                            addButton.setVisibility(View.GONE);
                                                                            renewButton.setVisibility(View.VISIBLE);
                                                                            renewButton.setOnClickListener(renewUpgradeClickListener);
                                                                        } else {
                                                                            if (memberId != null) {
                                                                                family.put(memberId, "/users/" + memberId);
                                                                                Map<String, Object> familyData = new HashMap<>();
                                                                                familyData.put("family." + memberId, "/users/" + memberId);
                                                                                docRef.update(familyData);
                                                                                finish();
                                                                            }
                                                                        }
                                                                    } else if (purchaseList.get(0).getPurchaseToken().equals(purchaseLicence)) {
                                                                        String temp = documentSnapshot.getString("allowedMembers");
                                                                        if (temp != null) {
                                                                            if (temp.length() == 0) {
                                                                                allowedMembers = Helper.ALLOWED_MEMBERS;
                                                                            } else {
                                                                                allowedMembers = Integer.parseInt(temp);
                                                                            }
                                                                        }
                                                                        if (family.size() >= allowedMembers) {
                                                                            error.setText(getString(R.string.max_member_error, allowedMembers));
                                                                            error.setVisibility(View.VISIBLE);
//                                                                    upgradeButton.setOnClickListener(renewUpgradeClickListener);
                                                                        } else {
                                                                            if (memberId != null) {
                                                                                family.put(memberId, "/users/" + memberId);
                                                                                Map<String, Object> familyData = new HashMap<>();
                                                                                familyData.put("family." + memberId, "/users/" + memberId);
                                                                                docRef.update(familyData);
                                                                                finish();
                                                                            }
                                                                        }
                                                                    }
                                                                } else {
                                                                    if (family.size() >= allowedMembers) {
                                                                        Toast.makeText(AddMemberActivity.this, "Maximum Test Members Allowed: " + allowedMembers, Toast.LENGTH_LONG).show();
                                                                        error.setText(getString(R.string.max_member_error, allowedMembers));
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
                                                                }
                                                            }
                                                        }
                                                    });
                                        }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
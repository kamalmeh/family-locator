package com.smiansh.famtrack;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BillingManager implements PurchasesUpdatedListener {
    public static final String PREMIUM = "premium";
    private static List<SkuDetails> skuDetails;
    private static List<Purchase> myPurchases;
    private Activity context;
    private BillingClient.Builder builder;
    private BillingClient client;
    private String TAG = "BILLING_MANAGER";

    BillingManager(Activity activity) {
        super();
        context = activity;
    }

    private BillingClient getClient() {
        return client;
    }

    void launchBillingWorkflow() {
        List<SkuDetails> skuDetailsList = getSkuDetails();
        BillingFlowParams.Builder flowParams = BillingFlowParams.newBuilder();
        try {
            flowParams.setSkuDetails(skuDetailsList.get(0));
            BillingFlowParams params = flowParams.build();
            BillingClient client = getClient();
            BillingResult responseCode = client.launchBillingFlow(context, params);
            if (responseCode.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Flow Start Successful");
            } else {
                Log.i(TAG, "Flow Start Error" + responseCode.getDebugMessage());
            }
        } catch (NullPointerException e) {
            Toast.makeText(context, "Subscriptions are unavailable to this device", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
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
            Log.i(TAG, "User cancelled the payment");
        } else {
            // Handle any other error codes.
            Toast.makeText(context, billingResult.getDebugMessage() + "\nUnknown error: " + billingResult.getResponseCode(), Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Unknown Error: " + billingResult.getDebugMessage());
        }
    }

    private void handlePurchase(final Purchase purchase) {
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
                                        String purchaseToken = purchase.getPurchaseToken();
                                        userData.put("purchaseLicence", purchaseToken);
                                        userData.put("allowedMembers", "5");
                                        userData.put("userType", "regular");
                                        db.document("users/" + user.getUid()).set(userData, SetOptions.merge());
                                        if (!purchase.isAcknowledged()) {
                                            AcknowledgePurchaseParams acknowledgePurchaseParams =
                                                    AcknowledgePurchaseParams.newBuilder()
                                                            .setPurchaseToken(purchaseToken)
                                                            .build();
                                            client.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                                                @Override
                                                public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                        Log.i(TAG, "Purchase Successful");
                                                    }
                                                }
                                            });
                                            refreshPurchases();
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
            Log.i(TAG, "Pending Purchase");
        }
    }

    private void createBuilder() {
        builder = BillingClient.newBuilder(context).setListener(this);
    }

    public BillingManager build() {
        createBuilder();
        client = builder
                .enablePendingPurchases()
                .setListener(this)
                .build();
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(final BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                    Toast.makeText(context, "Billing Setup Finished", Toast.LENGTH_SHORT).show();
                    refreshPurchases();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Toast.makeText(context, "Billing Service Disconnected", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Billind Service Disconnected");
//                    TODO: Add Retry billing connection setup
            }
        });
        return this;
    }

    void refreshPurchases() {
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
                            setSkuDetails(skuDetailsList);
                            Purchase.PurchasesResult purchasesResult = client.queryPurchases(BillingClient.SkuType.SUBS);
                            if (purchasesResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchasesResult.getPurchasesList() != null) {
                                setMyPurchases(client.queryPurchases(BillingClient.SkuType.SUBS).getPurchasesList());
                                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean("isSkuDetailsRetrieved", true).apply();
                                if (myPurchases.size() > 0) {
                                    String purchase = myPurchases.get(0).toString();
                                    editor.putString("myPurchase", purchase).apply();
                                }
                            }
                        }
                    }
                });
    }

    List<SkuDetails> getSkuDetails() {
        return skuDetails;
    }

    private void setSkuDetails(List<SkuDetails> skuList) {
        skuDetails = skuList;
    }

    List<Purchase> getMyPurchases() {
        return myPurchases;
    }

    private void setMyPurchases(List<Purchase> purchaseList) {
        myPurchases = purchaseList;
    }
}

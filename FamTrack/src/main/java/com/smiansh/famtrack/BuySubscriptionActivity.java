package com.smiansh.famtrack;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

public class BuySubscriptionActivity extends AppCompatActivity {
    private final String TAG = "BUY_SUBSCRIPTION_ACTIVITY";
    private Helper myHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_subscription);
        myHelper = new Helper(this);
        Button buy = findViewById(R.id.buy);

        buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper.Subscription subscription = myHelper.getSubscription();
                List<SkuDetails> skuDetailsList = subscription.getSkuDetails();
                BillingFlowParams.Builder flowParams = BillingFlowParams.newBuilder();
                try {
                    flowParams.setSkuDetails(skuDetailsList.get(0));
                    BillingFlowParams params = flowParams.build();
                    BillingClient client = subscription.getBillingClient();
                    BillingResult responseCode = client.launchBillingFlow(BuySubscriptionActivity.this, params);
                    if (responseCode.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                        Toast.makeText(BuySubscriptionActivity.this, "Flow Start Successful", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Flow Start Successful");
                    } else {
//                        Toast.makeText(BuySubscriptionActivity.this, "Flow Start Error", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Flow Start Error");
                    }
                } catch (NullPointerException e) {
                    Toast.makeText(BuySubscriptionActivity.this, "Subscriptions are under development", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }
}

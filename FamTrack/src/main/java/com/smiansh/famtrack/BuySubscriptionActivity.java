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
    private final String TAG = "BUY_SUBSCRIPTION";
    Helper.Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_subscription);
        Helper myHelper = new Helper(this);
        subscription = myHelper.getSubscription();
        Button buy = findViewById(R.id.buy);

        buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<SkuDetails> skuDetailsList = subscription.getSkuDetails();
                BillingFlowParams.Builder flowParams = BillingFlowParams.newBuilder();
                try {
                    flowParams.setSkuDetails(skuDetailsList.get(0));
//                    SkuDetails product = new SkuDetails("{'productId':'android.test.purchased','type':'Subscription','price':'199','price_amount_micros':'123','price_currency_code':'321','title':'Test Product','description':'Test Product'}");
//                    flowParams.setSkuDetails(product);
                    BillingFlowParams params = flowParams.build();
                    BillingClient client = subscription.getClient();
                    BillingResult responseCode = client.launchBillingFlow(BuySubscriptionActivity.this, params);
                    if (responseCode.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                        Toast.makeText(BuySubscriptionActivity.this, "Flow Start Successful", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Flow Start Successful");
                    } else {
//                        Toast.makeText(BuySubscriptionActivity.this, "Flow Start Error", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Flow Start Error" + responseCode.getDebugMessage());
                    }
                } catch (NullPointerException e) {
                    Toast.makeText(BuySubscriptionActivity.this, "Subscriptions are under development", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }
}

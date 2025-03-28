package com.smiansh.famtrack;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class ShareCodeActivity extends AppCompatActivity {
    static boolean licencedProduct = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_code);
//        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        String authCodeText = getIntent().getStringExtra("authCode");
        TextView authCode = findViewById(R.id.authCode);
        authCode.setLetterSpacing(Float.parseFloat("0.3"));
        authCode.setText(authCodeText);
        Helper myHelper = new Helper(this);
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
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

package com.smiansh.famtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class ShareCodeActivity extends AppCompatActivity {
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_code);
        userId = getIntent().getStringExtra("userId");
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        String authCodeText = getIntent().getStringExtra("authCode");
        TextView authCode = findViewById(R.id.authCode);
        authCode.setText(authCodeText);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent mapActivity = new Intent(this, MapsActivity.class);
        mapActivity.putExtra("userId", userId);
        startActivity(mapActivity);
        finish();
    }
}

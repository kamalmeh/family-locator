package com.smiansh.famtrack;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

//import android.widget.RatingBar;

public class RatingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);
//        final RatingBar simpleRatingBar = findViewById(R.id.ratingBar);
        Button submitButton = findViewById(R.id.submitRating);
        // perform click event on button
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefManager prefManager = new PrefManager(getApplicationContext());
                SharedPreferences.Editor ed = prefManager.getEditor();
                ed.putBoolean("isRatingGiven", true).apply();
                // get values and then displayed in a toast
//                String totalStars = "Total Stars:: " + simpleRatingBar.getNumStars();
//                String rating = "Rating :: " + simpleRatingBar.getRating();
//                Toast.makeText(getApplicationContext(), totalStars + "\n" + rating, Toast.LENGTH_LONG).show();
//                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Uri uri = Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName());
                Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(myAppLinkToMarket);
                    finish();
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(RatingActivity.this, getResources().getText(R.string.app_not_found), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}

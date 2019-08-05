package com.smiansh.familylocator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "Login Activity";
    private static final int RC_SIGN_IN = 1001;
    private static final int PERMISSION_REQUEST = 10;
    View.OnClickListener exitClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }
    };
    private Button login, exit;
    View.OnClickListener loginClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (login.getText().equals("Login")) {
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.PhoneBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build(),
                        new AuthUI.IdpConfig.FacebookBuilder().build());

                // Create and launch sign-in intent
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .setIsSmartLockEnabled(true)
                                .build(),
                        RC_SIGN_IN);
            } else {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    startMapActivity(user.getUid());
                }
            }
        }
    };

    @Override
    protected void onPostResume() {
        super.onPostResume();
        login.setOnClickListener(loginClickListener);
        exit.setOnClickListener(exitClickListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        login = findViewById(R.id.login);
        exit = findViewById(R.id.exit);
        exit.setOnClickListener(exitClickListener);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    int fine_location = ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                    if (fine_location != PackageManager.PERMISSION_GRANTED) {
                        String[] permissions = new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        };
                        ActivityCompat.requestPermissions(LoginActivity.this, permissions,
                                PERMISSION_REQUEST);
                    } else {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            login.setText("Find Family");
                            startMapActivity(user.getUid());
                        } else {
                            login.setText("Login");
                        }
                        login.setOnClickListener(loginClickListener);
                    }
                }
            });
        } else {
            login.setOnClickListener(loginClickListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
//            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                login.setText("Find Family");
                login.setOnClickListener(loginClickListener);
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    final String userId = user.getUid();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.document("/users/" + userId).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if (documentSnapshot.get("firstName") != null) {
                                        startMapActivity(userId);
                                        stopService(getIntent());
                                    } else {
                                        startMapActivity(userId);
                                        startProfileActivity(userId);
                                        stopService(getIntent());
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
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Log.i(TAG, "Login Failure");
            }
        }
    }

    protected void startProfileActivity(String userId) {
        Intent profileActivityIntent = new Intent(LoginActivity.this, ProfileActivity.class);
        profileActivityIntent.putExtra("userId", userId);
        startActivity(profileActivityIntent);
    }

    protected void startMapActivity(String userId) {
        Intent mapActivityIntent = new Intent(LoginActivity.this, MapsActivity.class);
        mapActivityIntent.putExtra("userId", userId);
        startActivity(mapActivityIntent);
    }
}

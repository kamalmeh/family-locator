package com.smiansh.famtrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "Login Activity";
    private static final int RC_SIGN_IN = 1001;
    private static final int PERMISSION_REQUEST = 10;
    View.OnClickListener panicClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                Helper myHelper = new Helper(getApplicationContext());
                myHelper.sendSMS("Please Help!!!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private GoogleMap mMap;
    View.OnClickListener exitClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }
    };
    private Location currentLocation;
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
    GoogleMap.OnCameraMoveStartedListener moveStartedListener = new GoogleMap.OnCameraMoveStartedListener() {
        @Override
        public void onCameraMoveStarted(int i) {
            mMap.stopAnimation();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 13));
        }
    };
    private Button login, exit, panic;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        login.setOnClickListener(loginClickListener);
        exit.setOnClickListener(exitClickListener);
        panic.setOnClickListener(panicClickListener);
        if (mMap != null)
            mMap.setOnCameraMoveStartedListener(moveStartedListener);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            login.setText("Login");
        }
    }

    void isLoggedIn() {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        try {
            assert mapFragment != null;
            mapFragment.getMapAsync(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        login = findViewById(R.id.login);
        exit = findViewById(R.id.exit);
        exit.setOnClickListener(exitClickListener);
        panic = findViewById(R.id.panicButton);
        panic.setOnClickListener(panicClickListener);
        panic.setVisibility(View.INVISIBLE); //Don't remove. It will be used to send notifications to members

        isLoggedIn();
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

    @Override
    protected void onResume() {
        super.onResume();
        requestLocation();
        if (mMap != null)
            mMap.setOnCameraMoveStartedListener(moveStartedListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else {
                if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                        (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted.
                    Log.i(TAG, "Permission was granted");
                } else {
                    Snackbar.make(
                            findViewById(R.id.map),
                            R.string.permission_denied_explanation,
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.settings, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Build intent that displays the App settings screen.
                                    Intent intent = new Intent();
                                    intent.setAction(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package",
                                            BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            })
                            .show();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        requestLocation();
    }

    void requestLocation() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}
                    , PERMISSION_REQUEST);
        }
        client.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Helper myHelper = new Helper(getApplicationContext());
                currentLocation = locationResult.getLastLocation();
                LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                MarkerOptions mOpt = new MarkerOptions().title("You Are Here").position(latLng);
                mOpt.icon(BitmapDescriptorFactory.fromBitmap(myHelper.createCustomMarker(LoginActivity.this, myHelper.getBitmap())));
                mMap.addMarker(mOpt).showInfoWindow();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                mMap.setOnCameraMoveStartedListener(moveStartedListener);
            }
        }, null);
    }
}

package com.smiansh.famtrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.login.LoginManager;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "Login Activity";
    private static final int RC_SIGN_IN = 1001;
    private static final int PERMISSION_REQUEST = 10;
    private static Marker myMarker;
    private Helper myHelper;
    LinearLayout linearLayout;
    SupportMapFragment mapFragment;
    private boolean linearLayoutShow = true;
    public static boolean isTestUser = false;
    private BillingManager subscription;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = mAuth.getCurrentUser();
    final View.OnClickListener loginClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (login.getText().equals("Login")) {
                try {

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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (user != null) {
                    if (login.getText().equals("Find Family")) {
                        startMapActivity(user.getUid());
                    } else checkIfAlreadyLoggedIn();
                }
            }
        }
    };

    View.OnClickListener panicClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
//                Helper myHelper = new Helper(getApplicationContext());
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
    private FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            int fine_location = ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (fine_location != PackageManager.PERMISSION_GRANTED) {
                String[] permissions;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissions = new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACTIVITY_RECOGNITION
                    };
                } else {
                    permissions = new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    };
                }
                ActivityCompat.requestPermissions(LoginActivity.this, permissions,
                        PERMISSION_REQUEST);
            } else {
//                if (firebaseAuth.getCurrentUser() != null) {
//                    user=firebaseAuth.getCurrentUser();
//                    login.setText("Find Family");
//                    panic.setVisibility(View.VISIBLE);
//                } else {
//                    login.setText("Login");
//                    panic.setVisibility(View.INVISIBLE);
//                    user = null;
//                }
                login.setOnClickListener(loginClickListener);
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

    void isLoggedIn() {
        if (user != null) {
            if (myHelper == null)
                myHelper = new Helper(this);
            myHelper.setCurrUser(user);
            myHelper.createAlarm(60000);
            mAuth.addAuthStateListener(authStateListener);
        } else {
            login.setOnClickListener(loginClickListener);
        }
    }

    private void logout() {
        if (login.getText().equals("Logout")) {
            Log.i(TAG, "Signing out...");
            if (user != null) {
                db.collection("users").document(user.getUid()).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                Map<String, Object> data = documentSnapshot.getData();
                                if (data != null) {
                                    data.put("status", "Signed Out");
                                    db.collection("users").document(user.getUid()).set(data, SetOptions.merge())
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    FirebaseAuth.getInstance().signOut();
                                                    LoginManager.getInstance().logOut();
                                                    AuthUI.getInstance().signOut(getApplicationContext());
                                                    Log.i(TAG, "Signed out...");
                                                    finish();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(LoginActivity.this, "Could not sign out the user", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
                myHelper.destroyAlarm();
                myHelper.stopTrackingService(LoginActivity.this);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        try {
            assert mapFragment != null;
            mapFragment.getMapAsync(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        myHelper = new Helper(getApplicationContext());
        login = findViewById(R.id.login);
        exit = findViewById(R.id.exit);
        exit.setOnClickListener(exitClickListener);
        panic = findViewById(R.id.panicButton);
        panic.setOnClickListener(panicClickListener);
        panic.setVisibility(View.INVISIBLE);

        isLoggedIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Successfully signed in
                login.setOnClickListener(loginClickListener);
                user = mAuth.getCurrentUser();
                if (user != null) {
                    checkIfAlreadyLoggedIn();
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

    private void checkIfAlreadyLoggedIn() {
        db.document("/users/" + user.getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(final DocumentSnapshot documentSnapshot) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            data.put("status", "Signed In");
                            db.document("/users/" + user.getUid()).set(data, SetOptions.merge())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
//                                                Toast.makeText(LoginActivity.this, "Successfully Signed In", Toast.LENGTH_SHORT).show();
                                            // Successfully signed in
                                            login.setText("Find Family");
                                            panic.setVisibility(View.VISIBLE);
                                            linearLayout.removeAllViews();
                                            linearLayout.setVisibility(View.GONE);
                                            login.setEnabled(true);
                                            subscription = new BillingManager(LoginActivity.this).build();
                                            String userType = documentSnapshot.getString("userType");
                                            if (userType != null && userType.equals("test"))
                                                isTestUser = true;
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            e.printStackTrace();
                                            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }

                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String userType = documentSnapshot.getString("userType");
                        if (firstName != null && lastName != null && userType != null) {
                            if (firstName.equals("") || lastName.equals("")) {
                                startProfileActivity(user.getUid());
                                stopService(getIntent());
                            }
                        } else {
                            startProfileActivity(user.getUid());
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

        if (linearLayoutShow) {
            linearLayoutShow = false;
            // Create progressBar dynamically...
            final ProgressBar progressBar = new ProgressBar(this);
            progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            linearLayout = findViewById(R.id.rootContainer);
            // Add ProgressBar to LinearLayout
            if (linearLayout != null) {
                linearLayout.addView(progressBar);
                linearLayout.setVisibility(View.VISIBLE);
                login.setEnabled(false);
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
        mAuth.addAuthStateListener(authStateListener);
        requestLocation();
        if (mMap != null)
            mMap.setOnCameraMoveStartedListener(moveStartedListener);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        login.setOnClickListener(loginClickListener);
        exit.setOnClickListener(exitClickListener);
        panic.setOnClickListener(panicClickListener);
        mAuth.addAuthStateListener(authStateListener);
        if (mMap != null)
            mMap.setOnCameraMoveStartedListener(moveStartedListener);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            login.setText("Login");
            panic.setVisibility(View.GONE);
            linearLayoutShow = true;
        } else {
            checkIfAlreadyLoggedIn();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else {
                if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSION_REQUEST);
                        }
                    } else {
                        Log.i(TAG, "Permission was granted");
                    }
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
        mMap.getUiSettings().setScrollGesturesEnabled(false);
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mMap.getUiSettings().setScrollGesturesEnabled(true);
            }
        });
    }

    void requestLocation() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION};

            } else {
                permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};
            }
            ActivityCompat.requestPermissions(this,
                    permissions,
                    PERMISSION_REQUEST);
        }
        client.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                MarkerOptions mOpt = new MarkerOptions().title("You Are Here").position(latLng);
                mOpt.icon(BitmapDescriptorFactory.fromBitmap(myHelper.createCustomMarker(LoginActivity.this, myHelper.getBitmap())));
                if (myMarker == null) {
                    myMarker = mMap.addMarker(mOpt);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                } else
                    myMarker.setPosition(latLng);
                myMarker.showInfoWindow();
                mMap.setOnCameraMoveStartedListener(moveStartedListener);
            }
        }, null);
    }
}

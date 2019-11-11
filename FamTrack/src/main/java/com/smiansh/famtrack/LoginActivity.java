package com.smiansh.famtrack;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class LoginActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "LOGIN_ACTIVITY";
    private static final int RC_SIGN_IN = 1001;
    private static final int PERMISSION_REQUEST1 = 1;
    private static final int PERMISSION_REQUEST2 = 2;
    private static Marker myMarker;
    private static MarkerOptions mOpt;
    private Helper myHelper;
    LinearLayout linearLayout;
    SupportMapFragment mapFragment;
    private boolean linearLayoutShow = true;
    public static boolean isTestUser = false;
    private BillingManager subscription;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = mAuth.getCurrentUser();
    GoogleMap.OnMarkerClickListener onMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
//            nearbyPlaces.setVisibility(View.INVISIBLE);
//            infoButton.setVisibility(View.INVISIBLE);
//            clearMap.setVisibility(View.INVISIBLE);
            return false;
        }
    };
    GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
//            nearbyPlaces.setVisibility(View.VISIBLE);
//            infoButton.setVisibility(View.VISIBLE);
//            clearMap.setVisibility(View.VISIBLE);
        }
    };
    private String self = "";
    //    private AutocompleteSupportFragment autocompleteFragment;
//    private PlacesClient placesClient;
    View.OnClickListener panicClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            myHelper.sendMessage("Please Help!!!!");
        }
    };
    private PrefManager prefManager;
    private SharedPreferences mySharedPreferences;
    private GoogleMap mMap;

    //    View.OnClickListener nearByButtonClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            linearLayoutShow = true;
//            // Use fields to define the data types to return.
//            List<Place.Field> placeFields = new ArrayList<>();
//            placeFields.add(Place.Field.ID);
//            placeFields.add(Place.Field.NAME);
//            placeFields.add(Place.Field.ADDRESS);
//            placeFields.add(Place.Field.PHOTO_METADATAS);
//            placeFields.add(Place.Field.LAT_LNG);
//            placeFields.add(Place.Field.TYPES);
//
//// Use the builder to create a FindCurrentPlaceRequest.
//            FindCurrentPlaceRequest request =
//                    FindCurrentPlaceRequest.newInstance(placeFields);
//
//// Call findCurrentPlace and handle the response (first check that the user has granted permission).
//            if (ContextCompat.checkSelfPermission(LoginActivity.this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
//                placeResponse.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
//                    @Override
//                    public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
//                        linearLayout.removeAllViews();
//                        linearLayout.setVisibility(View.GONE);
////                        nearbyPlaces.setEnabled(true);
//                        if (task.isSuccessful()) {
//                            FindCurrentPlaceResponse response = task.getResult();
//                            if (response != null) {
//                                for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
//                                    setPlaceMarker(placeLikelihood.getPlace());
//                                    Log.i(TAG, String.format("Place '%s' has likelihood: %f",
//                                            placeLikelihood.getPlace().getName(),
//                                            placeLikelihood.getLikelihood()));
//                                }
//                            }
//                        } else {
//                            Exception exception = task.getException();
//                            if (exception instanceof ApiException) {
//                                ApiException apiException = (ApiException) exception;
//                                Log.e(TAG, "Place not found: " + apiException.getStatusCode());
//                                Toast.makeText(LoginActivity.this, apiException.getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }
//                });
//            } else {
//                // A local method to request required permissions;
//                // See https://developer.android.com/training/permissions/requesting
////                getLocationPermission();
//                Snackbar.make(
//                        findViewById(R.id.map),
//                        R.string.permission_denied_explanation,
//                        Snackbar.LENGTH_INDEFINITE)
//                        .setAction(R.string.settings, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                // Build intent that displays the App settings screen.
//                                Intent intent = new Intent();
//                                intent.setAction(
//                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                Uri uri = Uri.fromParts("package",
//                                        BuildConfig.APPLICATION_ID, null);
//                                intent.setData(uri);
//                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                startActivity(intent);
//                            }
//                        })
//                        .show();
//            }
//            if (linearLayoutShow) {
//                linearLayoutShow = false;
//                // Create progressBar dynamically...
//                final ProgressBar progressBar = new ProgressBar(LoginActivity.this);
//                progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//
//                linearLayout = findViewById(R.id.rootContainer);
//                // Add ProgressBar to LinearLayout
//                if (linearLayout != null) {
//                    linearLayout.addView(progressBar);
//                    linearLayout.setVisibility(View.VISIBLE);
////                    nearbyPlaces.setEnabled(false);
//                }
//            }
//        }
//    };
    private Button login, panic;

    private Location currentLocation;
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
                        login.setEnabled(false);
                        startMapActivity(user.getUid());
                    } else checkIfAlreadyLoggedIn();
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
    private FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            int fine_location = ContextCompat.checkSelfPermission(LoginActivity.this, ACCESS_FINE_LOCATION);
            int permission_read = ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int permission_write = ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (fine_location != PackageManager.PERMISSION_GRANTED || permission_read != PackageManager.PERMISSION_GRANTED ||
                    permission_write != PackageManager.PERMISSION_GRANTED) {
                String[] permissions;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissions = new String[]{
                            ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACTIVITY_RECOGNITION
                    };
                } else {
                    permissions = new String[]{
                            ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    };
                }
                ActivityCompat.requestPermissions(LoginActivity.this, permissions,
                        PERMISSION_REQUEST2);
            } else {
                login.setOnClickListener(loginClickListener);
            }
        }
    };
    private ArrayList<Marker> placeMarkers = new ArrayList<>();
//    private ExtendedFloatingActionButton nearbyPlaces, clearMap, infoButton;
//    private View.OnClickListener clearMapClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            mMap.clear();
//            myMarker=null;
//            placeMarkers.clear();
//            requestLocation();
//        }
//    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        prefManager = new PrefManager(this);
        subscription = new BillingManager(LoginActivity.this).build();
        mySharedPreferences = prefManager.getApplicationDefaultSharedPreferences();
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
        panic = findViewById(R.id.panicButton);
        panic.setOnClickListener(panicClickListener);
        panic.setVisibility(View.INVISIBLE);
//        infoButton = findViewById(R.id.help);
//        clearMap = findViewById(R.id.clean_map);
//        clearMap.setOnClickListener(clearMapClickListener);
//        nearbyPlaces = findViewById(R.id.nearby_places);
//        nearbyPlaces.setOnClickListener(nearByButtonClickListener);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View marker = inflater.inflate(R.layout.custom_places_layout, new LinearLayoutCompat(this));
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
            marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
            marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
            marker.buildDrawingCache();
            Bitmap bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            marker.draw(canvas);
//            nearbyPlaces.setIcon(new BitmapDrawable(getResources(), bitmap));
        }

        if (!Places.isInitialized())
            Places.initialize(this, "AIzaSyAi99s5tBFdSfQXExGwFGoy6tDFeh0Cxwg");
//        placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment.
//        autocompleteFragment =
//                (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        isLoggedIn();
        // ATTENTION: This was auto-generated to handle app links.
//        Intent appLinkIntent = getIntent();
//        String appLinkAction = appLinkIntent.getAction();
//        Uri appLinkData = appLinkIntent.getData();

    }

    private void setPlaceMarker(Place place) {
        if (place != null) {
            LatLng latLng = place.getLatLng();
            String name = place.getName();
            List<Place.Type> placeTypes = place.getTypes();
            if (latLng != null) {
                if (placeMarkers != null) {
                    if (placeTypes != null && placeTypes.size() > 0) {
                        Place.Type type = placeTypes.get(0);
                        for (int i = 0; i < placeTypes.size(); i++) {
                            Log.i("TYPE: ", placeTypes.get(i).name());
                            Log.i(TAG, place.getName() + "");
                        }
                        BitmapDescriptor bitmapDescriptor;
                        switch (type.name()) {
                            case "CAFE":
                                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                                        myHelper.createCustomMarker(
                                                LoginActivity.this,
                                                R.layout.custom_place_marker_layout,
                                                myHelper.getBitmapFromDrawable(R.drawable.ic_cafe_24dp)
                                        )
                                );
                                break;
                            case "FOOD":
                            case "RESTAURANT":
                                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                                        myHelper.createCustomMarker(
                                                LoginActivity.this,
                                                R.layout.custom_place_marker_layout,
                                                myHelper.getBitmapFromDrawable(R.drawable.ic_restaurant_24dp)
                                        )
                                );
                                break;
                            case "FURNITURE_STORE":
                            case "GENERAL_CONTRACTOR":
                            case "HOME_GOODS_STORE":
                                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                                        myHelper.createCustomMarker(
                                                LoginActivity.this,
                                                R.layout.custom_place_marker_layout,
                                                myHelper.getBitmapFromDrawable(R.drawable.ic_furniture_icon)
                                        )
                                );
                                break;
                            case "LODGING":
                                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                                        myHelper.createCustomMarker(
                                                LoginActivity.this,
                                                R.layout.custom_place_marker_layout,
                                                myHelper.getBitmapFromDrawable(R.drawable.ic_hotel_24dp)
                                        )
                                );
                                break;
                            case "PET_STORE":
                            case "VETERINARY_CARE":
                                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                                        myHelper.createCustomMarker(
                                                LoginActivity.this,
                                                R.layout.custom_place_marker_layout,
                                                myHelper.getBitmapFromDrawable(R.drawable.ic_pets_24dp)
                                        )
                                );
                                break;
                            case "HEALTH":
                            case "HOSPITAL":
                            case "PHARMACY":
                                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                                        myHelper.createCustomMarker(
                                                LoginActivity.this,
                                                R.layout.custom_place_marker_layout,
                                                myHelper.getBitmapFromDrawable(R.drawable.ic_local_hospital_24dp)
                                        )
                                );
                                break;
                            case "GYM":
                                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                                        myHelper.createCustomMarker(
                                                LoginActivity.this,
                                                R.layout.custom_place_marker_layout,
                                                myHelper.getBitmapFromDrawable(R.drawable.ic_fitness_center_24dp)
                                        )
                                );
                                break;
                            case "STORE":
                                bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                                        myHelper.createCustomMarker(
                                                LoginActivity.this,
                                                R.layout.custom_place_marker_layout,
                                                myHelper.getBitmapFromDrawable(R.drawable.ic_grocery_store_24dp)
                                        )
                                );
                                break;
                            case "ESTABLISHMENT":
                            case "POINT_OF_INTEREST":
                            default:
                                bitmapDescriptor = BitmapDescriptorFactory.defaultMarker();
                                break;
                        }
                        placeMarkers.add(
                                mMap.addMarker(
                                        new MarkerOptions()
                                                .title(name)
                                                .position(latLng)
                                                .icon(bitmapDescriptor)
                                )
                        );
                    }

                    if (placeMarkers.size() > 0) {
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (int i = 0; i < placeMarkers.size(); i++) {
                            Marker marker = placeMarkers.get(i);
                            Location location = new Location(LocationManager.GPS_PROVIDER);
                            if (marker != null) {
                                builder.include(marker.getPosition());
                                location.setLongitude(marker.getPosition().longitude);
                                location.setLatitude(marker.getPosition().latitude);
                                InfoWindowData info =
                                        myHelper.createCustomInfoWindow(mMap,
                                                myHelper.getAddressFromLocation(location),
                                                getApplicationContext()
                                        );
                                marker.setTag(info);
                                marker.setVisible(true);
                                marker.hideInfoWindow();
                            }
                        }
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 170));
                    } else {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                    mMap.setOnCameraMoveStartedListener(moveStartedListener);
                }
            }
        }
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

                        FirebaseInstanceId.getInstance().getInstanceId()
                                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                        if (!task.isSuccessful()) {
                                            Log.w(TAG, "getInstanceId failed", task.getException());
                                            return;
                                        }

                                        // Get new Instance ID token
                                        String token = "";
                                        if (task.getResult() != null)
                                            token = task.getResult().getToken();

                                        Map<String, Object> data = documentSnapshot.getData();
                                        if (data != null) {
                                            data.put("status", "Signed In");
                                            data.put("android_version", Build.VERSION.SDK_INT);
                                            data.put("FirebaseInstanceId", token);
                                            db.document("/users/" + user.getUid()).set(data, SetOptions.merge())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
//                                                Toast.makeText(LoginActivity.this, "Successfully Signed In", Toast.LENGTH_SHORT).show();
                                                            // Successfully signed in
                                                            login.setText(R.string.btn_find_family);
                                                            panic.setVisibility(View.VISIBLE);
                                                            linearLayout.removeAllViews();
                                                            linearLayout.setVisibility(View.GONE);
                                                            login.setEnabled(true);
                                                            boolean isLicenced = subscription.getMyPurchases().size() > 0;
                                                            prefManager.editor.putBoolean("isLicenced", isLicenced).apply();
                                                            String userType = documentSnapshot.getString("userType");
                                                            self = documentSnapshot.getString("firstName");
                                                            if (myMarker != null) {
                                                                myMarker.setIcon(BitmapDescriptorFactory.fromBitmap(myHelper.createCustomMarker(LoginActivity.this, R.layout.custom_marker_layout, myHelper.getBitmapFromPreferences(mySharedPreferences, self))));
                                                            }
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
                                    }
                                });

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
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        login.setOnClickListener(loginClickListener);
        panic.setOnClickListener(panicClickListener);
        mAuth.addAuthStateListener(authStateListener);
        if (mMap != null) {
            mMap.setOnCameraMoveStartedListener(moveStartedListener);
            mMap.setOnMapClickListener(onMapClickListener);
            mMap.setOnMarkerClickListener(onMarkerClickListener);
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            login.setText(R.string.btn_login);
            panic.setVisibility(View.GONE);
            linearLayoutShow = true;
        } else {
            checkIfAlreadyLoggedIn();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST1) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else {
                if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                        (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSION_REQUEST1);
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
        } else if (requestCode == PERMISSION_REQUEST2) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else {
                if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) &&
                        (grantResults[1] == PackageManager.PERMISSION_GRANTED) &&
                        (grantResults[2] == PackageManager.PERMISSION_GRANTED) &&
                        (grantResults[3] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (grantResults[4] != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSION_REQUEST2);
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
    public void onBackPressed() {
        super.onBackPressed();
        boolean isRatingGiven = mySharedPreferences.getBoolean("isRatingGiven", false);
        if (!isRatingGiven)
            askForRating();
    }

    private void askForRating() {
        Intent ratingIntent = new Intent(this, RatingActivity.class);
        startActivity(ratingIntent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        requestLocation();
        mMap.getUiSettings().setScrollGesturesEnabled(false);
//        try {
//            // Customise the styling of the base map using a JSON object defined
//            // in a raw resource file.
//            boolean success = googleMap.setMapStyle(
//                    MapStyleOptions.loadRawResourceStyle(
//                            this, R.raw.map_style));
//
//            if (!success) {
//                Log.e(TAG, "Style parsing failed.");
//            }
//        } catch (Resources.NotFoundException e) {
//            Log.e(TAG, "Can't find style. Error: ", e);
//        }
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mMap.getUiSettings().setScrollGesturesEnabled(true);
            }
        });

        mMap.setOnMapClickListener(onMapClickListener);
        mMap.setOnMarkerClickListener(onMarkerClickListener);

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                final LatLng latLng = marker.getPosition();
                new AlertDialog.Builder(LoginActivity.this)
                        .setIcon(R.drawable.ic_map_marker_point)
                        .setTitle("Get Direction")
                        .setMessage("Do you want to open the turn by turn navigation to " + marker.getTitle() + "?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latLng.latitude + "," + latLng.longitude);
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                mapIntent.setPackage("com.google.android.apps.maps");
                                startActivity(mapIntent);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    void requestLocation() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions = new String[]{ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION};

            } else {
                permissions = new String[]{ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};
            }
            ActivityCompat.requestPermissions(this,
                    permissions,
                    PERMISSION_REQUEST1);
        }
        client.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                mOpt = new MarkerOptions().title(self).position(latLng);
                mOpt.icon(BitmapDescriptorFactory.fromBitmap(myHelper.createCustomMarker(LoginActivity.this, R.layout.custom_marker_layout, myHelper.getBitmapFromPreferences(myHelper.sharedPreferences, self))));
                InfoWindowData info = myHelper.createCustomInfoWindow(mMap, myHelper.getAddressFromLocation(currentLocation), getApplicationContext());
                if (myMarker == null) {
                    myMarker = mMap.addMarker(mOpt);
                    myMarker.setTag(info);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                } else
                    myMarker.setPosition(latLng);
                placeMarkers.add(myMarker);
                mMap.setOnCameraMoveStartedListener(moveStartedListener);
//                if (autocompleteFragment != null) {
//                    autocompleteFragment.setCountry(Locale.getDefault().getCountry());
//                    try {
//                        List<Address> addressList;
//                        Geocoder geocoder = new Geocoder(LoginActivity.this, Locale.getDefault());
//                        addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
//                        LatLng localityPosition;
//                        String loc;
//                        if (addressList.size() > 0) {
//                            if(addressList.get(0).getPostalCode()!=null)
//                                loc = addressList.get(0).getPostalCode();
//                            else
//                                loc = addressList.get(0).getLocality();
//                            addressList = geocoder.getFromLocationName(loc,1);
//                        }
//                        localityPosition = new LatLng(addressList.get(0).getLatitude(),addressList.get(0).getLongitude());
//                        autocompleteFragment.setLocationRestriction(
//                                RectangularBounds.newInstance(
//                                        localityPosition, myMarker.getPosition()
//                                )
//                        );
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));
//
//                    autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//                        @Override
//                        public void onPlaceSelected(@NonNull Place place) {
//                            setPlaceMarker(place);
//                        }
//
//                        @Override
//                        public void onError(@NonNull Status status) {
//
//                        }
//                    });
//                }
            }
        }, null);
    }
}

package com.smiansh.famtrack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.facebook.login.LoginManager;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.smiansh.famtrack.LoginActivity.isTestUser;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MAPS_ACTIVITY";
    private static final int PERMISSION_REQUEST = 10;
    private GoogleMap mMap;
    private static final String ALPHA_NUMERIC_STRING = "0123456789" +
            "ABCDEFGHJKLMNOPQRSTUVWXYZ" + "abcdefghijkmnopqrstuvwxyz";
    public static String self;
    public static Marker selfMarker;
    PrefManager prefManager;
    private String userId;
    private static boolean hideMyLocation = false;
    public HashMap<String, Marker> mMarkers = new HashMap<>();
    private MarkerOptions mOpt = null;
    private boolean boundLatLong = false;
    SharedPreferences sharedPreferences;
    private Bitmap bmp = null;
    private Helper myHelper;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = mAuth.getCurrentUser();
    private BillingManager subscription;
    private HashMap<String, String> userStatuss = new HashMap<>();
    private boolean licencedProduct = false;
    private ExtendedFloatingActionButton recenter, hide, addPerson;
    boolean skipFlag = false;
    View.OnClickListener recenterClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!boundLatLong)
                boundLatLong = true;
            if (mMarkers.size() == 0) {
                mMarkers.put(self, selfMarker);
            }
            setLatLongBound();
        }
    };
    View.OnClickListener hideClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!hideMyLocation) {
                hide.setIcon(getDrawable(R.drawable.ic_visibility_off_24dp));
                selfMarker = mMarkers.remove(self);
                if (selfMarker != null) {
                    selfMarker.setVisible(false);
                }
                hideMyLocation = true;
            } else {
                hide.setIcon(getDrawable(R.drawable.ic_visibility_on_24dp));
                selfMarker.setVisible(true);
                mMarkers.put(self, selfMarker);
                hideMyLocation = false;
            }
            setLatLongBound();
        }
    };
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.renew:
                    subscription.launchBillingWorkflow();
                    finish();
                    break;
                case R.id.signout:
                    Log.i(TAG, "Signing out...");
                    db.collection("users").document(userId).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Map<String, Object> data = documentSnapshot.getData();
                                    if (data != null) {
                                        data.put("status", "Signed Out");
                                        db.collection("users").document(userId).set(data, SetOptions.merge())
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
                                                        Toast.makeText(MapsActivity.this, "Could not sign out the user", Toast.LENGTH_SHORT).show();
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
                    myHelper.stopTrackingService(MapsActivity.this);
                    break;
                case R.id.updateProfile:
                    Intent intentProfile = new Intent(MapsActivity.this, ProfileActivity.class);
                    intentProfile.putExtra("userId", userId);
                    startActivity(intentProfile);
                    break;
                case R.id.addMember:
                    startAddMemberActivity(userId);
                    break;
                case R.id.addPlaces:
                    Intent intentAddPlace = new Intent(MapsActivity.this, GeofenceActivity.class);
                    intentAddPlace.putExtra("userId", userId);
                    startActivity(intentAddPlace);
                    break;
                case R.id.help:
                    PrefManager pfMan = new PrefManager(MapsActivity.this);
                    pfMan.editor.remove(PrefManager.IS_FIRST_TIME_LAUNCH).apply();
                    pfMan.editor.putBoolean("fromMapView", true).apply();
                    Intent faqIntent = new Intent(MapsActivity.this, WelcomeActivity.class);
                    faqIntent.putExtra("userId", userId);
                    startActivity(faqIntent);
                    break;
                case R.id.shareLocation:
                    final DocumentReference docRef = db.collection("users").document(userId);
                    docRef.get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    String oldAuthCode = null;
                                    SimpleDateFormat format = new SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault());
                                    Map<String, Object> data = documentSnapshot.getData();
                                    if (data == null)
                                        data = new HashMap<>();
                                    String authCode = documentSnapshot.getString("authCode");
                                    String timestamp = documentSnapshot.getString("authCodeTimestamp");
                                    if (timestamp == null || authCode == null) {
                                        authCode = randomAlphaNumeric(6);
                                        timestamp = "00000101000000";
                                    } else {
                                        oldAuthCode = authCode;
                                    }
                                    Date D1 = null;
                                    try {
                                        D1 = format.parse(timestamp);
                                    } catch (ParseException e1) {
                                        e1.printStackTrace();
                                    }
                                    Date D2 = new Date();
                                    assert D1 != null;
                                    long diff = D2.getTime() - D1.getTime();

                                    long limit = 7 * 24 * 60 * 60 * 1000; //7 days

                                    if (diff > limit) {
                                        authCode = randomAlphaNumeric(6);
                                        data.put("authCode", authCode);
                                        data.put("authCodeTimestamp", format.format(new Date()));
                                        docRef.update(data);
                                        data.clear();
                                        data.put("userId", userId);
                                        db.collection("authcodes").document(authCode).set(data, SetOptions.merge());
                                        if (oldAuthCode != null) {
                                            db.collection("authcodes").document(oldAuthCode).delete();
                                        }
                                    }
                                    Intent intent = new Intent(MapsActivity.this, ShareCodeActivity.class);
                                    intent.putExtra("authCode", authCode);
                                    startActivity(intent);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MapsActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;
            }
            mDrawerLayout.closeDrawers();
            return true;
        }
    };
    private View.OnClickListener addPersonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startAddMemberActivity(userId);
        }
    };
    private CircleImageView drawerCircleImage;
    private MaterialTextView drawerTextBox;

    public MapsActivity() {
        initialize();
    }

    private void startAddMemberActivity(String userId) {
        Intent intentAddMember = new Intent(MapsActivity.this, AddMemberActivity.class);
        intentAddMember.putExtra("userId", userId);
        startActivity(intentAddMember);
    }

    public void setLatLongBound() {
        if (mMarkers == null)
            return;
        if (mMarkers.size() > 0) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Map.Entry entry : mMarkers.entrySet()) {
                Marker marker = (Marker) entry.getValue();
                if (marker != null) {
                    builder.include(marker.getPosition());
                    Location location = new Location(LocationManager.GPS_PROVIDER);
                    location.setLongitude(marker.getPosition().longitude);
                    location.setLatitude(marker.getPosition().latitude);
//                    InfoWindowData info = createCustomInfoWindow(getGeoAddress(new GeoPoint(marker.getPosition().latitude, marker.getPosition().longitude), userStatuss.get(entry.getKey().toString())));
                    InfoWindowData info = myHelper.createCustomInfoWindow(
                            mMap,
                            prepateGeoAddress(myHelper.getAddressFromLocation(location), userStatuss.get(entry.getKey().toString())),
                            getApplicationContext()
                    );
                    marker.setTag(info);
                    marker.setVisible(true);
                    marker.hideInfoWindow();
                }
            }
            if (mMarkers.size() > 1)
                hide.setVisibility(View.VISIBLE);
            mMap.resetMinMaxZoomPreference();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        }
        recenter.setVisibility(View.GONE);
        boundLatLong = false;
    }

    protected void initialize() {
        try {
            userId = user.getUid();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        initialize();
        if (licencedProduct || isTestUser) {
            if (hideMyLocation)
                hide.setIcon(getDrawable(R.drawable.ic_visibility_on_24dp));
            locateFamily();
            subscribeToLocations();
            if (mMarkers.size() > 1)
                hide.setVisibility(View.VISIBLE);
        } else {
            selfSubscribe();
            hide.setVisibility(View.GONE);
        }
        recenter.setOnClickListener(recenterClickListener);
        hide.setOnClickListener(hideClickListener);
        addPerson.setOnClickListener(addPersonClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideMyLocation = false;
        hide.setIcon(getDrawable(R.drawable.ic_visibility_on_24dp));
        subscription.refreshPurchases();
        navigationView.setNavigationItemSelectedListener(navigationItemSelectedListener);
        MenuItem selectedItem = navigationView.getCheckedItem();
        if (selectedItem != null) {
            selectedItem.setChecked(false);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
//        if(id == R.id.action_settings){
//            return true;
//        } else
        if (id == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        return super.onOptionsItemSelected(item);
    }

    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        prefManager = new PrefManager(getApplicationContext());
        sharedPreferences = prefManager.getApplicationDefaultSharedPreferences();
        subscription = new BillingManager(this).build();
        licencedProduct = sharedPreferences.getBoolean("isLicenced", false);
        myHelper = new Helper(this);
        myHelper.setCurrUser(user);
        myHelper.createAlarm(60000);
        recenter = findViewById(R.id.recenter);
        recenter.setOnClickListener(recenterClickListener);
        recenter.setVisibility(View.GONE);
        hide = findViewById(R.id.hide);
        hide.setOnClickListener(hideClickListener);
        addPerson = findViewById(R.id.addPerson);
        addPerson.setOnClickListener(addPersonClickListener);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        navigationView = findViewById(R.id.nav_view);
        mDrawerLayout = findViewById(R.id.drawer);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            VectorDrawableCompat indicator =
                    VectorDrawableCompat.create(getResources(), R.drawable.ic_menu_hamburger, getTheme());
            supportActionBar.setHomeAsUpIndicator(indicator);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        try {
            navigationView.setNavigationItemSelectedListener(navigationItemSelectedListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!licencedProduct || isTestUser) {
            try {
                MobileAds.initialize(this, getString(R.string.ads));
                AdView mAdView = findViewById(R.id.adView);
                mAdView.setVisibility(View.VISIBLE);
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        try {
            assert mapFragment != null;
            mapFragment.getMapAsync(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initialize();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
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

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                boundLatLong = false;
                return false;
            }
        });

        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                recenter.setVisibility(View.VISIBLE);
                boundLatLong = false;
            }
        });
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                final LatLng latLng = marker.getPosition();
                new AlertDialog.Builder(MapsActivity.this)
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

        if (licencedProduct || isTestUser) {
            subscribeToLocations();
        } else {
            selfSubscribe();
        }
        locateFamily();
    }

    private void selfSubscribe() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot != null) {
                            self = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            setMarker(documentSnapshot);
                            setLatLongBound();
                            drawerCircleImage = findViewById(R.id.drawerCircleImage);
                            drawerTextBox = findViewById(R.id.drawerTextBox);
                            drawerCircleImage.setImageBitmap(myHelper.getBitmapFromPreferences(sharedPreferences, self));
                            drawerTextBox.setText(self.concat(" " + lastName));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
        db.collection("users").document(userId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (documentSnapshot != null) {
                            self = documentSnapshot.getString("firstName");
                            String status = documentSnapshot.getString("status");
                            if (status != null)
                                userStatuss.put(self, status);
                            else userStatuss.put(self, "Unknown");
                            if (!hideMyLocation)
                                setMarker(documentSnapshot);
                            setLatLongBound();
                        }
                    }
                });
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

    private void subscribeToLocations() {
        try {
            DocumentReference docRef = db.collection("users").document(userId);
            docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    if (documentSnapshot != null) {
                        self = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String status = documentSnapshot.getString("status");
                        if (status != null)
                            userStatuss.put(self, status);
                        else userStatuss.put(self, "Unknown");
                        if (!hideMyLocation)
                            setMarker(documentSnapshot);
                        drawerCircleImage = findViewById(R.id.drawerCircleImage);
                        drawerTextBox = findViewById(R.id.drawerTextBox);
                        drawerCircleImage.setImageBitmap(myHelper.getBitmapFromPreferences(sharedPreferences, self));
                        drawerTextBox.setText(self.concat(" " + lastName));
                    }
                }
            });
            docRef.get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            //noinspection unchecked
                            Map<String, Object> family = (Map<String, Object>) documentSnapshot.get("family");
                            if (family != null) {
                                for (Map.Entry<String, Object> entry : family.entrySet()) {
                                    DocumentReference memberRef = db.document(entry.getValue().toString());
                                    memberRef.get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    setMarker(documentSnapshot);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                    memberRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                                            if (documentSnapshot != null) {
                                                setMarker(documentSnapshot);
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    String prepateGeoAddress(String address, String userStatus) {
        String addr;
        if (userStatus != null)
            addr = address.concat("\n\nLogged In Status: " + userStatus);
        else addr = address.concat("\n\nLogged In Status: Unknown");

        return addr;
    }

    private void setMarker(DocumentSnapshot dataSnapshot) {
        String title = dataSnapshot.getString("firstName");
        GeoPoint geoPoint = dataSnapshot.getGeoPoint("location");
        String uid = dataSnapshot.getId();
        String status = dataSnapshot.getString("status");
        userStatuss.put(title, status);
        LatLng latLng = null;
        if (geoPoint != null) {
            latLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
        }
        if (latLng != null) {
            if (!mMarkers.containsKey(title)) {
                mOpt = new MarkerOptions().title(title).position(latLng);
//                downloadProfileImage(uid, title);
                try {
                    String path = sharedPreferences.getString(title + "_profileImage", null);
                    try {
                        FileInputStream is;
                        if (path != null) {
                            is = new FileInputStream(new File(path));
                            bmp = BitmapFactory.decodeStream(is);
                            is.close();
                        } else {
                            Log.i(TAG, "Downloading Image - 01" + uid + "/" + title);
                            downloadProfileImage(uid, title, status);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "Downloading Image - 02");
                        downloadProfileImage(uid, title, status);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mOpt.icon(BitmapDescriptorFactory.fromBitmap(myHelper.createCustomMarker(MapsActivity.this, R.layout.custom_marker_layout, bmp)));
//Required to reset the bmp null to avoid loading login user picture to members' markers.
                bmp = null;
                Marker newMarker = mMap.addMarker(mOpt);
                Location location = new Location(LocationManager.GPS_PROVIDER);
                location.setLatitude(newMarker.getPosition().latitude);
                location.setLongitude(newMarker.getPosition().longitude);
                InfoWindowData info = myHelper.createCustomInfoWindow(
                        mMap,
                        prepateGeoAddress(myHelper.getAddressFromLocation(location), status),
                        getApplicationContext()
                );
                newMarker.setTag(info);
                mMarkers.put(title, newMarker);
                if (self.equals(title)) {
                    selfMarker = newMarker;
                }
            } else {
                Marker marker = mMarkers.get(title);
                if (marker != null) {
                    marker.setPosition(latLng);
                    marker.setVisible(true);
                }
            }

            setLatLongBound();
        }
    }

    private void downloadProfileImage(String uid, final String title, final String userStatus) {
        StorageReference profilePicRef = FirebaseStorage.getInstance().getReference();
        StorageReference profilePicPath = profilePicRef.child("images/" + uid);
        profilePicPath.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        try {
                            MarkerParams mParam = new MarkerParams(title, userStatus, new URL(uri.toString()));
                            DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
                            downloadFilesTask.execute(mParam);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
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

    private void locateFamily() {
        try {
            mMap.clear();
            mMarkers.clear();
            myHelper.getUserData().get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            try {
                                self = documentSnapshot.getString("firstName");
                                final String temp = documentSnapshot.getString("allowedMembers");
                                //noinspection unchecked
                                final Map<String, Object> family = (Map<String, Object>) documentSnapshot.get("family");
                                if (family != null) {
                                    TreeMap<String, Object> sortedFamily = new TreeMap<>(family);
                                    for (Map.Entry row : sortedFamily.entrySet()) {
                                        DocumentReference localDocRef = db.document(row.getValue().toString());
                                        localDocRef.get()
                                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                        int allowedMembers = Helper.ALLOWED_MEMBERS;
                                                        if (temp != null) {
                                                            if (temp.length() > Helper.ALLOWED_MEMBERS) {
                                                                allowedMembers = Integer.parseInt(temp);
                                                            }
                                                        }
                                                        int familySize = family.size();
                                                        if (!licencedProduct)
                                                            familySize = Helper.ALLOWED_MEMBERS;
                                                        if (familySize <= allowedMembers && !skipFlag) {
                                                            setMarker(documentSnapshot);
                                                            if (!licencedProduct)
                                                                skipFlag = true;
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
                                }
                                setMarker(documentSnapshot);
                                if (mMarkers.size() > 1)
                                    hide.setVisibility(View.VISIBLE);
                                else hide.setVisibility(View.GONE);
                                boundLatLong = true;
                                setLatLongBound();
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    class MarkerParams {
        String title;
        String userStatus;
        URL url;

        MarkerParams(String m, String status, URL u) {
            title = m;
            userStatus = status;
            url = u;
        }
    }

    @SuppressLint("StaticFieldLeak")
    class DownloadFilesTask extends AsyncTask<MarkerParams, Void, Void> {

        @Override
        protected Void doInBackground(MarkerParams... markerParams) {
            final String title = markerParams[0].title;
            final String status = markerParams[0].userStatus;

            InputStream is = null;
            try {
                HttpsURLConnection connection = (HttpsURLConnection) new URL(markerParams[0].url.toString()).openConnection();
                Log.i(TAG, markerParams[0].url.toString());
                connection.connect();
                is = connection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            final Bitmap bitmap = BitmapFactory.decodeStream(is);
            final Marker m = mMarkers.get(title);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), title);
            if (file.exists())
                if (!file.delete()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MapsActivity.this, "File could not be deleted, skipping", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            try {
                OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(file);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
                sharedPreferences.edit().putString(title + "_profileImage", file.getAbsolutePath()).apply();
                os.flush();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (m != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mOpt = new MarkerOptions().title(title).position(m.getPosition());
                        Location location = new Location(LocationManager.GPS_PROVIDER);
                        location.setLongitude(m.getPosition().longitude);
                        location.setLatitude(m.getPosition().latitude);
                        InfoWindowData info = myHelper.createCustomInfoWindow(
                                mMap,
                                prepateGeoAddress(myHelper.getAddressFromLocation(location), status),
                                getApplicationContext()
                        );
                        mOpt.icon(BitmapDescriptorFactory.fromBitmap(myHelper.createCustomMarker(MapsActivity.this, R.layout.custom_marker_layout, bitmap)));
//Required to reset the bmp null to avoid loading login user picture to members' markers.
                        bmp = null;
                        Marker newMarker = mMap.addMarker(mOpt);
                        newMarker.setTag(info);
                        mMarkers.put(title, newMarker);
                        m.remove();
                    }
                });
            }
            return null;
        }
    }
}

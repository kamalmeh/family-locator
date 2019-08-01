package com.smiansh.familylocator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;

import de.hdodenhof.circleimageview.CircleImageView;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MAPS_ACTIVITY";
    private static final int PERMISSION_REQUEST = 10;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private static final String ALPHA_NUMERIC_STRING = "0123456789" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz";
    private String userId;
    private FirebaseFirestore db;
    private MarkerOptions mOpt = null;
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private Bitmap bmp = null;
    private SharedPreferences sp = null;
    private AdView mAdView;
    private Helper myHelper;

    public static Bitmap createCustomMarker(Context context, Bitmap bmp) {

        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);

        CircleImageView markerImage = marker.findViewById(R.id.user_dp);
        if (bmp == null)
            markerImage.setImageResource(R.drawable.ic_boy);
        else
            markerImage.setImageBitmap(bmp);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        marker.draw(canvas);

        return bitmap;
    }

    public MapsActivity() {
        initialize();
    }

    protected void initialize() {
        try {
            userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            db = FirebaseFirestore.getInstance();
            sp = PreferenceManager.getDefaultSharedPreferences(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialize();
        locateFamily();
        subscribeToLocations();
//        requestLocationUpdates(null);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signout:
                FirebaseAuth.getInstance().signOut();
                myHelper.destroyAlarm();
                myHelper.stopTrackingService(this);
                finish();
                break;
            case R.id.updateProfile:
                Intent intentProfile = new Intent(this, ProfileActivity.class);
                intentProfile.putExtra("userId", userId);
                startActivity(intentProfile);
                finish();
                break;
            case R.id.addMember:
                Intent intentAddMember = new Intent(this, AddMemberActivity.class);
                intentAddMember.putExtra("userId", userId);
                startActivity(intentAddMember);
                finish();
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
                                    assert data != null;
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
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
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
        myHelper = new Helper(getApplicationContext());
        myHelper.createAlarm(60000);
        setContentView(R.layout.activity_maps);
        MobileAds.initialize(this, getString(R.string.ads));
        mAdView = findViewById(R.id.adView);
        mAdView.setAdSize(AdSize.SMART_BANNER);
        mAdView.setAdUnitId(getString(R.string.ads));
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            assert mapFragment != null;
            mapFragment.getMapAsync(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        prepareLocationRequest();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        requestLocationUpdates(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        requestLocationUpdates(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initialize();
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
//        }
//        requestLocationUpdates(null);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
//                Toast.makeText(MapsActivity.this, marker.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        mMap.setOnInfoWindowLongClickListener(new GoogleMap.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(Marker marker) {

            }
        });

        locateFamily();
        subscribeToLocations();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else {
                if ((grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted.
                    Log.i(TAG, "Permission was granted");
//                    requestLocationUpdates(null);
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

    public void prepareLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);              //1 Second
        locationRequest.setFastestInterval(1000);       //1 Second
        locationRequest.setSmallestDisplacement(10);    //10 Meters
        locationRequest.setMaxWaitTime(10000);          //10 Seconds
        locationRequest.setNumUpdates(120);             //120 updates in a minute
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdates.class);
        intent.setAction(LocationUpdates.PROCESS_LOCATION_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void requestLocationUpdates(View view) {
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
        } catch (SecurityException e) {
            Toast.makeText(this, "Security Exception", Toast.LENGTH_SHORT).show();
        }
    }

    private void subscribeToLocations() {
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot != null) {
                    setMarker(documentSnapshot);
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
    }

    private void setMarker(DocumentSnapshot dataSnapshot) {
        final String title = dataSnapshot.getString("firstName");
        GeoPoint geoPoint = dataSnapshot.getGeoPoint("location");
        String uid = dataSnapshot.getId();
        LatLng location = null;
        if (geoPoint != null) {
            location = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
        }
        if (location != null) {
            if (!mMarkers.containsKey(title)) {
                mOpt = new MarkerOptions().title(title).position(location);
                downloadProfileImage(uid, title);
                mOpt.icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(MapsActivity.this, bmp)));
                mMarkers.put(title, mMap.addMarker(mOpt));
            } else {
                Objects.requireNonNull(mMarkers.get(title)).setPosition(location);
                downloadProfileImage(uid, title);
            }
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : mMarkers.values()) {
                builder.include(marker.getPosition());
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        }
    }

    private void downloadProfileImage(String uid, final String title) {
        StorageReference profilePicRef = FirebaseStorage.getInstance().getReference();
        StorageReference profilePicPath = profilePicRef.child("images/" + uid);
        profilePicPath.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        try {
                            MarkerParams mParam = new MarkerParams(title, new URL(uri.toString()));
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
        myHelper.getUserData().get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        try {
                            //noinspection unchecked
                            Map<String, Object> family = (Map<String, Object>) documentSnapshot.get("family");
                            if (family != null) {
                                for (Map.Entry row : family.entrySet()) {
                                    DocumentReference localDocRef = db.document(row.getValue().toString());
                                    localDocRef.get()
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
                                }
                            }
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
    }

    class MarkerParams {
        String title;
        URL url;

        MarkerParams(String m, URL u) {
            title = m;
            url = u;
        }
    }

    @SuppressLint("StaticFieldLeak")
    class DownloadFilesTask extends AsyncTask<MarkerParams, Void, Void> {

        @Override
        protected Void doInBackground(MarkerParams... markerParams) {
            final String title = markerParams[0].title;

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
            if (m != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mOpt = new MarkerOptions().title(title).position(m.getPosition());
                        mOpt.icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(MapsActivity.this, bitmap)));
                        mMarkers.put(title, mMap.addMarker(mOpt));
                        m.remove();
                    }
                });
            }
            return null;
        }
    }
}

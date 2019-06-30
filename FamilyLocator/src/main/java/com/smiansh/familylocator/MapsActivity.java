package com.smiansh.familylocator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int PERMISSION_REQUEST = 10;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private static final String ALPHA_NUMERIC_STRING = "0123456789" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz";
    private LocationManager locationManager;
    private String userId;
    final static long REFRESH = 10 * 1000;
    private FirebaseFirestore db;
    private Map<String, Marker> markers;
    private float zoomLevel;
    private Location myLocation;
    private LocationHandler locationHandler;
    private LocationCallback locationCallback;
    private LocationListener locationListener;

    public MapsActivity() {
        initialize();
    }

    protected void initialize() {
        zoomLevel = 15;
        userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialize();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Toast.makeText(this, "Selected Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {
            case R.id.updateProfile:
                Intent intentProfile = new Intent(this, ProfileActivity.class);
                intentProfile.putExtra("userId", userId);
                startActivity(intentProfile);
                break;
            case R.id.addMember:
                Intent intentAddMember = new Intent(this, AddMemberActivity.class);
                intentAddMember.putExtra("userId", userId);
                startActivity(intentAddMember);
                break;
            case R.id.shareLocation:
                final DocumentReference docRef = db.collection("users").document(userId);
                docRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                SimpleDateFormat format = new SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault());
                                Map<String, Object> data = documentSnapshot.getData();
                                String authCode = documentSnapshot.getString("authCode");
                                String timestamp = documentSnapshot.getString("authCodeTimestamp");
                                if (timestamp == null && authCode == null) {
                                    authCode = randomAlphaNumeric(6);
                                    timestamp = "00000101000000";
                                }
                                Date D1 = null;
                                try {
                                    assert timestamp != null;
                                    D1 = format.parse(timestamp);
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
                                Date D2 = new Date();
                                assert D1 != null;
                                long diff = D2.getTime() - D1.getTime();
                                long limit = 7 * 24 * 60 * 60; //7 days

                                if (diff > limit) {
                                    authCode = randomAlphaNumeric(6);
                                    assert data != null;
                                    data.put("authCode", authCode);
                                    data.put("authCodeTimestamp", format.format(new Date()));
                                    docRef.update(data);
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
                Intent intentShareLocation = new Intent(this, ShareCodeActivity.class);
                intentShareLocation.putExtra("userId", userId);
                startActivity(intentShareLocation);
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
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
        }
        requestLocationUpdates(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        try {
            assert mapFragment != null;
            mapFragment.getMapAsync(this);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        prepareLocationRequest();

        locationHandler = new LocationHandler();
        locationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                locateFamily();
                locateSelf();
            }
        }, 10000);
        markers = new HashMap<>();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                super.onLocationResult(locationResult);
                myLocation = locationResult.getLastLocation();
                db.collection("users").document(userId).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                String title = documentSnapshot.getString("firstName");
                                assert title != null;
                                setMarker(title, myLocation.getLatitude(), myLocation.getLongitude(), true);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
            }
        };

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                myLocation = location;
                db.collection("users").document(userId).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                String title = documentSnapshot.getString("firstName");
                                assert title != null;
                                setMarker(title, myLocation.getLatitude(), myLocation.getLongitude(), true);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST);
        }

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                locateSelf();
                locateFamily();
            }
        });

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                zoomLevel = mMap.getCameraPosition().zoom;
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(MapsActivity.this, marker.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Toast.makeText(this, "Permission is required for the core functionality of the application", Toast.LENGTH_LONG).show();

            } else if ((grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                   && (grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                // Permission was granted.
                requestLocationUpdates(null);

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

    public void setMarker(@NonNull String title, double latitude, double longitude, boolean center) {
        LatLng markerPlace = new LatLng(latitude, longitude);
        if (markers.containsKey(title)) {
            markers.get(title).setPosition(markerPlace);
        } else {
            markers.put(title, mMap.addMarker(new MarkerOptions().position(markerPlace).title(title)));
        }
        if (center) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPlace, zoomLevel));
        }
    }

    public void prepareLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(1000);             //1 Seconds
        locationRequest.setFastestInterval(1000);       //1 Second
        locationRequest.setMaxWaitTime(300 * 1000);       //300 Seconds = 5 Minutes
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdates.class);
        intent.setAction(LocationUpdates.PROCESS_LOCATION_UPDATES);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @SuppressLint("MissingPermission")
    public void requestLocationUpdates(View view) {
        try {
            locateSelf();
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
        } catch (SecurityException e) {
            Toast.makeText(this, "Security Exception", Toast.LENGTH_SHORT).show();
        }
    }

    public void locateSelf() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } else
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, Looper.myLooper());
    }

    public void locateFamily() {
        final DocumentReference documentReference = db.collection("users")
                .document(userId);
        documentReference.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        try {
                            @SuppressWarnings("unchecked") Map<String, Object> family = (Map<String, Object>) documentSnapshot.get("family");
                            for (Map.Entry<String, Object> entry : Objects.requireNonNull(family).entrySet()) {
                                DocumentReference memberRef = (DocumentReference) entry.getValue();
                                final GeoPoint[] geoPoint = new GeoPoint[1];
                                memberRef.get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                String firstName = documentSnapshot.getString("firstName");
                                                geoPoint[0] = documentSnapshot.getGeoPoint("location");
                                                assert firstName != null;
                                                assert geoPoint[0] != null;
                                                setMarker(firstName, geoPoint[0].getLatitude(), geoPoint[0].getLongitude(), false);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("locateFamily()", e.toString());
                                            }
                                        });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
        locationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                locateFamily();
                locateSelf();
            }
        }, 1000);
    }

    @SuppressLint("HandlerLeak")
    private class LocationHandler extends Handler {
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                locateFamily();
                this.sendEmptyMessageDelayed(0, REFRESH);
            }
        }
    }
}

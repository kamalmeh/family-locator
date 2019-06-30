package com.smiansh.familylocator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST = 10;
    private GoogleMap mMap;
    private CircleOptions circleOptions;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private static final String ALPHA_NUMERIC_STRING = "0123456789" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz";
    private LocationManager locationManager;
    private Button addMember, shareButton;
    private String userId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Toast.makeText(this, "Selected Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {
            case R.id.signout:
//                PackageManager pm = this.getPackageManager();
//                ComponentName componentName = new ComponentName(this, LocationUpdates.class);
//                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//                        PackageManager.DONT_KILL_APP);
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return super.onOptionsItemSelected(item);
            case R.id.updateProfile:
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        addMember = findViewById(R.id.addMember);
        userId = this.getIntent().getStringExtra("userId");
        addMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapsActivity.this, AddMemberActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        shareButton = findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final DocumentReference docRef = db.collection("users").document(userId);
                docRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {
                                    String authCode = randomAlphaNumeric(6);
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("userId", userId);
                                    db.collection("authcodes").document(authCode).set(data);
                                    data.clear();
                                    data.put("authCode", authCode);
                                    docRef.set(data, SetOptions.merge());
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MapsActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        prepareLocationRequest();
        locateFamily();
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        circleOptions = new CircleOptions()
                .radius(100)
                .strokeColor(Color.argb(30, 0, 16, 255))
                .strokeWidth(1)
                .fillColor(Color.argb(15, 0, 16, 255));

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
                }
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    //setMarker("OnMapReady", location.getLatitude(), location.getLongitude());
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MapsActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    public void prepareLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(1000);             //1 Seconds
        locationRequest.setFastestInterval(1000);       //1 Second
        locationRequest.setMaxWaitTime(300 * 1000);       //300 Seconds = 5 Minutes
    }

    public void setMarker(String title, double latitude, double longitude) {
        //mMap.clear();
        LatLng markerPlace = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(markerPlace).title(title));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPlace, 17));
        mMap.addCircle(circleOptions.center(markerPlace));
    }

    private PendingIntent getPendingIntent() {
//        PackageManager pm = this.getPackageManager();
//        ComponentName componentName = new ComponentName(this, LocationUpdates.class);
//        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//                PackageManager.DONT_KILL_APP);
        Intent intent = new Intent(this, LocationUpdates.class);
        intent.setAction(LocationUpdates.PROCESS_LOCATION_UPDATES);
        //intent.putExtra("userId", userId);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @SuppressLint("MissingPermission")
    public void requestLocationUpdates(View view) {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //UserNote.sendNotification(MapsActivity.this, "In Location Listener Callback");
                    //("On requestLocationUpdates", location.getLatitude(), location.getLongitude());
//                Toast.makeText(MapsActivity.this, location.toString(), Toast.LENGTH_SHORT).show();
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference documentReference = db.collection("users").document(userId);
                    Map<String, Object> data = new HashMap<>();
                    data.put("location", geoPoint);
                    documentReference.set(data, SetOptions.merge());
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
            });
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
        } catch (SecurityException e) {
            Toast.makeText(this, "Security Exception", Toast.LENGTH_SHORT).show();
        }
    }

    public void locateFamily() {
        final DocumentReference documentReference = db.collection("users")
                .document(userId);
        documentReference.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        try {
                            HashMap<String, Object> family = (HashMap<String, Object>) documentSnapshot.get("family");
                            for (Map.Entry<String, Object> entry : family.entrySet()) {
                                DocumentReference memberRef = (DocumentReference) entry.getValue();
                                final GeoPoint[] geoPoint = new GeoPoint[1];
                                memberRef.get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                String firstName = documentSnapshot.getString("firstName");
                                                geoPoint[0] = documentSnapshot.getGeoPoint("location");
                                                setMarker(firstName, geoPoint[0].getLatitude(), geoPoint[0].getLongitude());
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.i("locateFamily()", e.getMessage());
                                            }
                                        });
                                //Log.i("LOCATE_FAMILY:"+entry.getKey(), entry.getValue().toString());
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
    }
}

package com.smiansh.familylocator;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
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

//public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SharedPreferences.OnSharedPreferenceChangeListener {
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private static final int PERMISSION_REQUEST = 10;
    private GoogleMap mMap;
    private CircleOptions circleOptions;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        prepareLocationRequest();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        PreferenceManager.getDefaultSharedPreferences(this)
//                .registerOnSharedPreferenceChangeListener(this);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
        }
        requestLocationUpdates(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        UserNote.getRequestingLocationUpdates(this);
//        UserNote.getLocationUpdatesResult(this);
    }


    @Override
    protected void onStop() {
//        PreferenceManager.getDefaultSharedPreferences(this)
//                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
//                Log.i(TAG, "User interaction was cancelled.");

            } else if ((grantResults[0] == PackageManager.PERMISSION_GRANTED)
//                   && (grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                // Permission was granted.
                requestLocationUpdates(null);

            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
//                Snackbar.make(
//                        findViewById(R.id.activity_detail),
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
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
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
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
                }
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    setMarker(location.getLatitude(), location.getLongitude());
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

//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
//        if (s.equals(UserNote.KEY_LOCATION_UPDATES_RESULT)) {
//            String result = UserNote.getLocationUpdatesResult(this);
//            String [] values = result.split(",");
//            if(values.length==3) {
//                setMarker(Double.parseDouble(values[1]), Double.parseDouble(values[2]));
//            }
//            requestLocationUpdates(null);
//        }else if (s.equals(UserNote.KEY_LOCATION_UPDATES_REQUESTED)) {
//            UserNote.getRequestingLocationUpdates(this);
//        }
//    }

    public void prepareLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(1000);             //10 Seconds
        locationRequest.setFastestInterval(1000);       //1 Second
        locationRequest.setMaxWaitTime(300 * 1000);       //300 Seconds = 5 Minutes
    }

    public void setMarker(double latitude, double longitude) {
        mMap.clear();
        LatLng markerPlace = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(markerPlace).title("My Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPlace, 17));
        mMap.addCircle(circleOptions.center(markerPlace));
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdates.class);
        intent.setAction(LocationUpdates.PROCESS_LOCATION_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void requestLocationUpdates(View view) {
        try {
//            Log.i(TAG, "Starting location updates");
//            UserNote.setRequestingLocationUpdates(this, true);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
        } catch (SecurityException e) {
//            UserNote.setRequestingLocationUpdates(this, false);
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        UserNote.sendNotification(this, "In Location Listener Callback");
        setMarker(location.getLatitude(), location.getLongitude());
        Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
    }
}

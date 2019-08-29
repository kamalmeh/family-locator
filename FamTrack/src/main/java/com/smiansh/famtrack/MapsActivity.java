package com.smiansh.famtrack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.util.List;
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
    private static final String ALPHA_NUMERIC_STRING = "0123456789" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz";
    private String userId;
    private FirebaseFirestore db;
    private MarkerOptions mOpt = null;
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private Bitmap bmp = null;
    private Helper myHelper;
    private static boolean boundLatLong = true;
    View.OnClickListener recenterClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boundLatLong = true;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : mMarkers.values()) {
                builder.include(marker.getPosition());
                marker.hideInfoWindow();
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            recenter.setVisibility(View.GONE);
        }
    };
    private Button recenter;
    private SharedPreferences sp;

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
        recenter.setOnClickListener(recenterClickListener);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signout:
                FirebaseAuth.getInstance().signOut();
                LoginManager.getInstance().logOut();
                AuthUI.getInstance().signOut(getApplicationContext());
                myHelper.destroyAlarm();
                myHelper.stopTrackingService(this);
                finish();
                break;
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
            case R.id.addPlaces:
                Intent intentAddPlace = new Intent(this, GeofenceActivity.class);
                intentAddPlace.putExtra("userId", userId);
                startActivity(intentAddPlace);
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
        recenter = findViewById(R.id.recenter);
        recenter.setOnClickListener(recenterClickListener);
        recenter.setVisibility(View.GONE);
        if (myHelper.isAdsEnabled()) {
            try {
                MobileAds.initialize(this, getString(R.string.ads));
                AdView mAdView = findViewById(R.id.adView);
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS},
                    PERMISSION_REQUEST);
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
//                Toast.makeText(MapsActivity.this, marker.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
                boundLatLong = false;
                return false;
            }
        });
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                boundLatLong = true;
            }
        });
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                recenter.setVisibility(View.VISIBLE);
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
                if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
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

    private void subscribeToLocations() {
        try {
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
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void setMarker(DocumentSnapshot dataSnapshot) {
        String title = dataSnapshot.getString("firstName");
        String address = "Address";
        List<Address> addressList;
        GeoPoint geoPoint = dataSnapshot.getGeoPoint("location");
        String uid = dataSnapshot.getId();
        LatLng location = null;
        if (geoPoint != null) {
            location = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
        }
        if (location != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1);
                if (addressList.size() > 0) {
                    address = addressList.get(0).getAddressLine(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!mMarkers.containsKey(title)) {
                InfoWindowData info = createCustomInfoWindow(address);
                mOpt = new MarkerOptions().title(title).position(location);
                downloadProfileImage(uid, title);
                mOpt.icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(MapsActivity.this, bmp)));
                Marker newMarker = mMap.addMarker(mOpt);
                newMarker.setTag(info);
                mMarkers.put(title, newMarker);
            } else {
                Objects.requireNonNull(mMarkers.get(title)).setPosition(location);
                //downloadProfileImage(uid, title);
            }
            if (boundLatLong) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : mMarkers.values()) {
                    builder.include(marker.getPosition());
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                recenter.setVisibility(View.GONE);
            } else {
                Objects.requireNonNull(mMarkers.get(title)).setPosition(location);
                recenter.setVisibility(View.VISIBLE);
            }
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
        try {
            mMap.clear();
            mMarkers.clear();
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
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    class MarkerParams {
        String title;
        URL url;

        MarkerParams(String m, URL u) {
            title = m;
            url = u;
        }
    }

    InfoWindowData createCustomInfoWindow(String address) {
        InfoWindowData info = new InfoWindowData();
        info.setAddress(address + "\nClick Me to get Direction");
        CustomInfoWindowGoogleMap customInfoWindow = new CustomInfoWindowGoogleMap(this);
        mMap.setInfoWindowAdapter(customInfoWindow);
        return info;
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
                        String address = "Address";
                        List<Address> addressList;
                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                        try {
                            addressList = geocoder.getFromLocation(m.getPosition().latitude, m.getPosition().longitude, 1);
                            if (addressList.size() > 0) {
                                address = addressList.get(0).getAddressLine(0);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        InfoWindowData info = createCustomInfoWindow(address);
                        mOpt.icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(MapsActivity.this, bitmap)));
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

    class CustomInfoWindowGoogleMap implements GoogleMap.InfoWindowAdapter {

        private Context context;

        CustomInfoWindowGoogleMap(Context ctx) {
            context = ctx;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            View view = ((Activity) context).getLayoutInflater()
                    .inflate(R.layout.map_custom_infowindow, null);

            TextView name_tv = view.findViewById(R.id.name);
            TextView address = view.findViewById(R.id.address);

            name_tv.setText(marker.getTitle());

            InfoWindowData infoWindowData = (InfoWindowData) marker.getTag();

            if (infoWindowData != null) {
                address.setText(infoWindowData.getAddress());
            }

            return view;
        }
    }
}

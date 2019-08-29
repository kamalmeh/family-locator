package com.smiansh.famtrack;

import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeofenceActivity extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId = FirebaseAuth.getInstance().getUid();
    private String myName = null;
    private ListView places;
    private Button addPlace;
    private String newPlace;
    private Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence);
        places = findViewById(R.id.places);
        addPlace = findViewById(R.id.addPlaces);
        db.document("/users/" + userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        myName = documentSnapshot.getString("firstName");
                        myName = myName.concat(" " + documentSnapshot.getString("lastName"));
                    }
                });
        addPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(GeofenceActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                final View popupView = inflater.inflate(R.layout.add_place, (ViewGroup) findViewById(R.id.addPlaceActivity));
                final EditText textAddress = popupView.findViewById(R.id.address);
                final Spinner myplaces = popupView.findViewById(R.id.myplaces);
                ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(GeofenceActivity.this, R.array.myplaces, android.R.layout.simple_spinner_dropdown_item);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                myplaces.setAdapter(dataAdapter);
                myplaces.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        newPlace = adapterView.getSelectedItem().toString();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                popupView.findViewById(R.id.mylocationbutton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                        LocationRequest request = LocationRequest.create();
                        request.setInterval(1000);
                        request.setFastestInterval(1000);
                        final LocationCallback locationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                location = locationResult.getLastLocation();
                                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                                List<Address> addressList;
                                String address;
                                try {
                                    addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addressList.size() > 0) {
                                        address = addressList.get(0).getAddressLine(0);
                                        TextView label = popupView.findViewById(R.id.label);
                                        label.setVisibility(View.VISIBLE);
                                        textAddress.setVisibility(View.VISIBLE);
                                        textAddress.setText(address);
                                        client.removeLocationUpdates(this);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        client.requestLocationUpdates(request, locationCallback, null);
                    }
                });
                dialogBuilder.setView(popupView)
                        .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final EditText address = popupView.findViewById(R.id.address);
                                final GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                                Map<String, Object> data = new HashMap<>();
                                data.put("name", myName);
                                data.put("address", address.getText().toString());
                                data.put("location", geoPoint);
                                db.document("/users/" + userId + "/places/" + newPlace).set(data)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(GeofenceActivity.this, newPlace + "Added Successfully", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                e.printStackTrace();
                                                Toast.makeText(GeofenceActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                getMyPlaces();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getMyPlaces();
                            }
                        })
                        .create()
                        .show();
            }
        });

        getMyPlaces();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getMyPlaces();
    }

    private void getMyPlaces() {
        db.collection("/users/" + userId + "/places").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> documentList = queryDocumentSnapshots.getDocuments();
                        Map<String, Object> placeList = new HashMap<>();
                        for (int i = 0; i < documentList.size(); i++) {
                            placeList.put(documentList.get(i).getReference().getId(), documentList.get(i).getReference().getPath());
                        }
                        try {
                            PlacesAdapter placesAdapter = new PlacesAdapter(getApplicationContext(), placeList);
                            places.setAdapter(placesAdapter);
                        } catch (NullPointerException e) {
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
}

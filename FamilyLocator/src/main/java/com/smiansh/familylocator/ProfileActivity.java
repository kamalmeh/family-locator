package com.smiansh.familylocator;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "PROFILE_ACTIVITY";
    private static final int PERMISSION_REQUEST = 10;
    private static final int PHOTO_SELECTION_CODE = 11;
    //    private TextView currUser;
    private EditText firstName, lastName, phone;
    private Button update, addMember;
    private ImageView uploadImage;
    private ListView listView;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId;
    private SharedPreferences sp = null;


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        firstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 0) {
                    update.setEnabled(false);
                    firstName.setHintTextColor(Color.RED);
                } else {
                    update.setEnabled(true);
                    firstName.setHintTextColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        lastName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 0) {
                    update.setEnabled(false);
                    firstName.setHintTextColor(Color.RED);
                } else {
                    update.setEnabled(true);
                    firstName.setHintTextColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 0) {
                    update.setEnabled(false);
                    firstName.setHintTextColor(Color.RED);
                } else {
                    update.setEnabled(true);
                    firstName.setHintTextColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, Object> data = new HashMap<>();
                data.put("firstName", firstName.getText().toString());
                data.put("lastName", lastName.getText().toString());
                data.put("phone", phone.getText().toString());
                DocumentReference docRef = db.collection("users").document(userId);
                docRef.set(data, SetOptions.merge()).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
                int permission = ContextCompat.checkSelfPermission(ProfileActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ProfileActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST);
                }
                finish();
            }
        });

        addMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, AddMemberActivity.class);
                startActivity(intent);
            }
        });

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PHOTO_SELECTION_CODE);
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

            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                Toast.makeText(this, "Permission check success", Toast.LENGTH_SHORT).show();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_SELECTION_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                StorageReference mStorageRef;
                mStorageRef = FirebaseStorage.getInstance().getReference();

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    uploadImage.setImageBitmap(bitmap);
                    this.grantUriPermission(this.getPackageName(), selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    this.getContentResolver().takePersistableUriPermission(selectedImage, getIntent().getFlags());
                    sp.edit().putString("profileImage", String.valueOf(selectedImage)).apply();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                StorageReference riversRef = mStorageRef.child("images/" + userId);

                riversRef.putFile(selectedImage)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // Get a URL to the uploaded content
                                Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                                Map<String, Object> profileImageData = new HashMap<>();
                                if (downloadUrl != null) {
                                    profileImageData.put("profileImage", downloadUrl.getPath() + "/images/" + userId);
                                }
                                db.collection("users").document(userId).set(profileImageData, SetOptions.merge());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Toast.makeText(ProfileActivity.this, "Upload Failed", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        userId = getIntent().getStringExtra("userId");

        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        phone = findViewById(R.id.phone);
        update = findViewById(R.id.update);
//        currUser = findViewById(R.id.currUser);
        addMember = findViewById(R.id.addMember);
        uploadImage = findViewById(R.id.profileImage);
        listView = findViewById(R.id.membersList);

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        }

        try {
            Uri imageUri = Uri.parse(sp.getString("profileImage", null));
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(imageUri, projection, null, null, null);
            String path = null;
            assert cursor != null;
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                path = cursor.getString(column_index);
            }
            cursor.close();
            assert path != null;
            File file = new File(path);
            if (file.canRead()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(file.getPath());
                uploadImage.setImageBitmap(myBitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String fName = documentSnapshot.getString("firstName");
                            String lName = documentSnapshot.getString("lastName");
                            firstName.setText(fName);
                            lastName.setText(lName);
                            phone.setText(documentSnapshot.getString("phone"));
//                            currUser.setText(getString(R.string.welcome_text, fName, lName));

                            @SuppressWarnings("unchecked") Map<String, String> membersList =
                                    (Map<String, String>) documentSnapshot.get("family");
                            if (membersList != null) {
                                MembersListAdapter adapter = new MembersListAdapter(membersList);
                                listView.setAdapter(adapter);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

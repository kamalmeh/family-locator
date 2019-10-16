package com.smiansh.famtrack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.hbb20.CountryCodePicker;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "PROFILE_ACTIVITY";
    private static final int PERMISSION_REQUEST = 10;
    private static final int PHOTO_SELECTION_CODE = 11;
    //    private TextView currUser;
    private EditText firstName, lastName, phone;
    private Button update, addMember;
    private ImageView uploadImage;
    private ListView listView;
    CountryCodePicker ccp;
    private String selectedCountryCode = "";
    private Map<String, Object> userData = new HashMap<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId;
    private SharedPreferences sp = null;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getProfileData();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String userType = documentSnapshot.getString("userType");
                        if (firstName != null && lastName != null && userType != null) {
                            if (firstName.equals("") || lastName.equals("")) {
                                Toast.makeText(ProfileActivity.this, "Please update your \"First Name\" and \"Last Name\"", Toast.LENGTH_SHORT).show();
                            } else finish();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Please Click Edit and then Update to correctly save the profile details", Toast.LENGTH_SHORT).show();
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
                    firstName.setHintTextColor(Color.RED);
                } else {
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
                    firstName.setHintTextColor(Color.RED);
                } else {
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
                    firstName.setHintTextColor(Color.RED);
                } else {
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
                if (update.getText().equals("Update")) {
                    String fName = firstName.getText().toString();
                    String lName = lastName.getText().toString();
                    if (fName.isEmpty()) {
                        firstName.setTextColor(Color.RED);
                        firstName.setHintTextColor(Color.RED);
                        firstName.setFocusable(true);
                        Toast.makeText(ProfileActivity.this, "First Name is Mandatory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    firstName.setTextColor(Color.BLACK);
                    firstName.setHintTextColor(Color.BLACK);
                    if (lName.isEmpty()) {
                        lastName.setTextColor(Color.RED);
                        lastName.setHintTextColor(Color.RED);
                        lastName.setFocusable(true);
                        Toast.makeText(ProfileActivity.this, "Last Name is Mandatory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    lastName.setTextColor(Color.BLACK);
                    lastName.setHintTextColor(Color.BLACK);
                    Map<String, Object> data = new HashMap<>();
                    data.put("firstName", fName);
                    data.put("lastName", lName);

                    TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    if (tm != null) {
                        ccp.setCountryForNameCode(tm.getNetworkCountryIso());
                    }
                    if (selectedCountryCode.length() == 0)
                        selectedCountryCode = ccp.getSelectedCountryCodeWithPlus();
                    if (phone.getText().toString().length() > 0 && phone.getText().toString().length() < 10) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
                        builder.setMessage("Please enter 10 digit phone number")
                                .setCancelable(false)
                                .setPositiveButton("Back", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                        return;
                    } else {
                        String number = phone.getText().toString();
                        if (number.length() > 0)
                            data.put("phone", selectedCountryCode + "-" + number);
                    }
                    if (!userData.containsKey("allowedMembers"))
                        data.put("allowedMembers", String.valueOf(Helper.ALLOWED_MEMBERS));
                    if (!userData.containsKey("userType"))
                        data.put("userType", "regular");
                    if (!userData.containsKey("status"))
                        data.put("status", "Signed In");
                    DocumentReference docRef = db.collection("users").document(userId);
                    docRef.set(data, SetOptions.merge()).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                } else {
                    firstName.setEnabled(true);
                    lastName.setEnabled(true);
                    phone.setEnabled(true);
                    uploadImage.setEnabled(true);
                    update.setText(R.string.update);
                    ccp.setCcpClickable(true);
                }
            }
        });

        addMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, AddMemberActivity.class);
                intent.putExtra("userId", userId);
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
                Log.i(TAG, "User interaction was cancelled.");
            } else {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted.
                    Log.i(TAG, "Permission was granted");
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
//                    sp.edit().putString("profileImage", String.valueOf(selectedImage)).apply();
                    sp.edit().putString("profileImage", selectedImage.getPath()).apply();
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
        firstName.setEnabled(false);
        lastName.setEnabled(false);
        phone.setEnabled(false);
        uploadImage.setEnabled(false);
        update.setText(R.string.profileEdit);
        ccp = findViewById(R.id.ccp);
        ccp.setCcpClickable(false);

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        int permission_read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permission_write = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission_read != PackageManager.PERMISSION_GRANTED &&
                permission_write != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST);
        }

        try {
            String path = sp.getString("profileImage", null);
            try {
                FileInputStream is;
                if (path != null) {
                    is = new FileInputStream(new File(path));
                    Bitmap myBitmap = BitmapFactory.decodeStream(is);
                    uploadImage.setImageBitmap(myBitmap);
                    is.close();
                } else {
                    downloadProfileImage();
                }
            } catch (Exception e) {
                e.printStackTrace();
                downloadProfileImage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        getProfileData();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getBaseContext(), "Touch and Hold to delete", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getProfileData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            userData = documentSnapshot.getData();
                            String fName = documentSnapshot.getString("firstName");
                            String lName = documentSnapshot.getString("lastName");
                            firstName.setText(fName);
                            lastName.setText(lName);
                            String phoneNumber = documentSnapshot.getString("phone");
                            if (phoneNumber != null) {
                                String[] nums = phoneNumber.split("-");
                                selectedCountryCode = nums[0];
                                if (nums.length == 2)
                                    phone.setText(nums[1]);
                            }
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
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final String name = adapterView.getAdapter().getItem(i).toString().split("=")[0];
                final String item = adapterView.getAdapter().getItem(i).toString().split("=")[1];
                final DocumentReference docRef = db.document("users/" + userId);
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
                builder.setMessage(getString(R.string.delete_msg, name))
                        .setTitle("Please Confirm Delete")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                docRef.get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                Map<String, Object> data = documentSnapshot.getData();
                                                if (data != null) {
                                                    //noinspection unchecked
                                                    Map<String, String> family = (Map<String, String>) data.get("family");
                                                    if (family != null) {
                                                        family.remove(item);
                                                    }
                                                    data.put("family", family);
                                                    docRef.set(data);
                                                    Toast.makeText(ProfileActivity.this, name + " deleted", Toast.LENGTH_LONG).show();
                                                    getProfileData();
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
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(ProfileActivity.this, "Delete cancelled", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        });
    }


    private void downloadProfileImage() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference profilePicPath = storageReference.child("images/" + userId);
        profilePicPath.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
                        try {
                            downloadFilesTask.execute(new URL(uri.toString()));
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

    public void onCountryPickerClick(View view) {
        ccp.setOnCountryChangeListener(new CountryCodePicker.OnCountryChangeListener() {
            @Override
            public void onCountrySelected() {
                selectedCountryCode = ccp.getSelectedCountryCodeWithPlus();
            }
        });
    }


    @SuppressLint("StaticFieldLeak")
    class DownloadFilesTask extends AsyncTask<URL, Void, Void> {

        @Override
        protected Void doInBackground(URL... urls) {
            InputStream is = null;
            try {
                HttpsURLConnection connection = (HttpsURLConnection) new URL(urls[0].toString()).openConnection();
                Log.i(TAG, urls[0].toString());
                connection.connect();
                is = connection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            final Bitmap bitmap = BitmapFactory.decodeStream(is);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), userId);
            if (file.exists())
                if (!file.delete()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ProfileActivity.this, "File could not be deleted, skipping", Toast.LENGTH_SHORT).show();
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
                sp.edit().putString("profileImage", file.getAbsolutePath()).apply();
                os.flush();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    uploadImage.setImageBitmap(bitmap);
                }
            });
            return null;
        }
    }
}

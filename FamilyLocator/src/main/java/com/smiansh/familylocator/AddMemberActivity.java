package com.smiansh.familylocator;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddMemberActivity extends AppCompatActivity {
    String memberId = null;
    private EditText authCode;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);
        Button addButton = findViewById(R.id.addButton);
        authCode = findViewById(R.id.authCode);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String userId = getIntent().getStringExtra("userId");
                if (userId == null) {
                    Toast.makeText(AddMemberActivity.this, "Invalid User Session", Toast.LENGTH_SHORT).show();
                    return;
                }
                final String memberAuthCode = authCode.getText().toString();
                if (memberAuthCode.isEmpty()) {
                    authCode.setHintTextColor(Color.RED);
                    return;
                }
                final DocumentReference docRef = db.collection("users").document(userId);
                final DocumentReference authRef = db.collection("authcodes").document(memberAuthCode);
                authRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                memberId = documentSnapshot.getString("userId");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(AddMemberActivity.this,
                                        "Invalid Authentication Code",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                docRef.get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot != null) {
                                    Map<String, Object> family = (Map<String, Object>) documentSnapshot.get("family");
                                    if (family == null) {
                                        family = new HashMap<>();
                                    }
                                    family.put(memberId, db.document("/users/" + memberId));
                                    Map<String, Object> familyData = new HashMap<>();
                                    familyData.put("family", family);
                                    docRef.update(familyData);
                                }
                            }
                        });
                finish();
            }
        });
    }
}

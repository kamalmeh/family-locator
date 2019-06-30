package com.smiansh.familylocator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddMemberActivity extends AppCompatActivity {
    private Button addButton;
    private EditText number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);
        addButton = findViewById(R.id.addButton);
        number = findViewById(R.id.number);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userId = getIntent().getStringExtra("userId");
                Toast.makeText(AddMemberActivity.this, "User Id: userId\nMobile: " + number.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

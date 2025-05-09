package com.android.newsapp; // Or your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Optional: if you want a toolbar

public class ProfileActivity extends AppCompatActivity {

    private TextView textViewProfileUsername;
    private Button buttonEditTopics;
    private Button buttonViewBookmarks;
    private Button buttonProfileLogout;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Optional: Add a Toolbar
        // Toolbar toolbar = findViewById(R.id.toolbarProfile); // Add Toolbar to XML if needed
        // setSupportActionBar(toolbar);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Back button
        // getSupportActionBar().setTitle("My Profile");

        textViewProfileUsername = findViewById(R.id.textViewProfileUsername);
        buttonEditTopics = findViewById(R.id.buttonEditTopics);
        buttonViewBookmarks = findViewById(R.id.buttonViewBookmarks);
        buttonProfileLogout = findViewById(R.id.buttonProfileLogout);

        sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);

        loadUserProfile();

        buttonEditTopics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ChooseTopicsActivity to edit topics
                // ChooseTopicsActivity should handle loading existing topics if launched for editing
                Intent intent = new Intent(ProfileActivity.this, ChooseTopicsActivity.class);
                startActivity(intent);
            }
        });

        buttonViewBookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, BookmarksActivity.class));
            }
        });

        buttonProfileLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
    }

    private void loadUserProfile() {
        String username = sharedPreferences.getString(LoginActivity.KEY_USERNAME, "User");
        textViewProfileUsername.setText("Username: " + username);
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false);
        editor.remove(LoginActivity.KEY_USER_ID);
        editor.remove(LoginActivity.KEY_USERNAME);
        editor.remove(LoginActivity.KEY_TOPICS_CHOSEN);
        editor.apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Optional: Handle Toolbar back button
    // @Override
    // public boolean onSupportNavigateUp() {
    //     onBackPressed();
    //     return true;
    // }
}
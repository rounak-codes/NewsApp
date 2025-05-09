package com.android.newsapp; // Or your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin;
    private TextView textViewGoToRegister;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;

    public static final String PREFS_NAME = "NewsAppPrefs";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USERNAME = "username"; // Store username for display
    public static final String KEY_TOPICS_CHOSEN = "topicsChosen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if user is already logged in
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            redirectLoggedInUser();
            return; // Skip login screen
        }

        setContentView(R.layout.activity_login);

        // Initialize UI components
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewGoToRegister = findViewById(R.id.textViewGoToRegister);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close database connection
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }

    /**
     * Redirects logged-in user to the appropriate activity
     * based on whether they've chosen topics or not
     */
    private void redirectLoggedInUser() {
        int userId = sharedPreferences.getInt(KEY_USER_ID, -1);

        // Check if user has chosen topics by checking database
        boolean hasChosenTopics = !databaseHelper.getUserTopics(userId).isEmpty();

        // Update shared preferences to match database state
        if (hasChosenTopics) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_TOPICS_CHOSEN, true);
            editor.apply();
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, ChooseTopicsActivity.class));
        }
        finish();
    }

    private void loginUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.checkUser(username, password)) {
            int userId = databaseHelper.getUserIdByUsername(username);
            if (userId != -1) {
                // Save login information
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_IS_LOGGED_IN, true);
                editor.putInt(KEY_USER_ID, userId);
                editor.putString(KEY_USERNAME, username);
                editor.apply();

                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                // Check if user has chosen topics in database
                boolean hasChosenTopics = !databaseHelper.getUserTopics(userId).isEmpty();

                // Update shared preferences
                editor.putBoolean(KEY_TOPICS_CHOSEN, hasChosenTopics);
                editor.apply();

                // Navigate to appropriate screen
                if (hasChosenTopics) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, ChooseTopicsActivity.class));
                }
                finish();
            } else {
                Toast.makeText(this, "Login successful, but could not retrieve user ID.", Toast.LENGTH_LONG).show();
                // This case should ideally not happen if checkUser is true
            }
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_LONG).show();
        }
    }
}
package com.android.newsapp; // Or your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    // Splash screen duration in milliseconds
    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Set the splash screen layout

        // Use a Handler to delay the transition to the next activity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Get SharedPreferences to check login status
            SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
            boolean isLoggedIn = sharedPreferences.getBoolean(LoginActivity.KEY_IS_LOGGED_IN, false);
            boolean topicsChosen = sharedPreferences.getBoolean(LoginActivity.KEY_TOPICS_CHOSEN, false);
            int userId = sharedPreferences.getInt(LoginActivity.KEY_USER_ID, -1); // Get user ID to double check topics if needed

            Intent nextActivityIntent;

            if (isLoggedIn) {
                // User is logged in
                // Re-check topics chosen status from DB on splash, in case it wasn't saved correctly before
                DatabaseHelper databaseHelper = new DatabaseHelper(this);
                boolean hasChosenTopics = !databaseHelper.getUserTopics(userId).isEmpty();

                if (hasChosenTopics) {
                    // Logged in and topics chosen, go to MainActivity
                    nextActivityIntent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // Logged in but topics not chosen, go to ChooseTopicsActivity
                    nextActivityIntent = new Intent(SplashActivity.this, ChooseTopicsActivity.class);
                }
            } else {
                // User is not logged in, go to LoginActivity
                nextActivityIntent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            // Start the next activity
            startActivity(nextActivityIntent);

            // Finish the splash activity so the user cannot go back to it
            finish();

        }, SPLASH_DURATION);
    }
}

package com.android.newsapp; // Or your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Import ContextCompat
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChooseTopicsActivity extends AppCompatActivity {

    private ChipGroup chipGroupTopics;
    private Button buttonSaveTopics;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private int userId;

    // Define your available topics
    private final List<String> availableTopics = Arrays.asList(
            "Business", "Entertainment", "General", "Health", "Science", "Sports", "Technology"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_topics);

        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        userId = sharedPreferences.getInt(LoginActivity.KEY_USER_ID, -1);

        if (userId == -1) {
            // Should not happen if user is logged in
            Toast.makeText(this, "Error: User not identified.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        chipGroupTopics = findViewById(R.id.chipGroupTopics);
        buttonSaveTopics = findViewById(R.id.buttonSaveTopics);

        populateChips();
        loadUserTopics(); // Load existing topics if user is editing

        buttonSaveTopics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSelectedTopics();
            }
        });
    }

    private void populateChips() {
        chipGroupTopics.removeAllViews(); // Clear existing chips if necessary

        for (String topic : availableTopics) {
            Chip chip = new Chip(this); // 'this' refers to the Activity context
            chip.setText(topic);
            chip.setCheckable(true); // Make chips selectable

            // --- FIX: Commented out placeholder resource calls ---
            // chip.setChipBackgroundColorResource(R.color.your_color); // Commented out
            // chip.setTextAppearance(R.style.YourChipTextStyle); // Commented out
            // ------------------------------------------------------

            // --- ADDED: Explicitly set the text color for visibility ---
            // Using black from Android's built-in colors as a default visible color
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));

            // Optional: If you have defined a custom text color selector for chips, use it here
            // chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_color_selector));


            // Optional: Adjust chip appearance for better contrast if needed
            chip.setChipBackgroundColorResource(R.color.ic_launcher_background); // Example
            chip.setChipStrokeColorResource(R.color.ic_launcher_background); // Example


            // Add the chip to the ChipGroup
            chipGroupTopics.addView(chip);
        }
    }

    private void loadUserTopics() {
        List<String> userTopics = databaseHelper.getUserTopics(userId);
        for (int i = 0; i < chipGroupTopics.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTopics.getChildAt(i);
            if (userTopics.contains(chip.getText().toString())) {
                chip.setChecked(true);
            }
        }
    }

    private void saveSelectedTopics() {
        List<String> selectedTopics = new ArrayList<>();
        for (int i = 0; i < chipGroupTopics.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTopics.getChildAt(i);
            if (chip.isChecked()) {
                selectedTopics.add(chip.getText().toString());
            }
        }

        if (selectedTopics.isEmpty()) {
            Toast.makeText(this, "Please select at least one topic.", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseHelper.updateUserTopics(userId, selectedTopics); // This will clear old and add new

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(LoginActivity.KEY_TOPICS_CHOSEN, true);
        editor.apply();

        Toast.makeText(this, "Topics saved!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(ChooseTopicsActivity.this, MainActivity.class));
        finish();
    }
}

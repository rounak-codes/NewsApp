package com.android.newsapp; // Or your actual package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
// TabLayout and TabLayoutMediator are no longer needed
// import com.google.android.material.tabs.TabLayout;
// import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    // TabLayout is no longer needed
    // private TabLayout tabLayout;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Use the updated activity_main.xml

        Toolbar toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);
        // Optionally set a title for the unified feed
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("News Feed");
        }


        sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);

        viewPager = findViewById(R.id.viewPager);
        // TabLayout is no longer needed
        // tabLayout = findViewById(R.id.tabLayout);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);


        // Disable swiping between pages if you only have one fragment
        viewPager.setUserInputEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); // Create main_menu.xml in res/menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Use if-else if for menu items as you are not using android.R.id values here
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class)); // Create ProfileActivity later
            return true;
        } else if (id == R.id.action_bookmarks) {
            startActivity(new Intent(this, BookmarksActivity.class)); // Create BookmarksActivity later
            return true;
        } else if (id == R.id.action_search) {
            // Implement Search: Open a new SearchActivity or handle in-app search
            // For now, a Toast message as placeholder
            // Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SearchActivity.class)); // Create SearchActivity later
            return true;
        } else if (id == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false);
        editor.remove(LoginActivity.KEY_USER_ID);
        editor.remove(LoginActivity.KEY_USERNAME);
        editor.remove(LoginActivity.KEY_TOPICS_CHOSEN); // Also clear this
        editor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

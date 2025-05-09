package com.android.newsapp; // Or your actual package name

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BookmarksActivity extends AppCompatActivity implements NewsAdapter.OnBookmarkClickListener {

    private RecyclerView recyclerViewBookmarks;
    private NewsAdapter newsAdapter;
    private List<NewsItem> bookmarkedItemsList;
    private DatabaseHelper databaseHelper;
    private TextView textViewNoBookmarks;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        Toolbar toolbar = findViewById(R.id.toolbarBookmarks);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
        }

        recyclerViewBookmarks = findViewById(R.id.recyclerViewBookmarks);
        textViewNoBookmarks = findViewById(R.id.textViewNoBookmarks);
        databaseHelper = new DatabaseHelper(this);
        bookmarkedItemsList = new ArrayList<>();

        SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt(LoginActivity.KEY_USER_ID, -1);

        recyclerViewBookmarks.setLayoutManager(new LinearLayoutManager(this));
        // We can reuse NewsAdapter. The OnBookmarkClickListener will handle unbookmarking.
        newsAdapter = new NewsAdapter(this, bookmarkedItemsList, currentUserId, this);
        recyclerViewBookmarks.setAdapter(newsAdapter);

        loadBookmarkedArticles();
    }

    private void loadBookmarkedArticles() {
        if (currentUserId == -1) {
            textViewNoBookmarks.setText("Error: User not logged in.");
            textViewNoBookmarks.setVisibility(View.VISIBLE);
            recyclerViewBookmarks.setVisibility(View.GONE);
            return;
        }

        List<NewsItem> bookmarks = databaseHelper.getAllBookmarks(currentUserId);
        bookmarkedItemsList.clear();

        if (bookmarks != null && !bookmarks.isEmpty()) {
            bookmarkedItemsList.addAll(bookmarks);
            textViewNoBookmarks.setVisibility(View.GONE);
            recyclerViewBookmarks.setVisibility(View.VISIBLE);
        } else {
            textViewNoBookmarks.setVisibility(View.VISIBLE);
            recyclerViewBookmarks.setVisibility(View.GONE);
        }
        newsAdapter.notifyDataSetChanged();
    }

    // Implementation of NewsAdapter.OnBookmarkClickListener
    @Override
    public void onBookmarkClick(NewsItem newsItem, boolean isBookmarked) {
        // In BookmarksActivity, if a bookmark icon is clicked, it's always to remove it.
        // The NewsAdapter's internal logic already handles DB update and icon change.
        // We just need to refresh the list here if an item is removed.
        if (!isBookmarked) { // Item was unbookmarked
            bookmarkedItemsList.remove(newsItem); // Requires NewsItem to have equals/hashCode or iterate
            // For simplicity, just reload all:
            loadBookmarkedArticles();
            // A more efficient way would be to find and remove the specific item
            // and use notifyItemRemoved(position).
        }
        // If isBookmarked is true, it means it was just added, which shouldn't happen from this screen's perspective
        // unless you allow adding bookmarks from a different source into this view.
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Go back to the previous activity
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh bookmarks in case user unbookmarked from main feed and came back here
        loadBookmarkedArticles();
    }
}
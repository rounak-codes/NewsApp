package com.android.newsapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson; // Import Gson
import com.google.gson.reflect.TypeToken; // Import TypeToken

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type; // Import Type
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements NewsAdapter.OnBookmarkClickListener {

    private static final String TAG = "SearchActivity";

    // Removed the hardcoded API key variable

    private RecyclerView recyclerViewSearchResults;
    private NewsAdapter searchResultsAdapter;
    private List<NewsItem> searchResultsList;
    private ProgressBar progressBarSearch;
    private TextView textViewNoResults;
    private RequestQueue requestQueue;
    private DatabaseHelper databaseHelper;
    private int currentUserId;
    private SharedPreferences sharedPreferences;

    // --- API Key Management for Search ---
    // Using the first API key from BuildConfig for search
    // You could add logic here to cycle through keys on error if needed,
    // similar to NewsListFragment, but for a single search request,
    // starting with the primary key is usually sufficient.
    private String currentApiKey;
    // -------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search); // Create this layout file

        Toolbar toolbar = findViewById(R.id.toolbarSearch);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search News");
        }

        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        textViewNoResults = findViewById(R.id.textViewNoResults);

        sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt(LoginActivity.KEY_USER_ID, -1);

        databaseHelper = new DatabaseHelper(this);
        searchResultsList = new ArrayList<>();
        // Reusing NewsAdapter
        searchResultsAdapter = new NewsAdapter(this, searchResultsList, currentUserId, this);
        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSearchResults.setAdapter(searchResultsAdapter);

        requestQueue = Volley.newRequestQueue(this);

        // --- Initialize the current API key ---
        // Use the first key from BuildConfig
        currentApiKey = BuildConfig.NEWS_API_KEY_1; // Assuming NEWS_API_KEY_1 is your primary key
        // --------------------------------------


        if (currentUserId == -1) {
            Toast.makeText(this, "User not logged in. Bookmark functionality will be limited.", Toast.LENGTH_LONG).show();
            // Proceed without user-specific features or redirect to login
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu); // Create search_menu.xml
        MenuItem searchItem = menu.findItem(R.id.action_search_view);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search news articles...");
        searchView.requestFocus();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    performSearch(query);
                    searchView.clearFocus(); // Hide keyboard
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Optional: Implement live search here or clear results if newText is empty
                if (TextUtils.isEmpty(newText)) {
                    clearSearchResults();
                }
                return true;
            }
        });
        return true;
    }

    private void performSearch(String query) {
        Log.d(TAG, "Performing search for: " + query);
        progressBarSearch.setVisibility(View.VISIBLE);
        textViewNoResults.setVisibility(View.GONE);
        searchResultsList.clear();
        searchResultsAdapter.notifyDataSetChanged();

        // --- Use the current API key ---
        if (currentApiKey == null || currentApiKey.isEmpty() || currentApiKey.equals("YOUR_ACTUAL_API_KEY_HERE")) { // Check against placeholder too
            Log.e(TAG, "API Key is not configured in BuildConfig.");
            Toast.makeText(this, "API Key not configured. Search cannot proceed.", Toast.LENGTH_LONG).show();
            progressBarSearch.setVisibility(View.GONE);
            textViewNoResults.setText("Search feature not available: API Key missing.");
            textViewNoResults.setVisibility(View.VISIBLE);
            return;
        }
        // -------------------------------


        // --- Using thenewsapi.com /v1/news/all endpoint for searching ---
        String baseUrl = "https://api.thenewsapi.com/v1/news/all?";

        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        // Use 'search' parameter for the query
        urlBuilder.append("search=").append(Uri.encode(query));

        // Add language parameter, e.g., English
        urlBuilder.append("&language=en"); // You can make language dynamic or configurable

        // Add sorting, e.g., by relevance or published_at
        urlBuilder.append("&sort_by=relevance"); // Other options: published_at, popularity

        // Optional: Add a limit if needed, though thenewsapi.com default might be okay for search
        // urlBuilder.append("&limit=50"); // Example limit

        // Append the current API Key (using api_token parameter)
        urlBuilder.append("&api_token=").append(currentApiKey);

        String url = urlBuilder.toString();
        // --- End of API Call Logic for thenewsapi.com ---


        Log.d(TAG, "Search URL: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBarSearch.setVisibility(View.GONE);
                    try {
                        // --- JSON Parsing Logic for thenewsapi.com ---
                        JSONArray articles = response.getJSONArray("data"); // Articles are under the 'data' key
                        if (articles.length() == 0) {
                            textViewNoResults.setText("No results found for \"" + query + "\"");
                            textViewNoResults.setVisibility(View.VISIBLE);
                        } else {
                            textViewNoResults.setVisibility(View.GONE);
                        }

                        for (int i = 0; i < articles.length(); i++) {
                            JSONObject articleObject = articles.getJSONObject(i);
                            String title = articleObject.optString("title", "No Title");
                            String description = articleObject.optString("snippet", "No Description"); // Description is 'snippet'
                            String articleUrl = articleObject.optString("url");
                            String imageUrl = articleObject.optString("image_url"); // Image URL is 'image_url'
                            String publishedAt = articleObject.optString("published_at"); // Published date is 'published_at'

                            if (title.equals("No Title") || title.isEmpty() || articleUrl.isEmpty() || title.equals("[Removed]")) {
                                continue;
                            }
                            // Using NewsItem model
                            searchResultsList.add(new NewsItem(title, description, articleUrl, imageUrl, publishedAt));
                        }
                        searchResultsAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(SearchActivity.this, "Error parsing search results.", Toast.LENGTH_SHORT).show();
                        textViewNoResults.setText("Error displaying results.");
                        textViewNoResults.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    progressBarSearch.setVisibility(View.GONE);
                    Log.e(TAG, "Volley error: " + error.toString());
                    String errorMessage = "Error fetching search results. Please try again.";
                    int statusCode = (error.networkResponse != null) ? error.networkResponse.statusCode : -1;
                    Log.e(TAG, "Status code: " + statusCode);

                    // --- Handle API Key/Rate Limit Errors ---
                    if(statusCode == 401 || statusCode == 403 || statusCode == 429) {
                        errorMessage = "API Error (" + statusCode + "). Could not fetch search results.";
                        // You could add logic here to try another API key if needed
                    } else if (statusCode >= 400 && statusCode < 500) {
                        errorMessage = "Client error (" + statusCode + "). Could not fetch search results.";
                    } else if (statusCode >= 500) {
                        errorMessage = "Server error (" + statusCode + "). Please try again.";
                    } else {
                        errorMessage = "Network error. Check connection.";
                    }
                    // --- End of API Key/Rate Limit Errors ---

                    Toast.makeText(SearchActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    textViewNoResults.setText(errorMessage);
                    textViewNoResults.setVisibility(View.VISIBLE);
                }
        );
        // Add a tag to the request so we can cancel it if the activity is destroyed
        jsonObjectRequest.setTag(TAG);
        requestQueue.add(jsonObjectRequest);
    }

    private void clearSearchResults() {
        searchResultsList.clear();
        searchResultsAdapter.notifyDataSetChanged();
        textViewNoResults.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close this activity and return to previous one
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Implementation of NewsAdapter.OnBookmarkClickListener
    @Override
    public void onBookmarkClick(NewsItem newsItem, boolean isBookmarked) {
        // The NewsAdapter itself handles the database operation and icon change.
        // If you needed to, for example, update a global bookmark counter shown in SearchActivity,
        // you could do that here. For now, a simple log/toast is sufficient.
        if (isBookmarked) {
            // Toast.makeText(this, "\"" + newsItem.getTitle() + "\" bookmarked", Toast.LENGTH_SHORT).show();
        } else {
            // Toast.makeText(this, "\"" + newsItem.getTitle() + "\" bookmark removed", Toast.LENGTH_SHORT).show();
        }
        // No need to call notifyDataSetChanged() on the adapter here as NewsAdapter does it,
        // or it's visually updated by the ImageButton state change.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG); // Cancel any pending Volley requests with this tag
        }
    }
}

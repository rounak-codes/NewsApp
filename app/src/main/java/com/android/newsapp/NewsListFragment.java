package com.android.newsapp; // Or your actual package name

import android.net.Uri;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Collections; // Import Collections for sorting
import java.util.Arrays; // Import Arrays

public class NewsListFragment extends Fragment implements NewsAdapter.OnBookmarkClickListener {

    // countryCode is no longer used for filtering the main feed API call
    // private static final String ARG_COUNTRY_CODE = "country_code";
    // private String countryCode; // Keep for potential future use or if needed elsewhere, but not for API fetch logic here

    private RecyclerView recyclerViewNews;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsItemList;
    private ProgressBar progressBar;
    private TextView textViewNoNews;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RequestQueue requestQueue;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private int currentUserId;
    private List<String> userTopics;

    // --- Multi-API Key Management ---
    // IMPORTANT: Replace with your actual list of API keys from your group members
    private List<String> apiKeys = new ArrayList<>(Arrays.asList(
            BuildConfig.NEWS_API_KEY_1, // Your key from gradle.properties
            BuildConfig.NEWS_API_KEY_2,
            BuildConfig.NEWS_API_KEY_3
            // Add more keys as needed
    ));
    private int currentApiKeyIndex = 0; // Index of the currently used API key
    // ---------------------------------


    // Pagination variables
    private int currentPage = 1;
    private final int articlesPerPage = 3;
    private boolean isLoading = false;
    private boolean hasMorePages = true; // Assume true initially

    // Cache variables
    private static final String CACHE_PREFS_NAME = "NewsCachePrefs";
    private static final String CACHE_KEY_PREFIX = "cache_unified_thenewsapi_"; // Updated cache key prefix for the new API
    private static final long CACHE_DURATION_MS = 15 * 60 * 1000; // 15 minutes


    public static NewsListFragment newInstance(String countryCode) {
        NewsListFragment fragment = new NewsListFragment();
        // No need to pass countryCode to arguments if not used for API filtering
        // Bundle args = new Bundle();
        // args.putString(ARG_COUNTRY_CODE, countryCode);
        // fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No need to retrieve countryCode from arguments if not used
        // if (getArguments() != null) {
        //     countryCode = getArguments().getString(ARG_COUNTRY_CODE);
        // }
        newsItemList = new ArrayList<>();
        databaseHelper = new DatabaseHelper(getContext());
        sharedPreferences = requireActivity().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt(LoginActivity.KEY_USER_ID, -1);
        // Load user topics once, but reload in onResume if needed
        userTopics = databaseHelper.getUserTopics(currentUserId);

        // --- Initialize API key index ---
        // You could potentially save and load the last used key index from SharedPreferences
        // to persist it across app sessions, but for simplicity, we'll start from the first key.
        currentApiKeyIndex = 0;
        // --------------------------------
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news_list, container, false);

        recyclerViewNews = view.findViewById(R.id.recyclerViewNews);
        progressBar = view.findViewById(R.id.progressBar);
        textViewNoNews = view.findViewById(R.id.textViewNoNews);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerViewNews.setLayoutManager(layoutManager);
        newsAdapter = new NewsAdapter(getContext(), newsItemList, currentUserId, this);
        recyclerViewNews.setAdapter(newsAdapter);

        requestQueue = Volley.newRequestQueue(requireContext());

        swipeRefreshLayout.setOnRefreshListener(this::onSwipeRefresh); // Use a dedicated method for swipe refresh

        // Add scroll listener for pagination
        recyclerViewNews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Check if we are near the end of the list and not currently loading
                if (!isLoading && hasMorePages) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        Log.d("NewsListFragment", "Scrolled to end, loading next page.");
                        fetchNewsFromApi(false); // Fetch the next page (not a retry)
                    }
                }
            }
        });


        loadNews(); // Load from cache first, then API if needed

        return view;
    }

    private void loadNews() {
        // Check for user topics here before attempting to load/fetch
        if (userTopics == null || userTopics.isEmpty()) {
            textViewNoNews.setVisibility(View.VISIBLE);
            textViewNoNews.setText("Please select topics in your profile to see personalized news."); // Inform the user
            progressBar.setVisibility(View.GONE);
            newsItemList.clear();
            newsAdapter.notifyDataSetChanged();
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            // No need to show a Toast here, the TextView handles it.
            return;
        }
        textViewNoNews.setVisibility(View.GONE); // Hide "No News" message if topics exist

        List<NewsItem> cachedNews = getCachedNews();
        if (cachedNews != null && !cachedNews.isEmpty()) {
            Log.d("NewsListFragment", "Loading news from cache for " + getCacheKey());
            newsItemList.clear(); // Clear existing list before adding from cache
            newsItemList.addAll(cachedNews);
            newsAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);

            // Determine the next page to load based on cached items
            currentPage = (cachedNews.size() / articlesPerPage) + 1;
            hasMorePages = true; // Assume more pages are available after cache

            // Only fetch from API if explicitly refreshing or if cache size is less than limit (implies incomplete page)
            if (swipeRefreshLayout.isRefreshing() || cachedNews.size() < articlesPerPage) {
                Log.d("NewsListFragment", "Swipe refresh detected or cache incomplete. Fetching from API.");
                fetchNewsFromApi(false); // Fetch from API (not a retry)
            } else {
                // If not refreshing, and cache was loaded, stop refreshing animation if it was somehow active
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        } else {
            Log.d("NewsListFragment", "Cache empty or expired for " + getCacheKey() + ". Fetching from API (page 1).");
            currentPage = 1; // Start from page 1
            hasMorePages = true; // Assume more pages are available
            fetchNewsFromApi(false); // Fetch from API (not a retry)
        }
    }

    private void onSwipeRefresh() {
        Log.d("NewsListFragment", "Swipe refresh triggered. Clearing data and fetching page 1.");
        newsItemList.clear(); // Clear current items
        newsAdapter.notifyDataSetChanged();
        currentPage = 1; // Reset to the first page
        hasMorePages = true; // Assume more pages are available after refresh
        currentApiKeyIndex = 0; // Reset to the first API key on refresh
        clearCacheForCurrentConfig(); // Clear cache on refresh
        fetchNewsFromApi(false); // Fetch the first page (not a retry)
    }

    // --- Modified fetchNewsFromApi to handle API key switching ---
    private void fetchNewsFromApi(boolean isRetry) {
        // Re-check topics before fetching
        if (userTopics == null || userTopics.isEmpty()) {
            if (swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.GONE);
            textViewNoNews.setVisibility(View.VISIBLE);
            textViewNoNews.setText("Please select topics in your profile to see personalized news."); // Ensure message is shown
            newsItemList.clear();
            newsAdapter.notifyDataSetChanged();
            isLoading = false; // Reset loading flag
            return;
        }

        // Check if we have API keys to try
        if (apiKeys == null || apiKeys.isEmpty()) {
            if (swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.GONE);
            textViewNoNews.setVisibility(View.VISIBLE);
            textViewNoNews.setText("No API keys available to fetch news.");
            isLoading = false;
            hasMorePages = false;
            Toast.makeText(getContext(), "No API keys available.", Toast.LENGTH_LONG).show();
            return;
        }

        // If this is not a retry and we've exhausted all keys, stop.
        // If it IS a retry, we've already handled the key switching in onErrorResponse.
        if (!isRetry && currentApiKeyIndex >= apiKeys.size()) {
            if (swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.GONE);
            textViewNoNews.setVisibility(View.VISIBLE);
            textViewNoNews.setText("Could not fetch news after trying all available API keys.");
            isLoading = false;
            hasMorePages = false;
            Toast.makeText(getContext(), "All API keys failed to fetch news.", Toast.LENGTH_LONG).show();
            return;
        }

        textViewNoNews.setVisibility(View.GONE); // Hide "No News" message
        progressBar.setVisibility(View.VISIBLE);
        isLoading = true; // Set loading flag

        // Get the current API key to use
        String currentApiKey = apiKeys.get(currentApiKeyIndex);
        Log.d("NewsListFragment", "Using API Key Index: " + currentApiKeyIndex);


        String url;

        // --- API Call Logic for thenewsapi.com (Unified Feed using topics) ---
        String baseUrl = "https://api.thenewsapi.com/v1/news/all?"; // thenewsapi.com Everything endpoint

        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        // Join topics with a comma or space for the 'search' parameter (documentation is a bit vague, comma is common)
        // Let's use comma-separated for the 'search' parameter
        StringBuilder queryBuilder = new StringBuilder();
        for (int i = 0; i < userTopics.size(); i++) {
            queryBuilder.append(userTopics.get(i));
            if (i < userTopics.size() - 1) {
                queryBuilder.append(","); // Use comma as separator
            }
        }
        String topicQuery = queryBuilder.toString();

        if (!topicQuery.isEmpty()) {
            urlBuilder.append("search=").append(Uri.encode(topicQuery)); // Use 'search' parameter
        } else {
            // Fallback if no topics selected (though loadNews handles this, this is a safeguard)
            urlBuilder.append("search=general"); // Default to general news if no topics
        }

        // Add language parameter, e.g., English
        urlBuilder.append("&language=en"); // Use 'language' parameter

        // Add sorting, e.g., by relevance or published_at
        urlBuilder.append("&sort_by=relevance"); // Use 'sort_by' parameter (relevance or published_at)

        // --- Add pagination parameters ---
        urlBuilder.append("&limit=").append(articlesPerPage); // Set the limit per page
        urlBuilder.append("&page=").append(currentPage); // Set the current page number
        // ---------------------------------


        // Append the CURRENT API Key (using api_token parameter)
        urlBuilder.append("&api_token=").append(currentApiKey); // Use 'api_token' parameter

        url = urlBuilder.toString();
        // --- End of API Call Logic for thenewsapi.com ---


        Log.d("NewsListFragment", "Fetching URL: " + url + " (Page: " + currentPage + ")");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    isLoading = false; // Reset loading flag

                    try {
                        // --- JSON Parsing Logic for thenewsapi.com ---
                        JSONArray articles = response.getJSONArray("data"); // Articles are under the 'data' key
                        List<NewsItem> fetchedItems = new ArrayList<>();
                        Gson gson = new Gson(); // Gson for parsing individual articles

                        for (int i = 0; i < articles.length(); i++) {
                            JSONObject articleObject = articles.getJSONObject(i);
                            String title = articleObject.optString("title", "No Title");
                            String description = articleObject.optString("snippet", "No Description"); // Description is 'snippet'
                            String articleUrl = articleObject.optString("url");
                            String imageUrl = articleObject.optString("image_url"); // Image URL is 'image_url'
                            String publishedAt = articleObject.optString("published_at"); // Published date is 'published_at'

                            // Skip articles with no title or URL as they are not very useful
                            if (title.equals("No Title") || title.isEmpty() || articleUrl.isEmpty() || title.equals("[Removed]")) {
                                continue;
                            }

                            fetchedItems.add(new NewsItem(title, description, articleUrl, imageUrl, publishedAt));
                        }
                        // --- End of JSON Parsing Logic for thenewsapi.com ---

                        // Append new items to the existing list
                        newsItemList.addAll(fetchedItems);
                        newsAdapter.notifyDataSetChanged(); // Notify adapter of data change

                        // Check if there are more pages based on the number of items fetched
                        if (fetchedItems.size() < articlesPerPage) {
                            hasMorePages = false; // No more pages if fetched items are less than the limit
                            Log.d("NewsListFragment", "No more pages available.");
                        } else {
                            hasMorePages = true; // Assume more pages if fetched items equal the limit
                            currentPage++; // Increment page number for the next request
                        }


                        if (newsItemList.isEmpty()) {
                            textViewNoNews.setVisibility(View.VISIBLE);
                            textViewNoNews.setText(userTopics.isEmpty() ? "Please select topics in your profile." : "No news found for your selected topics.");
                        } else {
                            textViewNoNews.setVisibility(View.GONE);
                        }

                        // Save the combined list to cache
                        saveNewsToCache(newsItemList);

                        // Reset API key index to 0 upon successful fetch (start with the primary key next time)
                        currentApiKeyIndex = 0;


                    } catch (JSONException e) {
                        Log.e("NewsListFragment", "JSON parsing error: " + e.getMessage());
                        Toast.makeText(getContext(), "Error parsing news data.", Toast.LENGTH_SHORT).show();
                        textViewNoNews.setVisibility(View.VISIBLE);
                        textViewNoNews.setText("Error loading news. Please try again.");
                        hasMorePages = false; // Stop pagination on parsing error
                        isLoading = false; // Ensure loading flag is reset
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    isLoading = false; // Reset loading flag
                    hasMorePages = false; // Stop pagination on network/API error for the current attempt

                    Log.e("NewsListFragment", "Volley error: " + error.toString());
                    String errorMessage = "Error fetching news. Please try again.";
                    int statusCode = (error.networkResponse != null) ? error.networkResponse.statusCode : -1;
                    Log.e("NewsListFragment", "Status code: " + statusCode);

                    // --- API Key Switching Logic on Error ---
                    if (statusCode == 401 || statusCode == 403 || statusCode == 429) {
                        // Unauthorized, Forbidden, or Rate Limit - attempt to switch API key
                        Log.w("NewsListFragment", "API key failed (Status: " + statusCode + "). Attempting to switch keys.");
                        currentApiKeyIndex++; // Move to the next API key

                        if (currentApiKeyIndex < apiKeys.size()) {
                            // Retry the request with the next API key
                            Log.i("NewsListFragment", "Retrying with API Key Index: " + currentApiKeyIndex);
                            // We need to retry fetching the SAME page that failed
                            // The pagination logic will handle the next page on successful fetch
                            fetchNewsFromApi(true); // Call fetchNewsFromApi again, indicating it's a retry
                            errorMessage = "Switching to next API key..."; // Inform user
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            // Don't show the "No news found" message immediately, as we are retrying
                            textViewNoNews.setVisibility(View.GONE);
                            progressBar.setVisibility(View.VISIBLE); // Show progress bar again for the retry
                            isLoading = true; // Set loading flag for the retry
                            return; // Exit this error handler to allow the retry
                        } else {
                            // No more API keys to try
                            Log.e("NewsListFragment", "All API keys exhausted.");
                            errorMessage = "Could not fetch news. All available API keys failed.";
                            hasMorePages = false; // Ensure pagination stops
                        }
                    } else {
                        // Handle other network or server errors
                        if (statusCode != -1) {
                            errorMessage = "Server error (" + statusCode + "). Please try again.";
                        } else {
                            errorMessage = "Network error. Please check connection.";
                        }
                        hasMorePages = false; // Ensure pagination stops for other errors
                    }
                    // --- End of API Key Switching Logic ---

                    // Display final error message if no more retries are possible or error is not API key related
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    textViewNoNews.setVisibility(View.VISIBLE);
                    textViewNoNews.setText(errorMessage); // Display error message in UI
                }
        );
        // Add a tag to the request so we can cancel it if the fragment is destroyed
        jsonObjectRequest.setTag("NewsRequest");
        requestQueue.add(jsonObjectRequest);
    }

    // Cache methods
    private String getCacheKey() {
        // Create a unique key based on topics and user ID for the unified feed
        StringBuilder keyBuilder = new StringBuilder(CACHE_KEY_PREFIX);

        if (userTopics != null && !userTopics.isEmpty()) {
            // Sort topics to ensure consistent cache key regardless of topic order
            List<String> sortedTopics = new ArrayList<>(userTopics);
            java.util.Collections.sort(sortedTopics);
            for (String topic : sortedTopics) {
                keyBuilder.append(topic.replaceAll("\\s+", "").toLowerCase()).append("_");
            }
        } else {
            keyBuilder.append("general_"); // Key for default general news if no topics
        }

        // Add user ID to cache key to ensure different users have different caches
        keyBuilder.append("user").append(currentUserId);
        return keyBuilder.toString();
    }


    // Save the entire current list to cache
    private void saveNewsToCache(List<NewsItem> items) {
        if (items == null || items.isEmpty() || getContext() == null) return;
        SharedPreferences cachePrefs = getContext().getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = cachePrefs.edit();
        Gson gson = new Gson();
        String jsonItems = gson.toJson(items);
        String cacheKey = getCacheKey();
        editor.putString(cacheKey, jsonItems);
        editor.putLong(cacheKey + "_timestamp", System.currentTimeMillis());
        editor.apply();
        Log.d("NewsListFragment", "Saved " + items.size() + " items to cache with key: " + cacheKey);
    }

    // Load the entire cached list
    private List<NewsItem> getCachedNews() {
        if (getContext() == null) return null;
        SharedPreferences cachePrefs = getContext().getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE);
        String cacheKey = getCacheKey();
        long cacheTimestamp = cachePrefs.getLong(cacheKey + "_timestamp", 0);

        if (System.currentTimeMillis() - cacheTimestamp > CACHE_DURATION_MS) {
            Log.d("NewsListFragment", "Cache expired for key: " + cacheKey);
            // Optionally clear expired cache
            clearCacheForCurrentConfig();
            return null; // Cache expired
        }

        String jsonItems = cachePrefs.getString(cacheKey, null);
        if (jsonItems == null) {
            Log.d("NewsListFragment", "No cache found for key: " + cacheKey);
            return null;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<NewsItem>>() {}.getType();
        List<NewsItem> cachedList = gson.fromJson(jsonItems, type);
        Log.d("NewsListFragment", "Loaded " + (cachedList != null ? cachedList.size() : 0) + " items from cache with key: " + cacheKey);
        return cachedList;
    }


    // From NewsAdapter.OnBookmarkClickListener
    @Override
    public void onBookmarkClick(NewsItem newsItem, boolean isBookmarked) {
        // You might want to refresh the item view if needed, or an event bus for larger apps
        // For now, the adapter handles the icon change.
        // If this fragment needs to react globally (e.g. update a bookmark count somewhere), handle here.
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh topics in case they changed in profile and user returns
        List<String> latestTopics = databaseHelper.getUserTopics(currentUserId);
        // Check if topics have actually changed before reloading
        if (userTopics == null || !userTopics.equals(latestTopics)) {
            userTopics = latestTopics;
            Log.d("NewsListFragment", "User topics updated onResume. Reloading news.");
            clearCacheForCurrentConfig(); // Clear old cache if topics changed
            loadNews(); // Reload news with new topics
        } else if (newsItemList.isEmpty() && (userTopics != null && !userTopics.isEmpty())) {
            // If list is empty but topics exist (e.g. after initial load failed or coming back to an empty tab)
            Log.d("NewsListFragment", "News list empty onResume with topics. Attempting reload.");
            loadNews();
        }
    }
    private void clearCacheForCurrentConfig() {
        if (getContext() == null) return;
        SharedPreferences cachePrefs = getContext().getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE);
        String cacheKey = getCacheKey();
        cachePrefs.edit().remove(cacheKey).remove(cacheKey + "_timestamp").apply();
        Log.d("NewsListFragment", "Cleared cache for key: " + cacheKey);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel any ongoing Volley requests to prevent memory leaks
        if (requestQueue != null) {
            requestQueue.cancelAll("NewsRequest"); // Cancel requests with the specific tag
        }
    }
}

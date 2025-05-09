package com.android.newsapp; // Or your actual package name

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View; // <-- Add this import
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ArticleDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ARTICLE_URL = "article_url";

    private WebView webView;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail); // Use the new layout

        toolbar = findViewById(R.id.toolbarArticleDetail);
        setSupportActionBar(toolbar);
        // Enable the Up button (back button)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(""); // Set an empty title initially, will update with page title
        }

        webView = findViewById(R.id.webViewArticle);
        progressBar = findViewById(R.id.progressBarWebView);

        // Configure WebView settings (basic settings)
        webView.getSettings().setJavaScriptEnabled(true); // Enable JavaScript if needed by websites
        webView.getSettings().setDomStorageEnabled(true); // Enable DOM storage

        // Set a WebViewClient to handle page navigation within the WebView
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) { // View is used here
                // Load the URL in the WebView itself
                view.loadUrl(url);
                return true; // Indicate that the WebView handled the URL
            }

            @Override
            public void onPageFinished(WebView view, String url) { // View is used here
                super.onPageFinished(view, url);
                // Update toolbar title with the page title once loading is finished
                if (getSupportActionBar() != null && view.getTitle() != null) {
                    getSupportActionBar().setTitle(view.getTitle());
                }
                progressBar.setVisibility(View.GONE); // Hide progress bar
            }
        });

        // Set a WebChromeClient to handle progress updates and page titles
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) { // View is used here
                super.onProgressChanged(view, newProgress);
                // Update progress bar
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) { // View is used here
                super.onReceivedTitle(view, title);
                // Update toolbar title with the page title
                if (getSupportActionBar() != null && title != null) {
                    getSupportActionBar().setTitle(title);
                }
            }
        });


        // Get the article URL from the Intent extras
        String articleUrl = getIntent().getStringExtra(EXTRA_ARTICLE_URL);

        if (articleUrl != null && !articleUrl.isEmpty()) {
            // Load the URL into the WebView
            webView.loadUrl(articleUrl);
        } else {
            // Handle case where no URL was passed (e.g., show an error message)
            Toast.makeText(this, "Could not load article.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
        }
    }

    // Handle the Up button (back button) click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Go back when the Up button is pressed
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Handle back button press within the WebView
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack(); // Go back in WebView history
        } else {
            super.onBackPressed(); // If WebView can't go back, use default back behavior
        }
    }

    // Release WebView resources when the activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}

package com.android.newsapp; // Or your actual package name

import android.content.Context;
import android.content.Intent;
import android.net.Uri; // Still needed if you want to fallback to browser or for other link types
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsItem> newsItemList;
    private Context context;
    private DatabaseHelper databaseHelper;
    private int userId; // Needed for bookmarking

    // Interface for bookmark click listener
    public interface OnBookmarkClickListener {
        void onBookmarkClick(NewsItem newsItem, boolean isBookmarked);
    }
    private OnBookmarkClickListener bookmarkClickListener;


    public NewsAdapter(Context context, List<NewsItem> newsItemList, int userId, OnBookmarkClickListener listener) {
        this.context = context;
        this.newsItemList = newsItemList;
        this.databaseHelper = new DatabaseHelper(context);
        this.userId = userId;
        this.bookmarkClickListener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.news_item_layout, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem newsItem = newsItemList.get(position);

        holder.textViewTitle.setText(newsItem.getTitle());
        holder.textViewDescription.setText(newsItem.getDescription());
        holder.textViewPublishedAt.setText(newsItem.getPublishedAt());

        // Load image using Glide
        Glide.with(context)
                // --- FIX: Use valid drawable resources for placeholder and error ---
                .load(newsItem.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery) // Replace with your actual placeholder drawable
                .error(android.R.drawable.ic_delete) // Replace with your actual error drawable
                .into(holder.imageViewNews);
        // ---------------------------------------------------------------------

        // Check if the article is bookmarked for the current user
        // --- FIX: Use the correct method name isBookmarked ---
        boolean isBookmarked = databaseHelper.isBookmarked(userId, newsItem.getUrl());
        // ----------------------------------------------------
        holder.buttonBookmark.setImageResource(isBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border);


        // --- Modify the item click listener to open in WebView Activity ---
        holder.itemView.setOnClickListener(v -> {
            String articleUrl = newsItem.getUrl();
            if (articleUrl != null && !articleUrl.isEmpty()) {
                Intent intent = new Intent(context, ArticleDetailActivity.class);
                intent.putExtra(ArticleDetailActivity.EXTRA_ARTICLE_URL, articleUrl);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Article URL not available.", Toast.LENGTH_SHORT).show();
            }
        });
        // --- End of item click listener modification ---


        // Bookmark button click listener
        if (userId != -1) { // Only show bookmark option if a user is logged in
            holder.buttonBookmark.setVisibility(View.VISIBLE);
            holder.buttonBookmark.setOnClickListener(v -> {
                // --- FIX: Use the correct method name isBookmarked ---
                boolean currentlyBookmarked = databaseHelper.isBookmarked(userId, newsItem.getUrl());
                // ----------------------------------------------------
                if (currentlyBookmarked) {
                    // Remove bookmark
                    databaseHelper.deleteBookmark(userId, newsItem.getUrl());
                    holder.buttonBookmark.setImageResource(R.drawable.ic_bookmark_border);
                    Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show();
                    if (bookmarkClickListener != null) {
                        bookmarkClickListener.onBookmarkClick(newsItem, false);
                    }
                } else {
                    // Add bookmark
                    // --- FIX: Pass individual parameters to addBookmark ---
                    long result = databaseHelper.addBookmark(
                            userId,
                            newsItem.getUrl(),
                            newsItem.getTitle(),
                            newsItem.getDescription(),
                            newsItem.getImageUrl(),
                            newsItem.getPublishedAt()
                    );
                    // ----------------------------------------------------
                    if (result != -1) {
                        holder.buttonBookmark.setImageResource(R.drawable.ic_bookmark_filled);
                        Toast.makeText(context, "Bookmarked", Toast.LENGTH_SHORT).show();
                        if (bookmarkClickListener != null) {
                            bookmarkClickListener.onBookmarkClick(newsItem, true);
                        }
                    } else {
                        Toast.makeText(context, "Failed to bookmark", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            holder.buttonBookmark.setVisibility(View.GONE); // Hide if no user logged in (shouldn't happen in main feed)
        }
    }

    @Override
    public int getItemCount() {
        return newsItemList == null ? 0 : newsItemList.size();
    }

    public void updateNews(List<NewsItem> newNewsItems) {
        this.newsItemList.clear();
        if (newNewsItems != null) {
            this.newsItemList.addAll(newNewsItems);
        }
        notifyDataSetChanged(); // Consider using DiffUtil for better performance
    }


    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewNews;
        TextView textViewTitle, textViewDescription, textViewPublishedAt;
        ImageButton buttonBookmark;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewNews = itemView.findViewById(R.id.imageViewNews);
            textViewTitle = itemView.findViewById(R.id.textViewNewsTitle);
            textViewDescription = itemView.findViewById(R.id.textViewNewsDescription);
            textViewPublishedAt = itemView.findViewById(R.id.textViewNewsPublishedAt);
            buttonBookmark = itemView.findViewById(R.id.buttonBookmark);
        }
    }
}

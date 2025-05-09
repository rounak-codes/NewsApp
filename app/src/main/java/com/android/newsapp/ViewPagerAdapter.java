package com.android.newsapp; // Or your actual package name

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    // We now have only one fragment for the unified feed
    private static final int NUM_TABS = 1;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Always return a single NewsListFragment instance
        // The NewsListFragment will handle fetching based on user topics
        return NewsListFragment.newInstance(null); // Pass null as countryCode is no longer used for filtering
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
}

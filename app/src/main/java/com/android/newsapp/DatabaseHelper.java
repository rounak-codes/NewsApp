package com.android.newsapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "NewsAppDB";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_TOPICS = "user_topics";
    private static final String TABLE_BOOKMARKS = "bookmarks";

    // Common column names
    private static final String KEY_ID = "id";

    // Users table columns
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    // User Topics table columns
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_TOPIC = "topic";

    // Bookmarks table columns
    private static final String KEY_NEWS_URL = "news_url";
    private static final String KEY_NEWS_TITLE = "news_title";
    private static final String KEY_NEWS_DESCRIPTION = "news_description";
    private static final String KEY_NEWS_IMAGE_URL = "news_image_url";
    private static final String KEY_NEWS_PUBLISHED_AT = "published_at";

    // Create table statements
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_USERNAME + " TEXT UNIQUE NOT NULL,"
                    + KEY_PASSWORD + " TEXT NOT NULL" + ")";

    private static final String CREATE_TABLE_TOPICS =
            "CREATE TABLE " + TABLE_TOPICS + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_USER_ID + " INTEGER NOT NULL,"
                    + KEY_TOPIC + " TEXT NOT NULL,"
                    + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + "))";

    private static final String CREATE_TABLE_BOOKMARKS =
            "CREATE TABLE " + TABLE_BOOKMARKS + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_USER_ID + " INTEGER NOT NULL,"
                    + KEY_NEWS_URL + " TEXT UNIQUE NOT NULL,"
                    + KEY_NEWS_TITLE + " TEXT,"
                    + KEY_NEWS_DESCRIPTION + " TEXT,"
                    + KEY_NEWS_IMAGE_URL + " TEXT,"
                    + KEY_NEWS_PUBLISHED_AT + " TEXT,"
                    + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + "))";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_TOPICS);
        db.execSQL(CREATE_TABLE_BOOKMARKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if they exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOPICS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);

        // Create tables again
        onCreate(db);
    }

    // --- User Management ---

    public long addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password); // In a real app, hash this password!
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {KEY_ID};
        String selection = KEY_USERNAME + "=? AND " + KEY_PASSWORD + "=?";
        String[] selectionArgs = {username, password}; // In a real app, compare with hashed password!
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count > 0;
    }

    public int getUserIdByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_ID}, KEY_USERNAME + "=?", new String[]{username}, null, null, null);
        int userId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
            cursor.close();
        }
        db.close();
        return userId;
    }

    // --- User Topics ---

    public void addUserTopic(int userId, String topic) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_ID, userId);
        values.put(KEY_TOPIC, topic);
        db.insert(TABLE_TOPICS, null, values);
        db.close();
    }

    public List<String> getUserTopics(int userId) {
        List<String> topics = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TOPICS, new String[]{KEY_TOPIC}, KEY_USER_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                topics.add(cursor.getString(cursor.getColumnIndexOrThrow(KEY_TOPIC)));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return topics;
    }

    public void updateUserTopics(int userId, List<String> newTopics) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TOPICS, KEY_USER_ID + "=?", new String[]{String.valueOf(userId)});
        for (String topic : newTopics) {
            addUserTopic(userId, topic);
        }
        db.close();
    }

    // --- Bookmarks ---

    public long addBookmark(int userId, String url, String title, String description, String imageUrl, String publishedAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_ID, userId);
        values.put(KEY_NEWS_URL, url);
        values.put(KEY_NEWS_TITLE, title);
        values.put(KEY_NEWS_DESCRIPTION, description);
        values.put(KEY_NEWS_IMAGE_URL, imageUrl);
        values.put(KEY_NEWS_PUBLISHED_AT, publishedAt);
        long id = db.insert(TABLE_BOOKMARKS, null, values);
        db.close();
        return id;
    }

    public boolean isBookmarked(int userId, String url) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {KEY_ID};
        String selection = KEY_USER_ID + "=? AND " + KEY_NEWS_URL + "=?";
        String[] selectionArgs = {String.valueOf(userId), url};
        Cursor cursor = db.query(TABLE_BOOKMARKS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count > 0;
    }

    public List<NewsItem> getAllBookmarks(int userId) {
        List<NewsItem> bookmarks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKMARKS, new String[]{KEY_NEWS_URL, KEY_NEWS_TITLE, KEY_NEWS_DESCRIPTION, KEY_NEWS_IMAGE_URL, KEY_NEWS_PUBLISHED_AT},
                KEY_USER_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                NewsItem newsItem = new NewsItem(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_NEWS_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_NEWS_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_NEWS_URL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_NEWS_IMAGE_URL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_NEWS_PUBLISHED_AT))
                );
                bookmarks.add(newsItem);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return bookmarks;
    }

    public int deleteBookmark(int userId, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_BOOKMARKS, KEY_USER_ID + "=? AND " + KEY_NEWS_URL + "=?", new String[]{String.valueOf(userId), url});
        db.close();
        return rowsDeleted;
    }
}
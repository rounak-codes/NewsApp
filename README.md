# NewsApp

A simple Android news application that allows users to register, select their favorite topics, view a personalized news feed, bookmark articles, and search for news.

## Features

* **User Authentication:** Register new users and log in existing ones.
* **Topic Selection:** Users can choose their preferred news topics.
* **Personalized News Feed:** Displays news articles based on the user's selected topics.
* **Pagination & Infinite Scrolling:** Load more articles as the user scrolls down the news feed.
* **Multi-API Key Handling:** Cycles through multiple API keys to mitigate rate limits (specifically for the thenewsapi.com free tier).
* **Article Bookmarking:** Users can save articles to their bookmarks for later reading.
* **Search Functionality:** Search for news articles based on keywords.
* **In-app Article Viewing:** Articles open within the app using a WebView.
* **Basic UI Theming:** Implemented a dark bluish theme for various screens.

## Technologies Used

* **Android SDK:** Building the native Android application.
* **Java:** The primary programming language.
* **SQLite:** Local database for managing user accounts, topics, and bookmarks.
* **Volley:** Android library for efficient networking operations (fetching news data).
* **Gson:** Java library for converting JSON responses to Java objects (NewsItem).
* **Glide:** Image loading library for displaying article images.
* **AndroidX Libraries:** Modern Android development libraries.
* **Material Components:** UI components following Material Design guidelines (e.g., Chips, CardView, Toolbar).

## Setup

To set up and run this project locally:

1.  **Clone the repository:**
    ```bash
    git clone <repository_url>
    ```
    (Replace `<repository_url>` with the actual URL of your Git repository)

2.  **Open in Android Studio:** Open the cloned project in Android Studio.

3.  **Add API Keys:** Obtain API keys from [thenewsapi.com](https://thenewsapi.com/). You will need at least two keys for the multi-key handling feature to be effective with the free tier limits. Add your API keys to the `gradle.properties` file in the root of your project:

    ```properties
    NEWS_API_KEY_1="YOUR_THENEWSAPI_KEY_1"
    NEWS_API_KEY_2="YOUR_THENEWSAPI_KEY_2"
    # Add more keys if you have them, e.g.:
    # NEWS_API_KEY_3="YOUR_THENEWSAPI_KEY_3"
    ```
    **Replace the placeholder values with your actual API keys.**

4.  **Sync and Build:** Sync your project with Gradle files (`File > Sync Project with Gradle Files`) and then build the project (`Build > Rebuild Project`).

5.  **Run the App:** Run the application on an Android emulator or a physical device.

## Database Schema

The application uses a local SQLite database with the following tables:

* `users`: Stores user registration information (username, password).
* `user_topics`: Links users to their selected topics.
* `bookmarks`: Stores bookmarked news articles for each user.

The schema is defined in `DatabaseHelper.java`.

## API Usage

This application uses the [thenewsapi.com](https://thenewsapi.com/) API to fetch news articles. The free tier has limitations, which are mitigated by:

* Fetching articles in small pages (currently set to 3 per request to align with the free tier limit).
* Implementing a multi-API key switching mechanism that attempts the next available key upon encountering rate limit or authentication errors (401, 403, 429 status codes).

## Contributing

(If you plan to accept contributions, add guidelines here.)

## License

(Specify the license under which your project is released.)

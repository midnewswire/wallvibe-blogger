# wallvibe-blogger

First-party Android client for Blogger's public classic JSON feed
(`https://<blog>/feeds/posts/default?alt=json`), which is unauthenticated and public for any
public Blogger blog. No API key, no access key, no third-party SDK - it replaces a
closed-source vendor AAR (`dreamspace-blogger-sdk`) that this project used to depend on, and
its licensing/access-key scheme entirely.

## Status

Written against Blogger's classic GData/JSON feed format, but **not built or run** in this
repo's environment (no Android SDK / network access available there). Build it in Android
Studio and smoke-test against a real public Blogger blog before shipping.

## Using it in an app

As a module in the same Gradle build (this is how the WallVibe app consumes it):

```groovy
// settings.gradle
include ':wallvibe-blogger'

// app/build.gradle
dependencies {
    implementation project(':wallvibe-blogger')
}
```

As a standalone repo via JitPack, once pushed to its own GitHub repository:

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

// app/build.gradle
dependencies {
    implementation 'com.github.midnewswire:wallvibe-blogger:<tag-or-commit>'
}
```

## Usage

```java
BloggerAPI api = new BloggerAPI();

api.getCategories("my-blog.blogspot.com", new RequestListener<List<String>>() {
    @Override public void onSuccess(List<String> categories) { /* ... */ }
    @Override public void onFailed(String message) { /* ... */ }
});

ParamList param = new ParamList();
param.url = "my-blog.blogspot.com";
param.page = "1";       // 1-based
param.count = "20";     // page size
param.category = null;  // optional label filter
param.keyword = null;   // optional full-text search

api.getPosts(param, new RequestListener<RespPosts>() {
    @Override public void onSuccess(RespPosts resp) { /* resp.list: List<Listing> */ }
    @Override public void onFailed(String message) { /* ... */ }
});
```

## How it works

Blogger's classic feed supports numeric pagination via `start-index`/`max-results`, label
filtering via a `/-/<label>` path segment, and full-text search via `q=` - this client builds
those requests directly with OkHttp and parses the JSON with Gson. The blog's full label list
comes from the feed's top-level `feed.category[]` field (requested with `max-results=0`).

## License

Your own code - choose whatever license you want for the repo. It doesn't carry over any
license terms from the removed vendor SDK; it was written from scratch against Blogger's
public feed format.

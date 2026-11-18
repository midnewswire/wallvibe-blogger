package com.wallvibe.blogger;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.wallvibe.blogger.listener.RequestListener;
import com.wallvibe.blogger.model.Listing;
import com.wallvibe.blogger.model.ParamList;
import com.wallvibe.blogger.model.RespPosts;
import com.wallvibe.blogger.model.internal.FeedResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * First-party client for Blogger's public classic JSON feed
 * ({@code https://<blog>/feeds/posts/default?alt=json}), which is public and unauthenticated
 * for any public Blogger blog - no API key, no access key, no third-party SDK.
 * <p>
 * This replaces the closed-source {@code dreamspace-blogger-sdk} AAR and its access-key
 * licensing scheme. The public surface (method names, {@link ParamList}, {@link RespPosts},
 * {@link Listing}, {@link RequestListener}) intentionally mirrors the old SDK so the rest of
 * the app needed no changes beyond the import.
 * <p>
 * Not built/run in this environment - smoke-test against a real Blogger blog before shipping.
 */
public class BloggerAPI {

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * @param unusedAccessKeyOrAnything kept only so old call sites like
     *                                  {@code new BloggerAPI(AppConfig.general.access_key)}
     *                                  still compile unchanged; the value is ignored, there is
     *                                  no access key any more. Prefer {@link #BloggerAPI()}.
     */
    @Deprecated
    public BloggerAPI(String unusedAccessKeyOrAnything) {
        this();
    }

    public BloggerAPI() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /** All labels used anywhere on the blog. */
    public void getCategories(String bloggerUrl, RequestListener<List<String>> listener) {
        HttpUrl url = feedUrlBuilder(bloggerUrl, null)
                .addQueryParameter("max-results", "0")
                .build();

        enqueue(url, listener, body -> {
            FeedResponse resp = gson.fromJson(body, FeedResponse.class);
            List<String> categories = new ArrayList<>();
            if (resp != null && resp.feed != null && resp.feed.category != null) {
                for (FeedResponse.Term t : resp.feed.category) {
                    if (t.term != null && !categories.contains(t.term)) categories.add(t.term);
                }
            }
            return categories;
        });
    }

    /** One page of posts, optionally filtered by label ({@link ParamList#category}) or
     *  full-text query ({@link ParamList#keyword}). */
    public void getPosts(ParamList param, RequestListener<RespPosts> listener) {
        int count = parseIntOr(param.count, 20);
        int page = parseIntOr(param.page, 1);
        int startIndex = Math.max(1, (page - 1) * count + 1);

        HttpUrl.Builder builder = feedUrlBuilder(param.url, param.category)
                .addQueryParameter("max-results", String.valueOf(count))
                .addQueryParameter("start-index", String.valueOf(startIndex));
        if (param.keyword != null && !param.keyword.trim().isEmpty()) {
            builder.addQueryParameter("q", param.keyword.trim());
        }

        enqueue(builder.build(), listener, body -> {
            FeedResponse resp = gson.fromJson(body, FeedResponse.class);
            List<Listing> items = new ArrayList<>();
            if (resp != null && resp.feed != null && resp.feed.entry != null) {
                for (FeedResponse.Entry e : resp.feed.entry) {
                    items.add(toListing(e));
                }
            }
            return new RespPosts(items);
        });
    }

    private HttpUrl.Builder feedUrlBuilder(String bloggerUrl, String category) {
        HttpUrl.Builder builder = new HttpUrl.Builder()
                .scheme("https")
                .host(normalizeHost(bloggerUrl))
                .addPathSegment("feeds")
                .addPathSegment("posts")
                .addPathSegment("default");
        if (category != null && !category.trim().isEmpty()) {
            builder.addPathSegment("-");
            builder.addPathSegment(category.trim());
        }
        builder.addQueryParameter("alt", "json");
        return builder;
    }

    private static String normalizeHost(String url) {
        if (url == null) return "";
        String host = url.trim();
        host = host.replaceFirst("^https?://", "");
        host = host.replaceAll("/+$", "");
        return host;
    }

    private static int parseIntOr(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Listing toListing(FeedResponse.Entry e) {
        Listing l = new Listing();
        l.id = extractPostId(text(e.id));
        l.title = text(e.title);
        l.content = text(e.content);
        l.published = text(e.published);
        l.updated = text(e.updated);
        l.link = extractAlternateLink(e.link);
        l.category = new ArrayList<>();
        if (e.category != null) {
            for (FeedResponse.Term t : e.category) {
                if (t.term != null) l.category.add(t.term);
            }
        }
        return l;
    }

    /** GData ids look like "tag:blogger.com,1999:blog-XXXX.post-YYYY"; fall back to the whole
     *  string if that shape doesn't match - it only needs to be a stable unique value. */
    private static String extractPostId(String gdataId) {
        if (gdataId == null) return "";
        int idx = gdataId.lastIndexOf(".post-");
        return idx >= 0 ? gdataId.substring(idx + 6) : gdataId;
    }

    private static String extractAlternateLink(List<FeedResponse.Link> links) {
        if (links == null || links.isEmpty()) return null;
        for (FeedResponse.Link link : links) {
            if ("alternate".equals(link.rel)) return link.href;
        }
        return links.get(0).href;
    }

    private static String text(FeedResponse.Text t) {
        return t == null ? null : t.$t;
    }

    private interface Mapper<T> {
        T map(String body) throws Exception;
    }

    private <T> void enqueue(HttpUrl url, RequestListener<T> listener, Mapper<T> mapper) {
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postFailed(listener, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        postFailed(listener, "HTTP " + response.code());
                        return;
                    }
                    String body = response.body().string();
                    T result = mapper.map(body);
                    postSuccess(listener, result);
                } catch (Exception e) {
                    postFailed(listener, e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    private <T> void postSuccess(RequestListener<T> listener, T result) {
        mainHandler.post(() -> listener.onSuccess(result));
    }

    private <T> void postFailed(RequestListener<T> listener, String message) {
        mainHandler.post(() -> listener.onFailed(message));
    }
}

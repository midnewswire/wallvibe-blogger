package com.wallvibe.blogger.model;

/**
 * Request parameters for {@link com.wallvibe.blogger.BloggerAPI#getPosts}.
 * Field names/types match the old vendor SDK's ParamList so call sites needed no changes.
 */
public class ParamList {
    /** Bare blog host, e.g. "my-blog.blogspot.com" (AppConfig.general.blogger_url). */
    public String url;
    /** 1-based page number, as a string (e.g. "1", "2", ...). */
    public String page;
    /** Page size, as a string (e.g. "20"). */
    public String count;
    /** Optional label/category to filter by. Null/empty = no filter. */
    public String category;
    /** Optional free-text search query. Null/empty = no search. */
    public String keyword;
}

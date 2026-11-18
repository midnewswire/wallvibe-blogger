package com.wallvibe.blogger.model;

import java.util.List;

/** Result of {@link com.wallvibe.blogger.BloggerAPI#getPosts}. */
public class RespPosts {
    public List<Listing> list;

    public RespPosts(List<Listing> list) {
        this.list = list;
    }
}

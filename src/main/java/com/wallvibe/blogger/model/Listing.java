package com.wallvibe.blogger.model;

import java.util.List;

/**
 * One blog post, normalized from Blogger's classic JSON feed entry.
 * Field names match the old vendor SDK's Listing model, which
 * {@code com.wallvibe.app.utils.Tools#parseListingToWallpaper} reads directly.
 */
public class Listing {
    public String id;
    public String type = "post";
    public String title;
    public String content;
    public String link;
    public String published;
    public String updated;
    public List<String> category;
}

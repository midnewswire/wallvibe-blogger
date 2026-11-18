package com.wallvibe.blogger.model.internal;

import java.util.List;

/**
 * Raw shape of Blogger's classic JSON feed
 * ({@code https://<blog>/feeds/posts/default?alt=json}), as Gson deserializes it.
 * Not part of the public API of this module - see {@link com.wallvibe.blogger.BloggerAPI}.
 */
public class FeedResponse {
    public Feed feed;

    public static class Feed {
        /** Every label used anywhere in the blog (present even when entry list is paged/empty). */
        public List<Term> category;
        /** Posts on this page, absent when there are none. */
        public List<Entry> entry;
    }

    public static class Entry {
        public Text id;
        public Text published;
        public Text updated;
        public Text title;
        public Text content;
        public List<Term> category;
        public List<Link> link;
    }

    public static class Text {
        public String $t;
    }

    public static class Term {
        public String term;
    }

    public static class Link {
        public String rel;
        public String type;
        public String href;
    }
}

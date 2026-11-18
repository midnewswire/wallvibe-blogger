package com.wallvibe.blogger.listener;

/**
 * Callback for a {@link com.wallvibe.blogger.BloggerAPI} request.
 * <p>
 * This is a plain base class (not an interface) so existing call sites that do
 * {@code super.onSuccess(resp)} / {@code super.onFailed(msg)} before their own logic keep
 * working unchanged - the base implementations here are no-ops.
 */
public abstract class RequestListener<T> {

    public void onSuccess(T response) {
        // no-op base implementation, override to handle the result
    }

    public void onFailed(String message) {
        // no-op base implementation, override to handle the error
    }
}

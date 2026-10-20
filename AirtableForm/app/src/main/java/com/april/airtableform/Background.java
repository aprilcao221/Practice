package com.april.airtableform;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Runs network calls off the main thread and delivers the result back on it. */
final class Background {

    interface Callback<T> {
        void onResult(T result, Exception error);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Background() {}

    static <T> void run(Callable<T> work, Callback<T> callback) {
        EXECUTOR.execute(() -> {
            T result = null;
            Exception error = null;
            try {
                result = work.call();
            } catch (Exception e) {
                error = e;
            }
            final T r = result;
            final Exception err = error;
            MAIN.post(() -> callback.onResult(r, err));
        });
    }

    /** A message fit to show the user. */
    static String describe(Exception e) {
        if (e instanceof AirtableApi.ApiException) {
            return e.getMessage();
        }
        if (e instanceof java.io.IOException) {
            return "Network problem: " + e.getMessage();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class IOUtils {

    private IOUtils() {}

    /**
     * A generic solution to read from an input stream in UTF-8. This function will read from the stream until it
     * is finished and close the stream - this is necessary to close the wrapping resources.
     *
     * For a higher-level method, see {@link #readStringFromFile(File)}.
     *
     * Since this is generic, it may not be the most performant for your use case.
     *
     * @param bufferSize Size of the underlying buffer for read optimizations - must be > 0.
     */
    public static String readStringFromInputStreamAndCloseStream(final InputStream inputStream, final int bufferSize)
            throws IOException {
        if (bufferSize <= 0) {
            // Safe close: it's more important to alert the programmer of
            // their error than to let them catch and continue on their way.
            safeStreamClose(inputStream);
            throw new IllegalArgumentException("Expected buffer size larger than 0. Got: " + bufferSize);
        }

        final StringBuilder stringBuilder = new StringBuilder(bufferSize);
        final InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        try {
            int charsRead;
            final char[] buffer = new char[bufferSize];
            while ((charsRead = reader.read(buffer, 0, bufferSize)) != -1) {
                stringBuilder.append(buffer, 0, charsRead);
            }
        } finally {
            reader.close();
        }
        return stringBuilder.toString();
    }


    // from m-c.
    public static void safeStreamClose(Closeable stream) {
        try {
            if (stream != null)
                stream.close();
        } catch (IOException e) { }
    }

    /**
     * Blocks until the given asynchronous call completes or the call times out.
     *
     * Callers should initiate their async request in their implementation of
     * {@link AsyncCall#initAsyncCall(OnAsyncCallComplete)} and, when the call completes, call the appropriate
     * method of {@link OnAsyncCallComplete}: {@link OnAsyncCallComplete#onException(Exception)} when an Exception
     * is thrown during the async task and {@link OnAsyncCallComplete#onComplete(Object)} for other completions.
     * For example:
     *
     * <pre>
     *     final Integer result;
     *     try {
     *         result = makeSync(5000, new AsyncCall<Integer>() {
     *             @Override
     *             public void initAsyncCall(OnAsyncCallComplete<Integer> onComplete) {
     *                 makeNetworkRequest(new Callback() {
     *                     @Override public void onSuccess(Integer result) { onComplete.onComplete(result); }
     *                     @Override public void onError(Exception e) { onComplete.onException(e); }
     *                 }
     *             }
     *         };
     *     } catch (ExecutionException e) {
     *         // Do something with async exception...
     *         return;
     *     } catch (TimeoutException e) {
     *         // Do something with time-out...
     *         return;
     *     }
     *
     *     // Do something with result...
     * </pre>
     *
     * Exceptions that occur during the async task that are caught and passed back via {@code onException} will
     * throw an {@code ExecutionException} while unchecked Exceptions during the async task are subject to the
     * mechanism that the async call runs on (e.g. they can be ignored if they're in an Executor). Unchecked
     * Exceptions that occur on the calling thread during {@code initAsyncCall} will be thrown from
     * {@code initAsyncCall}.
     *
     * @param timeoutMillis The number of milliseconds until this call times out.
     * @param asyncCall An object whose method will begin the async call.
     * @param <T> Return type of the async call.
     * @return The result of the asynchronous call.
     *
     * @throws ExecutionException thrown for any exception returned by the AsyncTask.
     * @throws TimeoutException when the async call times out.
     */
    public static <T> T makeSync(final long timeoutMillis, final AsyncCall<T> asyncCall) throws ExecutionException, TimeoutException {
        final CountDownLatch makeSyncLatch = new CountDownLatch(1);
        final AsyncReturnValueContainer<T> returnValueContainer = new AsyncReturnValueContainer<T>();

        asyncCall.initAsyncCall(new OnAsyncCallComplete<T>() {
            @Override
            public void onComplete(final T returnValue) {
                returnValueContainer.returnValue = returnValue;
                makeSyncLatch.countDown();
            }

            @Override
            public void onException(final Exception e) {
                returnValueContainer.exception = e;
                makeSyncLatch.countDown();
            }
        });

        try {
            final boolean didCountReachZero = makeSyncLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!didCountReachZero) {
                throw new TimeoutException("makeSync: async operation timed out.");
            }
        } catch (final InterruptedException e) {
            throw new ExecutionException("makeSync: thread interrupted while waiting for async call to complete.", e);
        }

        if (returnValueContainer.exception != null) {
            throw new ExecutionException("makeSync: Exception thrown during async task.", returnValueContainer.exception);
        }
        return returnValueContainer.returnValue;
    }

    private static class AsyncReturnValueContainer<T> {
        private Exception exception;
        private T returnValue;
    }

    /**
     * Allows a method who has an instance of an objecting implementing this interface to begin an async call
     * and be notified upon its completion. See {@link #makeSync(long, AsyncCall)}.
     */
    public interface AsyncCall<T> {
        /** Begins an async call, calling a method of {@link OnAsyncCallComplete} when the call terminates. */
        void initAsyncCall(OnAsyncCallComplete<T> onComplete);
     }


    /*
     * A class that implements this interface can be notified when an async call is complete.
     * See {@link #makeSync(long, AsyncCall)}}.
     */
    public interface OnAsyncCallComplete<T> {
        /** Called when the async call completes by throwing an Exception. */
        void onException(Exception e);
        /** Called when the async call completes. */
        void onComplete(T returnValue);
    }
}

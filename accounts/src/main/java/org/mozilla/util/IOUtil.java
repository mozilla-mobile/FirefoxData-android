/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IOUtil {

    private IOUtil() {}

    // from m-c.
    public static void safeStreamClose(Closeable stream) {
        try {
            if (stream != null)
                stream.close();
        } catch (IOException e) { }
    }

    // TODO: explain how this should be used.
    /**
     * Blocks until the given asynchronous call completes.
     *
     * @param timeoutMillis The number of milliseconds until this call times out.
     * @param asyncCall An object whose method will begin the async call.
     * @param <T> Return type of the async call.
     * @return The result of the asynchronous call.
     * @throws Exception An exception thrown during the async task.
     */
    public static <T> T makeSync(final long timeoutMillis, final AsyncCall<T> asyncCall) throws Exception { // TODO: ExecutionException.
        final CountDownLatch makeSyncLatch = new CountDownLatch(1);
        final AsyncReturnValueContainer<T> returnValueContainer = new AsyncReturnValueContainer<T>();

        asyncCall.initAsyncCall(new OnAsyncCallComplete<T>() {
            @Override
            public void onSuccess(T returnValue) {
                returnValueContainer.returnValue = returnValue;
                makeSyncLatch.countDown();
            }

            @Override
            public void onError(final Exception e) {
                returnValueContainer.exception = e;
                makeSyncLatch.countDown();
            }
        });

        makeSyncLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        if (returnValueContainer.exception != null) { throw returnValueContainer.exception; }
        return returnValueContainer.returnValue;
    }

    private static class AsyncReturnValueContainer<T> {
        private Exception exception;
        private T returnValue;
    }

    // TODO: docs.
    public interface AsyncCall<T> {
        void initAsyncCall(OnAsyncCallComplete<T> onComplete);
    }

    // todo: DOCS.
    public interface OnAsyncCallComplete<T> {
        void onError(Exception e);
        void onSuccess(T returnValue);
    }
}

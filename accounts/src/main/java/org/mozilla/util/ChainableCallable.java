/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * A Callable that can be chained to wait for other Callables to complete before running.
 * Each Callable will take as input the return value from the previously finished Callable. If
 * an Exception is thrown at any time, it will propagate to the last Callable in the chain.
 * For the last Callable, `get()` will either return the final value or it will throw the
 * chained Exception.
 *
 * This class could be replaced with Promises, once they're available (e.g. Java 8's
 * CompletableFuture).
 *
 * Here is the intended use case:
 * <pre>
 *   Executor executor = Executors.newSingleThreadedExecutor();
 *
 *   List<ChainableCallable<V>> callables = ...;
 *   V result = ... // some value.
 *   for (ChainableCallable callable : callables) {
 *       callable.setFutureDependency(result);
 *       result = executor.submit(callable);
 *   }
 *
 *   result.get(); // The final result, or an Exception which can cascade from a previous Callable.
 * </pre>
 *
 * @param <I> Type of the argument to this Callable.
 * @param <O> Return type of this Callable.
 */
public abstract class ChainableCallable<I, O> implements Callable {
    private Future<I> futureDependency;
    public final void setFutureDependency(final Future<I> futureDependency) { this.futureDependency = futureDependency; }

    @Override
    public final O call() throws Exception {
        return call(futureDependency.get()); // thrown exceptions are expected to cascade through all ChainableCallable.
    }

    /**
     * Computes a result, or throws an Exception if it is unable to do so.
     * @param value The return value from the previous Callable in the chain.
     */
    public abstract O call(final I value) throws Exception;

    /**
     * A {@link ChainableCallable} for async calls: it will block until the async call completes, allowing async calls
     * to occur in an executor.
     *
     * @param <I> Type of the argument to this Callable.
     * @param <O> Return type of this Callable.
     */
    public static abstract class AsyncChainableCallable<I, O> extends ChainableCallable<I, O> {
        private final long timeoutMillis;

        protected AsyncChainableCallable(final long timeoutMillis) { this.timeoutMillis = timeoutMillis; }

        /**
         * Begins the async call and calls a function of `onComplete`
         *
         * @param input The return value from the previous Callable in the chain.
         * @param onComplete The callback that should be called when the async call completes.
         */
        public abstract void initAsyncCall(final I input, final IOUtil.OnAsyncCallComplete<O> onComplete);

        @Override
        public O call(final I input) throws ExecutionException, TimeoutException {
            return IOUtil.makeSync(timeoutMillis, new IOUtil.AsyncCall<O>() {
                @Override
                public void initAsyncCall(final IOUtil.OnAsyncCallComplete<O> onComplete) {
                    AsyncChainableCallable.this.initAsyncCall(input, onComplete);
                }
            });
        }
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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
 *   The result from the final callable can be accessed via the returned Future, which will
 *   return the result or throw a cascaded Exception. If the final callable uses a callback,
 *   {@link ChainableCallableWithCallback} is recommended instead.
 * </pre>
 */
public abstract class ChainableCallable<V> implements Callable {
    private Future<V> futureDependency;
    public final void setFutureDependency(final Future<V> futureDependency) { this.futureDependency = futureDependency; }

    @Override
    public final V call() throws Exception {
        return call(futureDependency.get()); // thrown exceptions are expected to cascade through all ChainableCallable.
    }

    /**
     * Computes a result, or throws an Exception if it is unable to do so.
     * @param value The return value from the previous Callable in the chain.
     */
    public abstract V call(final V value) throws Exception;

    /**
     * A {@link ChainableCallable} that does not allow the user to accidentally suppress errors
     * by throwing during `call`: any thrown exceptions will call the error handler of the
     * given callback.
     */
    public static abstract class ChainableCallableWithCallback<V> extends ChainableCallable<V> {
        private ChainableCallableCallback callback;
        public ChainableCallableWithCallback(final ChainableCallableCallback callback) { this.callback = callback; }

        // TODO: If .get() throws, this method won't be called. :(
        @Override
        public final V call(final V value) throws Exception {
            try {
                callWithCallback(value);
            } catch (final Exception e) {
                callback.onError(e);
            }
            return null; // This is expected to end in a callback so we don't care for result.
        }

        /**
         * Computes a result or throws an Exception if it is unable to do so. All Exceptions will
         * call the `onError` handler of the given callback.
         */
        public abstract void callWithCallback(final V value) throws Exception;
    }

    public interface ChainableCallableCallback { void onError(Exception e); }
}

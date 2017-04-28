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
 *   // Maybe in an async callback...
 *   V finalResult = result.get(); // or it will throw an Exception.
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
}

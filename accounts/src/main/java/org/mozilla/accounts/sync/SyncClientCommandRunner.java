/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import android.support.annotation.NonNull;
import org.mozilla.accounts.sync.commands.AdvanceToMarriagePreCommand;
import org.mozilla.accounts.sync.commands.GetCryptoKeysPreCommand;
import org.mozilla.accounts.sync.commands.GetSyncTokenPreCommand;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientAsyncPreCommand;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientCollectionCommand;
import org.mozilla.util.ChainableCallable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An encapsulation to queue & run the given sync commands but first ensuring their prerequisites are met
 * (e.g. we have the keys to decrypt the Sync download).
 *
 * ---
 * This is an alternative to having a large chain of callbacks and provides some benefits:
 * - It's much simpler to add/remove precommands because they're no longer tightly coupled.
 * - Each step is better encapsulated
 * - It's clearer which thread everything runs on.
 * - Errors are easily propagated via the {@link org.mozilla.util.ChainableCallable}.
 *
 * The downside is that it's easy to lose Exceptions: if they're thrown in an Executor,
 * they need to be manually accessed to be thrown.
 */
class SyncClientCommandRunner {

    // This is intentionally a single thread: it ensures all commands run serially.
    // If you try to increase the thread count, note that the pre-commands are written
    // assuming they will never run concurrently.
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

    private List<? extends SyncClientAsyncPreCommand> getPreCommands() {
        return Collections.unmodifiableList(Arrays.asList(
                // The order matters: these commands may rely on results from the previous operations.
                new AdvanceToMarriagePreCommand(),
                new GetSyncTokenPreCommand(),
                new GetCryptoKeysPreCommand()
        ));
    }

    /**
     * Queues & runs the given command, running any prerequisite commands first. The outputs
     * from these pre-commands (e.g. sync tokens) will be passed in a Future to the given command.
     * If there are any errors in these pre-commands, they will cascade and the Future will
     * throw instead.
     *
     * This function is thread-safe: it's synchronized to ensure items are added to the queue
     * serially.
     */
    protected synchronized void queueAndRunCommand(final SyncClientCollectionCommand command,
            final FirefoxAccountSyncConfig initialSyncConfig) {
        final List<? extends ChainableCallable<FirefoxAccountSyncConfig>> preCommands = getPreCommands();

        Future<FirefoxAccountSyncConfig> result = new ReturnInputFuture(initialSyncConfig); // hack: set initial input.
        for (final ChainableCallable<FirefoxAccountSyncConfig> preCommand : preCommands) {
            preCommand.setFutureDependency(result);
            result = commandExecutor.submit(preCommand);
        }

        // It'd be great to add this to preCommands and do it all in a loop but
        // I can't get the types right.
        command.setFutureDependency(result);
        commandExecutor.submit(command);
    }

    /** A Future that immediately returns the given value (the future is now!). */
    private static class ReturnInputFuture implements Future<FirefoxAccountSyncConfig> {
        private final FirefoxAccountSyncConfig input;
        private ReturnInputFuture(final FirefoxAccountSyncConfig input) { this.input = input; }
        @Override public FirefoxAccountSyncConfig get() throws InterruptedException, ExecutionException { return input; }
        @Override public FirefoxAccountSyncConfig get(final long timeout, @NonNull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { return get(); }

        @Override public boolean cancel(final boolean mayInterruptIfRunning) { return false; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return true; }
    }
}

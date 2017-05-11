/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;


import android.content.Context;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.sync.sync.commands.GetSyncBookmarksCommand;
import org.mozilla.sync.sync.commands.GetSyncHistoryCommand;
import org.mozilla.sync.sync.commands.GetSyncPasswordsCommand;
import org.mozilla.sync.sync.commands.SyncCollectionCallback;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TODO:
 */
class FirefoxSyncFirefoxAccountClient implements FirefoxSyncClient {

    private final SyncClientCommandRunner commandRunner = new SyncClientCommandRunner();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(); // TODO: use shared executor? How do they stop/get GC'd?

    private final FirefoxAccount account;

    public FirefoxSyncFirefoxAccountClient(final FirefoxAccount account) {
        // todo: assert logged in?
        this.account = account;
    }

    @Override
    public List<HistoryRecord> getHistory() {
        final Future<List<HistoryRecord>> future = commandRunner.queueAndRunCommand(new GetSyncHistoryCommand(5000), getInitialSyncConfig());
        try {
            return future.get(); // todo: timeout.
        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace(); // todo: what now?
            return null;
        }
    }

    @Override
    public List<PasswordRecord> getPasswords() {
        final Future<List<PasswordRecord>> future = commandRunner.queueAndRunCommand(new GetSyncPasswordsCommand(), getInitialSyncConfig());
        try {
            return future.get(); // todo: timeout.
        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace(); // todo: what now?
            return null;
        }
    }

    @Override
    public List<BookmarkRecord> getBookmarks() {
        final Future<List<BookmarkRecord>> future = commandRunner.queueAndRunCommand(new GetSyncBookmarksCommand(), getInitialSyncConfig());
        try {
            return future.get(); // todo: timeout.
        } catch (final InterruptedException | ExecutionException e) {
            e.printStackTrace(); // todo: what now?
            return null;
        }
    }

    @Override
    public String getEmail() {
        return null;
    }

    private FirefoxAccountSyncConfig getInitialSyncConfig() {
        return new FirefoxAccountSyncConfig(account, networkExecutor, null, null);
    }
}

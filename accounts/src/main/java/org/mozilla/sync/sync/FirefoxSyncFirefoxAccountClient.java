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
import org.mozilla.sync.sync.commands.GetSyncHistoryCommand;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO:
 */
class FirefoxSyncFirefoxAccountClient implements FirefoxSyncClient {

    private final SyncClientCommandRunner commandRunner = new SyncClientCommandRunner();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    private final FirefoxAccount account;

    public FirefoxSyncFirefoxAccountClient(final FirefoxAccount account) {
        // todo: assert logged in?
        this.account = account;
    }

    @Override
    public List<HistoryRecord> getHistory() {
        //commandRunner.queueAndRunCommand(new GetSyncHistoryCommand(itemLimit, callback), getInitialSyncConfig(context));
        return null;
    }

    @Override
    public List<PasswordRecord> getPasswords() {
        //commandRunner.queueAndRunCommand(new GetSyncPasswordsCommand(callback), getInitialSyncConfig(context));
        return null;
    }

    @Override
    public List<BookmarkRecord> getBookmarks() {
        //commandRunner.queueAndRunCommand(new GetSyncBookmarksCommand(callback), getInitialSyncConfig(context));
        return null;
    }

    @Override
    public String getEmail() {
        return null;
    }

    private FirefoxAccountSyncConfig getInitialSyncConfig(final Context context) {
        return new FirefoxAccountSyncConfig(context, account, networkExecutor, null, null);
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import android.content.Context;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.sync.callbacks.SyncHistoryCallback;
import org.mozilla.accounts.sync.commands.GetSyncHistoryCommand;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirefoxAccountSyncClient {

    public static final String SYNC_CONFIG_SHARED_PREFS_NAME = "org.mozilla.accounts.FirefoxSyncClient.syncConfig";

    private final SyncClientCommandRunner commandRunner = new SyncClientCommandRunner();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    private final FirefoxAccount account;

    public FirefoxAccountSyncClient(final FirefoxAccount account) {
        this.account = account;
    }

    public void getCollectionInfo() {
        // TODO
        //final URI uri = new URI(storageServerURI.toString() + "/info/collections");
    }

    public void getHistory(final Context context, final int itemLimit, final SyncHistoryCallback callback) {
        // todo: assert logged in.
        commandRunner.queueAndRunCommand(new GetSyncHistoryCommand(itemLimit, callback), getInitialSyncConfig(context));
    }

    private FirefoxAccountSyncConfig getInitialSyncConfig(final Context context) {
        return new FirefoxAccountSyncConfig(context, account, networkExecutor, null);
    }
}

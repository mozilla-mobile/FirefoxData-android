/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.FirefoxAccountSyncTokenAccessor;
import org.mozilla.accounts.sync.commands.SyncClientCommands.OnAsyncPreCommandComplete;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientAsyncPreCommand;
import org.mozilla.gecko.tokenserver.TokenServerToken;

/**
 * A command to get the Sync token associated with the Firefox account. This command expects
 * the account to be in the married state.
 */
public class GetSyncTokenPreCommand extends SyncClientAsyncPreCommand {

    @Override
    void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final OnAsyncPreCommandComplete onComplete) {
        FirefoxAccountSyncTokenAccessor.get(syncConfig.account, new FirefoxAccountSyncTokenAccessor.TokenCallback() {
                @Override
                public void onError(final Exception e) {
                    onComplete.onException(e);
                }

                @Override
                public void onTokenReceived(final TokenServerToken token) {
                    final FirefoxAccountSyncConfig updatedSyncConfig = new FirefoxAccountSyncConfig(syncConfig.contextWeakReference,
                            syncConfig.account, syncConfig.networkExecutor, token, null);
                    onComplete.onSuccess(updatedSyncConfig);
                }
            });
    }
}

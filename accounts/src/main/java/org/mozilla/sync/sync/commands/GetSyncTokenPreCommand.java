/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync.commands;

import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.sync.sync.FirefoxAccountSyncTokenAccessor;
import org.mozilla.sync.sync.commands.SyncClientCommands.SyncClientAsyncPreCommand;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.util.IOUtil;

/**
 * A command to get the Sync token associated with the Firefox account. This command expects
 * the account to be in the married state.
 */
public class GetSyncTokenPreCommand extends SyncClientAsyncPreCommand {

    @Override
    public void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<FirefoxAccountSyncConfig> onComplete) {
        FirefoxAccountSyncTokenAccessor.get(syncConfig.account, new FirefoxAccountSyncTokenAccessor.TokenCallback() {
                @Override
                public void onError(final Exception e) {
                    onComplete.onError(e);
                }

                @Override
                public void onTokenReceived(final TokenServerToken token) {
                    final FirefoxAccountSyncConfig updatedSyncConfig = new FirefoxAccountSyncConfig(syncConfig.account,
                            syncConfig.networkExecutor, token, null);
                    onComplete.onSuccess(updatedSyncConfig);
                }
            });
    }
}

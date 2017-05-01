/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import android.content.Context;
import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.accounts.login.FirefoxAccountLoginDefaultDelegate;
import org.mozilla.accounts.sync.commands.SyncClientCommands.SyncClientAsyncPreCommand;
import org.mozilla.accounts.sync.commands.SyncClientCommands.OnAsyncPreCommandComplete;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.State;

/**
 * A sync pre command to advance the account to the Marriage state.
 */
public class AdvanceToMarriagePreCommand extends SyncClientAsyncPreCommand {

    @Override
    void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final OnAsyncPreCommandComplete onComplete) {
        // `advance` is a no-op if we're already married: no harm running it in every case.
        new FxAccountLoginStateMachine().advance(syncConfig.account.accountState, State.StateLabel.Married, new FirefoxAccountLoginDefaultDelegate(syncConfig.account, syncConfig.networkExecutor) {
            @Override
            public void handleFinal(final State state) {
                final FirefoxAccount updatedAccount = account.withNewState(state);
                maybeStoreAccount(updatedAccount);
                if (state.getStateLabel() != State.StateLabel.Married) {
                    onComplete.onException(new Exception("Unable to advance to married state. Instead: " +
                            updatedAccount.accountState.getStateLabel().name()));
                } else {
                    onComplete.onSuccess(new FirefoxAccountSyncConfig(syncConfig.contextWeakReference,
                            updatedAccount, syncConfig.networkExecutor, syncConfig.token, null));
                }
            }

            private void maybeStoreAccount(final FirefoxAccount account) {
                final Context context = syncConfig.contextWeakReference.get();
                if (context == null) {
                    Log.w(LOGTAG, "Unable to save account with updated state: context is null.");
                } else {
                    new FirefoxAccountDevelopmentStore(context).saveFirefoxAccount(account);
                }
            }
        });
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import android.content.Context;
import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.accounts.FirefoxAccountShared;
import org.mozilla.accounts.login.FirefoxAccountLoginMarriedDelegate;
import org.mozilla.accounts.login.FirefoxAccountLoginMarriedDelegate.MarriedCallback;
import org.mozilla.accounts.sync.FirefoxAccountSyncTokenAccessor.TokenCallback;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import java.lang.ref.WeakReference;
import java.util.List;

public class FirefoxAccountSyncClient {

    public interface BaseSyncResult {
        void onError(Exception e);
    }

    public interface SyncHistoryResult extends BaseSyncResult {
        void onReceive(List<HistoryRecord> historyRecords);
    }

    private static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    private final WeakReference<Context> contextWeakReference;
    private final FirefoxAccount account;

    public FirefoxAccountSyncClient(final Context context, final FirefoxAccount account) {
        this.contextWeakReference = new WeakReference<>(context);
        this.account = account;
    }

    // oh, I guess async?
    public List<Record> getHistory(final int limit) {
        // advance married.
        return null;
    }

    // todo: need married, then sync token.
    public void ensureSyncToken(final TokenCallback callback) {
        // TODO: if on disk/in-memory, access that value. Should this be separate cache class?
        advanceToMarried(new MarriedCallback() {
            @Override
            public void onNotMarried(final FirefoxAccount account, final State notMarriedState) {
                // TODO: anything else?
                callback.onError(new Exception("Could not advance to married state. Instead: " + notMarriedState.getStateLabel()));
            }

            @Override
            public void onMarried(final FirefoxAccount updatedAccount, final Married marriedState) {
                maybeStoreAccount(updatedAccount); // TODO: maybe don't conflate storing account with SyncClient.
                FirefoxAccountSyncTokenAccessor.get(updatedAccount, callback);
            }
        });
    }

    private void advanceToMarried(final MarriedCallback marriedCallback) {
        // TODO: we could be more efficient if we didn't spawn a new Runnable each time.
        // advance uses the network & must be called from a background thread.
        FirefoxAccountShared.executor.execute(new Runnable() {
            @Override
            public void run() {
                new FxAccountLoginStateMachine().advance(account.accountState, StateLabel.Married,
                        new FirefoxAccountLoginMarriedDelegate(account, marriedCallback));
            }
        });
    }

    private void maybeStoreAccount(final FirefoxAccount account) {
        final Context context = contextWeakReference.get();
        if (context != null) {
            new FirefoxAccountDevelopmentStore(context).saveFirefoxAccount(account);
        } else {
            Log.w(LOGTAG, "Unable to store Firefox Account: context is null.");
        }
    }
}

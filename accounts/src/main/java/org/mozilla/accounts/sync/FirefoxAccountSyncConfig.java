/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import android.content.Context;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

/**
 * Data container for information necessary to sync.
 */
public class FirefoxAccountSyncConfig {
    public final WeakReference<Context> contextWeakReference;
    public final FirefoxAccount account;
    public final ExecutorService networkExecutor;
    public final TokenServerToken token;

    public FirefoxAccountSyncConfig(final Context context, final FirefoxAccount account,
            final ExecutorService networkExecutor, final TokenServerToken token) {
        this.contextWeakReference = new WeakReference<Context>(context);
        this.account = account;
        this.networkExecutor = networkExecutor; // TODO: is this a bad place for it?
        this.token = token;
    }

    public FirefoxAccountSyncConfig(final WeakReference<Context> contextWeakReference, final FirefoxAccount account,
            final ExecutorService networkExecutor, final TokenServerToken token) {
        this.contextWeakReference = contextWeakReference;
        this.account = account;
        this.networkExecutor = networkExecutor;
        this.token = token;
    }
}

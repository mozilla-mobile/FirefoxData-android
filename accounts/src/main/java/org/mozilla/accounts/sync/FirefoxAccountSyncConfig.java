/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import android.content.Context;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountUtils;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;

/**
 * Data container for information necessary to sync.
 */
public class FirefoxAccountSyncConfig {
    public final WeakReference<Context> contextWeakReference;
    public final FirefoxAccount account;
    public final ExecutorService networkExecutor;
    public final TokenServerToken token;
    public final CollectionKeys collectionKeys;

    private KeyBundle keyBundleCache;

    public FirefoxAccountSyncConfig(final Context context, final FirefoxAccount account,
            final ExecutorService networkExecutor, final TokenServerToken token, final CollectionKeys collectionKeys) {
        this.contextWeakReference = new WeakReference<Context>(context);
        this.account = account;
        this.networkExecutor = networkExecutor;
        this.token = token;
        this.collectionKeys = collectionKeys;
    }

    public FirefoxAccountSyncConfig(final WeakReference<Context> contextWeakReference, final FirefoxAccount account,
            final ExecutorService networkExecutor, final TokenServerToken token, final CollectionKeys collectionKeys) {
        this.contextWeakReference = contextWeakReference;
        this.account = account;
        this.networkExecutor = networkExecutor;
        this.token = token;
        this.collectionKeys = collectionKeys;
    }

    /** Convenience method to get the sync key bundle. Assumes the account in the config is in the Married state. */
    public KeyBundle getSyncKeyBundle() throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        if (keyBundleCache == null) {
            final Married married = FirefoxAccountUtils.getMarried(account.accountState);
            keyBundleCache = married.getSyncKeyBundle();
        }
        return keyBundleCache;
    }
}

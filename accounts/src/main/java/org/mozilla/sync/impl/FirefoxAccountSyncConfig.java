/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.impl; // TODO: only here for access to FirefoxAccount.

import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;

/**
 * Data container for information necessary to sync.
 */
public class FirefoxAccountSyncConfig {
    public final FirefoxAccount account;
    public final ExecutorService networkExecutor;
    public final TokenServerToken token;
    public final CollectionKeys collectionKeys;

    private KeyBundle keyBundleCache;

    public FirefoxAccountSyncConfig(final FirefoxAccount account, final ExecutorService networkExecutor,
            final TokenServerToken token, final CollectionKeys collectionKeys) {
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

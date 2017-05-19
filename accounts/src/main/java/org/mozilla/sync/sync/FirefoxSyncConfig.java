/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.sync.impl.FirefoxAccount;

import java.util.concurrent.ExecutorService;

/** Data container for information necessary to sync. */
class FirefoxSyncConfig {
    final FirefoxAccount account; // TODO: rm unused?
    final ExecutorService networkExecutor;
    final TokenServerToken token;
    final CollectionKeys collectionKeys;

    FirefoxSyncConfig(final FirefoxAccount account, final ExecutorService networkExecutor,
            final TokenServerToken token, final CollectionKeys collectionKeys) {
        this.account = account;
        this.networkExecutor = networkExecutor;
        this.token = token;
        this.collectionKeys = collectionKeys;
    }
}

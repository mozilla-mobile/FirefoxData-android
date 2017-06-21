/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.sync;

import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.tokenserver.TokenServerToken;

/** Data container for information necessary to sync. */
class FirefoxSyncConfig {
    final TokenServerToken token;
    final CollectionKeys collectionKeys;

    FirefoxSyncConfig(final TokenServerToken token, final CollectionKeys collectionKeys) {
        this.token = token;
        this.collectionKeys = collectionKeys;
    }
}

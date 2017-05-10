/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.content.Context;

/**
 * A collection of static functions with entry points to Firefox Sync operations.
 */
public class FirefoxSync {
    private FirefoxSync() {}

    // todo: name.
    // tODO: singleton or new instance?
    public static FirefoxSyncLoginManager getLoginManager(final Context context) {
        return new FirefoxSyncWebViewLoginManager(context);
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import org.mozilla.sync.login.InternalFirefoxSyncLoginManagerFactory;

/**
 * A collection of static functions with entry points to Firefox Sync operations.
 */
public class FirefoxSync {
    private FirefoxSync() {}

    /**
     * Gets a FirefoxSyncLoginManager, which grants access a Firefox Account user's Sync information.
     *
     * This function is intended to be called in initialization (such as {@link android.app.Activity#onCreate(Bundle)})
     * because it can only be called with a specific Context instance once.
     *
     * @param context The Context from which this LoginManager is being accessed.
     * @return A FirefoxSyncLoginManager.
     */
    public static FirefoxSyncLoginManager getLoginManager(@NonNull final Context context) {
        return InternalFirefoxSyncLoginManagerFactory.internalGetLoginManager(context);
    }
}

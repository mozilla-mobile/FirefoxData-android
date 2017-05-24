/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import org.mozilla.sync.login.FirefoxSyncLoginManager;
import org.mozilla.sync.login.InternalFirefoxSyncLoginManagerFactory;
import org.mozilla.util.DeviceUtils;

/**
 * The main entry point to the Firefox Sync library: this class is a collection of static
 * functions to interact with the library.
 */
public class FirefoxSync {
    private FirefoxSync() {}

    /**
     * Initializes the library, in particular, for items that need a Context.
     *
     * This function should be called before any Objects are returned to the library user.
     */
    private static void initLibrary(final Context context) {
        DeviceUtils.init(context);
    }

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
        initLibrary(context);
        return InternalFirefoxSyncLoginManagerFactory.internalGetLoginManager(context);
    }
}

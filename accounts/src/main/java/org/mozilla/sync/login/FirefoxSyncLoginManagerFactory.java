/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.content.Context;
import org.mozilla.sync.FirefoxSyncLoginManager;

/**
 * TODO:
 */
public class FirefoxSyncLoginManagerFactory {
    private FirefoxSyncLoginManagerFactory() {} // TODO: is factory pattern okay?

    public static FirefoxSyncLoginManager getLoginManager(final Context context) {
        return new FirefoxSyncWebViewLoginManager(context);
    }
}

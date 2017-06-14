/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.impl;

import android.text.TextUtils;
import android.util.Log;
import org.mozilla.sync.login.FirefoxSyncLoginManager;

/** A collection of shared functions for Firefox Sync. */
public class FirefoxSyncShared {
    public static final String LOGTAG = "FirefoxSync";

    private static String signedInApplication;

    private FirefoxSyncShared() {}

    /**
     * Globally sets the name of the signed in application using this library.
     *
     * The applicationName resolves around the currently signed in account in the FirefoxAccountSessionSharedPrefs: if
     * an account is signed in, this correlates with the signed in application name; if no account is signed in, it will
     * be null. In the current API, only one application can be signed in so there should never be a conflict.
     *
     * See {@link #getUserAgent} for more info on how this is used.
     */
    public static void setSessionApplicationName(final String applicationName) {
        signedInApplication = applicationName;
    }

    /**
     * Returns the User Agent for network requests to the Firefox servers.
     *
     * HACK: the user agent depends on the name of the logged in application and is thus dependent on an instance of
     * {@link FirefoxSyncLoginManager}. However, our infrastructure for making requests embeds the
     * User Agent override deeply in the code so it's often non-trivial to pass it in. Instead (at the cost of fragility),
     * we define a global signed in application via {@link #setSessionApplicationName(String)}, get the global form factor
     * from {@link DeviceUtils} and allow the request code to access the user agent globally here. We should consider a
     * proper solution when we replace our request infrastructure (issue #4).
     */
    public static String getUserAgent() {
        final String appName;
        if (!TextUtils.isEmpty(signedInApplication)) {
            appName = signedInApplication;
        } else {
            Log.w(LOGTAG, "getUserAgent: signedInApplication is unexpectedly not yet set");
            appName = "Unknown app";
        }
        return FirefoxSyncRequestUtils.getUserAgent(appName, DeviceUtils.isTablet());
    }
}

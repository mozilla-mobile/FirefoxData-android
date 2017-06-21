/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.impl;

/**
 * A class to help the gecko/ module, which contains all the code imported from fennec, access functionality from
 * the main module. This is necessary for the following:
 *
 * - FirefoxSyncShared.getUserAgent: it was too difficult to pass a String userAgent argument to the request code in the
 * fennec/ module so I added a static reference to get the user agent.
 */
public class FirefoxSyncInterModuleReceiver {

    interface UserAgentFetcher { String getUserAgent(); }

    private static UserAgentFetcher userAgentFetcher;

    static void setUserAgentFetcher(final UserAgentFetcher userAgentFetcher) {
        FirefoxSyncInterModuleReceiver.userAgentFetcher = userAgentFetcher;
    }

    public static String getUserAgent() { return userAgentFetcher != null ? userAgentFetcher.getUserAgent() : null; }
}

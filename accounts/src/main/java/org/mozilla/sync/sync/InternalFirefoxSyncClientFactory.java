/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.login.FirefoxAccount;

/**
 * <b>NON-PUBLIC API:</b> please use {@link org.mozilla.sync.FirefoxSync} instead. This class is used to escalate
 * visibility of {@code protected} components for internal library use.
 */
public class InternalFirefoxSyncClientFactory {
    private InternalFirefoxSyncClientFactory() {}

    /** Please don't use directly. */
    public static FirefoxSyncClient getSyncClient(final FirefoxAccount account) {
        return new FirefoxSyncFirefoxAccountClient(account);
    }
}

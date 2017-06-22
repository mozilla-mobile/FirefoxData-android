/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import org.mozilla.fxa_data.FirefoxData;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.tokenserver.TokenServerToken;
import org.mozilla.fxa_data.impl.FirefoxAccount;

/**
 * <b>NON-PUBLIC API:</b> please use {@link FirefoxData} instead. This class is used to escalate
 * visibility of {@code protected} components for internal library use.
 */
public class InternalFirefoxDataClientFactory {
    private InternalFirefoxDataClientFactory() {}

    /** Please don't use directly. */
    public static FirefoxDataClient getDataClient(final FirefoxAccount account, final TokenServerToken token,
            final CollectionKeys collectionKeys) {
        return new FirefoxDataFirefoxAccountClient(account, token, collectionKeys);
    }
}

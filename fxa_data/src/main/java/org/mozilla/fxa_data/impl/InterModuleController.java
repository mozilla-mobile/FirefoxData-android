/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.impl;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An abstraction to help other modules access code from this primary module. For example, it helps to instantiate
 * {@link FirefoxSyncInterModuleReceiver}.
 */
class InterModuleController {

    private static final AtomicBoolean isInit = new AtomicBoolean(false);

    static synchronized void init() {
        if (isInit.get()) { return; }

        FirefoxSyncInterModuleReceiver.setUserAgentFetcher(new FirefoxSyncInterModuleReceiver.UserAgentFetcher() {
            @Override
            public String getUserAgent() { return FirefoxDataShared.getUserAgent(); }
        });

        isInit.set(true);
    }
}

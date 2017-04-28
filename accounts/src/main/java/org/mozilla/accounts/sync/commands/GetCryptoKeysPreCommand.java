/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;

/** A command to get the crypto keys necessary to begin a sync. */
public class GetCryptoKeysPreCommand extends SyncClientPreCommand {
    @Override
    public FirefoxAccountSyncConfig call(final FirefoxAccountSyncConfig syncConfig) throws Exception {
        return syncConfig;
    }
}

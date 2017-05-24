/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.sync.login;

import android.support.annotation.NonNull;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.sync.impl.FirefoxAccount;

/** An instance of a logged in account and its metadata. */
class FirefoxAccountSession {

    final FirefoxAccount firefoxAccount;
    /** The name of the application that logged us in. */
    final String applicationName;

    FirefoxAccountSession(@NonNull final FirefoxAccount firefoxAccount, @NonNull final String applicationName) {
        if (firefoxAccount == null || applicationName == null) { throw new IllegalArgumentException("FirefoxAccountSession: expected non-null args."); }
        this.firefoxAccount = firefoxAccount;
        this.applicationName = applicationName;
    }
}

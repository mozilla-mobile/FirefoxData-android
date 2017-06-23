/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.login;

import android.content.Context;
import android.support.annotation.NonNull;
import org.mozilla.fxa_data.FirefoxData;

/**
 * <b>NON-PUBLIC API:</b> please use {@link FirefoxData} instead. This class is used to escalate
 * visibility of {@code protected} components for internal library use.
 */
public class InternalFirefoxDataLoginManagerFactory {
    private InternalFirefoxDataLoginManagerFactory() {}

    /** Please use {@link FirefoxData#getLoginManager(android.content.Context)} instead. */
    public static FirefoxDataLoginManager internalGetLoginManager(@NonNull final Context context) {
        // We return a new instance, rather than a singleton, because the Context can change.
        final FirefoxAccountSessionSharedPrefsStore sessionStore = new FirefoxAccountSessionSharedPrefsStore(context);
        return new FirefoxDataWebViewLoginManager(sessionStore);
    }
}

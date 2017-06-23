/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import android.support.annotation.NonNull;

/**
 * A data class for a password stored on a web page.
 */
public class PasswordRecord {

    private final org.mozilla.gecko.sync.repositories.domain.PasswordRecord underlyingRecord;

    PasswordRecord(final org.mozilla.gecko.sync.repositories.domain.PasswordRecord underlyingRecord) {
        this.underlyingRecord = underlyingRecord;
    }

    /**
     * The hostname this password was saved on.
     * @return The hostname or the empty String if it does not exist.
     */
    @NonNull public String getHostname() { return StringUtils.emptyStrIfNull(underlyingRecord.hostname); }

    /**
     * The user name associated with this password.
     * @return the user name or the empty String if it does not exist.
     */
    @NonNull public String getUsername() { return StringUtils.emptyStrIfNull(underlyingRecord.encryptedUsername); /* not actually encrypted */ }

    /**
     * The unencrypted password that was stored.
     * @return the password or the empty String if it does not exist.
     */
    @NonNull public String getPassword() { return StringUtils.emptyStrIfNull(underlyingRecord.encryptedPassword); /* not actually encrypted */ }

    // Additional fields we can add:
    // - formSubmitUrl
    // - httpRealm
    // - usernameField
    // - passwordField
    // - timeCreated
    // - timeLastUsed
    // - timePassChanged (useful for reminding to change passwords!)
    // - timesUsed
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;
import org.mozilla.util.StringUtils;

/**
 * TODO:
 */
public class PasswordRecord {

    private final org.mozilla.gecko.sync.repositories.domain.PasswordRecord underlyingRecord;

    PasswordRecord(final org.mozilla.gecko.sync.repositories.domain.PasswordRecord underlyingRecord) {
        this.underlyingRecord = underlyingRecord;
    }

    // todo: docs.
    @NonNull public String getHostname() { return StringUtils.emptyStrIfNull(underlyingRecord.hostname); }
    @NonNull public String getUsername() { return StringUtils.emptyStrIfNull(underlyingRecord.encryptedUsername); /* not actually encrypted */ }
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

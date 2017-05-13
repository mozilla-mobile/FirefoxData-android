/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

/**
 * TODO:
 */
public class PasswordRecord {

    private org.mozilla.gecko.sync.repositories.domain.PasswordRecord underlyingRecord;

    public PasswordRecord(final org.mozilla.gecko.sync.repositories.domain.PasswordRecord underlyingRecord) {this.underlyingRecord = underlyingRecord;}

    // todo: docs.
    public String getHostname() { return underlyingRecord.hostname; }
    public String getUsername() { return underlyingRecord.encryptedUsername; /* not actually encrypted */ }
    public String getPassword() { return underlyingRecord.encryptedPassword; /* not actually encrypted */ }

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

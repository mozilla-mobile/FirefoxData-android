/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

/**
 * TODO:
 */
public class FirefoxSyncLoginException extends FirefoxSyncException {
    private final FailureReason failureReason;

    public FirefoxSyncLoginException(final FailureReason failureReason) {
        this.failureReason = failureReason;
    }

    public FailureReason getFailureReason() { return failureReason; }

    public enum FailureReason { // Some reasons: https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#response-format
        ACCOUNT_NOT_VERIFIED,
        SERVER_SENT_UNEXPECTED_MESSAGE,
        SERVER_SENT_INVALID_ACCOUNT,
        UNKNOWN,
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

/**
 * TODO:
 */
public class LoginSyncException extends FirefoxSyncException {
    private final FailureReason failureReason;

    public LoginSyncException(final FailureReason failureReason) {
        this.failureReason = failureReason;
    }

    public FailureReason getFailureReason() { return failureReason; }

    public enum FailureReason {
        SERVER_SENT_UNEXPECTED_MESSAGE,
        SERVER_SENT_INVALID_ACCOUNT,
        UNKNOWN,
    }
}

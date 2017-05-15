/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

/**
 * TODO:
 */
public class FirefoxSyncLoginException extends Exception {
    private final FailureReason failureReason;

    public FirefoxSyncLoginException(final String message, final FailureReason failureReason) {
        super(message);
        this.failureReason = failureReason;
    }

    public FirefoxSyncLoginException(final Throwable cause, final FailureReason failureReason) {
        super(cause);
        this.failureReason = failureReason; // TODO: add message?
    }

    /**
     * Gets the reason this exception was thrown. Normally, this would be handled by the type of the exception thrown
     * but because these Exceptions are not thrown but passed by callback, this is used instead.
     *
     * @return the reason this exception was thrown.
     */
    public FailureReason getFailureReason() { return failureReason; }

    public enum FailureReason { // Some reasons: https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#response-format
        ACCOUNT_NEEDS_VERIFICATION, // TODO: how to document these for public use?
        FAILED_TO_LOAD_ACCOUNT,
        SERVER_RESPONSE_UNEXPECTED,
        UNKNOWN,
    }
}

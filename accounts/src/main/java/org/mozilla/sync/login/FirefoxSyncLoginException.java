/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

/**
 * A Exception that occurs while a user tries to log in to, or access their Firefox Sync account.
 *
 * Note that the exceptions returned by {@link #getCause()} may include user info like email addresses
 * so be careful how you log them.
 * todo: ^ do we want to strip the stuff ourselves?
 */
public class FirefoxSyncLoginException extends Exception {
    private final FailureReason failureReason;

    FirefoxSyncLoginException(final String message, final FailureReason failureReason) {
        super(message + ". " + failureReason.toString());
        this.failureReason = failureReason;
    }

    FirefoxSyncLoginException(final Throwable cause, final FailureReason failureReason) {
        super(failureReason.toString(), cause);
        this.failureReason = failureReason;
    }

    /**
     * Gets the reason this exception was thrown. Normally, this would be handled by the type of the exception thrown
     * but because these Exceptions are not thrown but passed by callback, this is used instead.
     *
     * @return the reason this exception was thrown.
     */
    public FailureReason getFailureReason() { return failureReason; }

    public enum FailureReason {
        ACCOUNT_NEEDS_VERIFICATION, // TODO: how to document these for public use?
        FAILED_TO_LOAD_ACCOUNT,
        SERVER_RESPONSE_UNEXPECTED,
        UNKNOWN,
    }
}

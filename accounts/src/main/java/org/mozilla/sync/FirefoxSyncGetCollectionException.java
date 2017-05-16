/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

/**
 * A Exception that occurs while a user tries to get data located in one of their Sync collections,
 * like history or bookmarks.
 *
 * Note that the exceptions returned by {@link #getCause()} may include user info like email addresses
 * so be careful how you log them.
 * todo: ^ do we want to strip the stuff ourselves?
 */
public class FirefoxSyncGetCollectionException extends Exception {
    private final FailureReason failureReason;

    public FirefoxSyncGetCollectionException(final String message, final FailureReason failureReason) {
        super(message + ". " + failureReason.toString());
        this.failureReason = failureReason;
    }

    public FirefoxSyncGetCollectionException(final Throwable cause, final FailureReason failureReason) {
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

    // Some reasons: https://github.com/mozilla/fxa-auth-server/blob/master/docs/api.md#response-format
    public enum FailureReason {
        REQUIRES_LOGIN_PROMPT, // TODO: should we be more specific? less specific so multiple ways to handle? Return more data?
        UNKNOWN,
    }
}

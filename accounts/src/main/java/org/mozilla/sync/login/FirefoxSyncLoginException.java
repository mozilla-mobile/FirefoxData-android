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

    private static final int DEFAULT_BACKOFF_SECONDS = 0;

    private final FailureReason failureReason;
    private final int backoffSeconds;

    FirefoxSyncLoginException(final String message, final FailureReason failureReason) {
        super(message + ". " + failureReason.toString());
        this.failureReason = failureReason;
        backoffSeconds = DEFAULT_BACKOFF_SECONDS;
    }

    FirefoxSyncLoginException(final Throwable cause, final FailureReason failureReason) {
        super(failureReason.toString(), cause);
        this.failureReason = failureReason;
        backoffSeconds = DEFAULT_BACKOFF_SECONDS;
    }

    private FirefoxSyncLoginException(final int backoffSeconds) {
        super("Requires backoff of " + backoffSeconds);
        failureReason = FailureReason.REQUIRES_BACKOFF;
        this.backoffSeconds = backoffSeconds;
    }

    static FirefoxSyncLoginException newForBackoffSeconds(final int backoffSeconds) {
        return new FirefoxSyncLoginException(backoffSeconds);
    }

    /**
     * Get the number of seconds the login request should wait before retrying.
     *
     * @return the number of seconds the login request should wait before retrying; 0 if there is no backoff request.
     */
    public int getBackoffSeconds() { return backoffSeconds; }

    /**
     * Gets the reason this exception was thrown. Normally, this would be handled by the type of the exception thrown
     * but because these Exceptions are not thrown but passed by callback, this is used instead.
     *
     * @return the reason this exception was thrown.
     */
    public FailureReason getFailureReason() { return failureReason; }

    public enum FailureReason {
        ACCOUNT_NEEDS_VERIFICATION, // TODO: how to document these for public use?
        REQUIRES_BACKOFF,
        REQUIRES_LOGIN_PROMPT,
        TIMED_OUT,
        USER_HAS_NO_DATA,

        NETWORK_ERROR,
        SERVER_ERROR,

        ASSERTION_FAILURE,

        UNKNOWN,
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

/**
 * TODO: name?
 */
public class FirefoxSyncGetCollectionException extends Exception {
    private final FailureReason failureReason;

    public FirefoxSyncGetCollectionException(final Throwable cause, final FailureReason failureReason) {
        super(failureReason.toString(), cause);
        this.failureReason = failureReason;
    }

    public FailureReason getFailureReason() { return failureReason; }

    public enum FailureReason { // TODO: To RecommendedAction?
        REQUIRES_LOGIN_PROMPT, // TODO: should we be more specific? less specific so multiple ways to handle? Return more data?
        UNKNOWN,
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data;

import android.support.annotation.NonNull;

/**
 * A Exception that occurs while using the Firefox Sync library.
 *
 * Note that the exceptions returned by {@link #getCause()} may include user info like email addresses
 * so be careful how you log them.
 * todo: ^ do we want to strip the stuff ourselves?
 */
public class FirefoxDataException extends Exception {

    /** You should generally specify a cause. If you don't want to, see {@link #newWithoutThrowable(String)}. */
    private FirefoxDataException(final String message) { super(message); }
    public FirefoxDataException(final String message, final Throwable cause) { super(message, cause); }

    /**
     * Factory method to create an exception without specifying a cause {@link Throwable}.
     * Callers should generally specify a cause, so we make it obvious that you're making one without.
     */
    @NonNull
    public static FirefoxDataException newWithoutThrowable(final String message) {
        return new FirefoxDataException(message);
    }
}

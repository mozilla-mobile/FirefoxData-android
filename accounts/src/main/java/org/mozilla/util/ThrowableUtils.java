/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Utility functions for {@link Throwable}.
 */
public class ThrowableUtils {
    private ThrowableUtils() {}

    /**
     * Returns the Throwable at the bottom of the {@link Throwable#getCause()} chain. If the first {@code getCause}
     * returns null, returns the parameter throwable.
     *
     * @param throwableToGetRootCauseOf The Throwable to find the root cause of.
     * @return The Throwable at the bottom of the {@code getCause} chain, or the parameter if the first getCause is null.
     */
    @CheckResult
    public static Throwable getRootCause(@NonNull final Throwable throwableToGetRootCauseOf) {
        Throwable bottommostCause = throwableToGetRootCauseOf.getCause();
        if (bottommostCause == null) { return throwableToGetRootCauseOf; }

        Throwable nonNullCause;
        do {
            nonNullCause = bottommostCause;
            bottommostCause = nonNullCause.getCause();
        } while (bottommostCause != null);
        return nonNullCause;
    }
}

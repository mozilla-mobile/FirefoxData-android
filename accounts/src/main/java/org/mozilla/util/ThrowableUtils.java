/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility functions for {@link Throwable}.
 */
public class ThrowableUtils {
    private ThrowableUtils() {}

    /**
     * Returns a sequence from the bottom-most Throwable from calling {@link java.lang.Throwable#getCause()}
     * repeatedly, then the Throwable one level above, etc., to the given Throwable, which is included in the list.
     */
    @NonNull
    @CheckResult
    public static List<Throwable> getRootCauseToTopThrowable(final Throwable topThrowable) {
        final LinkedList<Throwable> throwables = new LinkedList<>();
        Throwable currentThrowable = topThrowable;
        while (currentThrowable != null) {
            throwables.addFirst(currentThrowable);
            currentThrowable = currentThrowable.getCause();
        }
        return throwables;
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/** A collection of static functions to operate on Strings. */
class StringUtils {
    private StringUtils() {}

    /** @return the empty String, if the input is null, else the input String. */
    @NonNull static String emptyStrIfNull(@Nullable final String input) { return (input != null) ? input : ""; }
}

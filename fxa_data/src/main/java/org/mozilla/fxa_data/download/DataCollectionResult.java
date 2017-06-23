/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import android.support.annotation.NonNull;

/**
 * A container with the data from a Firefox Sync get operation (e.g. bookmarks) and
 * related metadata. The primary results can be retrieved with {@link #getResult()}.
 *
 * We wrap the main Sync data in an Object in order to allow the API to expand in the future.
 */
public class DataCollectionResult<T> {
    private final T result;

    DataCollectionResult(@NonNull final T result) {
        this.result = result;
    }

    /**
     * Returns the data received from the Firefox Sync get request.
     * @return the user's FirefoxData data; this will never be null.
     */
    @NonNull public T getResult() { return result; }
}

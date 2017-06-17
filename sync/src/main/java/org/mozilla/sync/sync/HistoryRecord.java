/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;

/**
 * A data class for a history entry, which represents a visited URI.
 */
public class HistoryRecord { // TODO: name: record?

    private final org.mozilla.gecko.sync.repositories.domain.HistoryRecord underlyingRecord;

    HistoryRecord(final org.mozilla.gecko.sync.repositories.domain.HistoryRecord underlyingRecord) {
        this.underlyingRecord = underlyingRecord;
    }

    /**
     * The title of the visited page.
     * @return The title of the visited page or the empty String if it does not exist.
     */
    @NonNull public String getTitle() { return StringUtils.emptyStrIfNull(underlyingRecord.title); }

    /**
     * The URI of the visited page.
     * @return The URI of the visited page or the empty String if it does not exist.
     */
    @NonNull public String getURI() { return StringUtils.emptyStrIfNull(underlyingRecord.histURI); }

    // Additional fields we can add:
    // - visits array (date & type?)
    // - fennecDateVisited (do these rely on fennec data stores?)
    // - fennecVisitCount
}

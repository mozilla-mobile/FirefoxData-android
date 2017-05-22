/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;
import org.mozilla.util.StringUtils;

// TODO: docs.
public class HistoryRecord { // TODO: name: record?

    private final org.mozilla.gecko.sync.repositories.domain.HistoryRecord underlyingRecord;

    HistoryRecord(final org.mozilla.gecko.sync.repositories.domain.HistoryRecord underlyingRecord) {
        this.underlyingRecord = underlyingRecord;
    }

    @NonNull public String getTitle() { return StringUtils.emptyStrIfNull(underlyingRecord.title); }
    @NonNull public String getURI() { return StringUtils.emptyStrIfNull(underlyingRecord.histURI); }

    // Additional fields we can add:
    // - visits array (date & type?)
    // - fennecDateVisited (do these rely on fennec data stores?)
    // - fennecVisitCount
}

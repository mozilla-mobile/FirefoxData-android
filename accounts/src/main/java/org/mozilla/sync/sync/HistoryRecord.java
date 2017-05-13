/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

// TODO: Class vs. interface. docs.
public class HistoryRecord { // TODO: name: record?

    private org.mozilla.gecko.sync.repositories.domain.HistoryRecord underlyingRecord;

    public HistoryRecord(final org.mozilla.gecko.sync.repositories.domain.HistoryRecord underlyingRecord) {
        this.underlyingRecord = underlyingRecord;
    }

    // TODO: docs.
    public String getTitle() { return underlyingRecord.title; }
    public String getURI() { return underlyingRecord.histURI; }

    // Additional fields we can add:
    // - visits array (date & type?)
    // - fennecDateVisited (do these rely on fennec data stores?)
    // - fennecVisitCount
}

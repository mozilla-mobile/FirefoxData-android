/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

/**
 * TODO: method docs.
 */
public class BookmarkRecord extends BookmarkBase {

    BookmarkRecord(final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord bookmarkRecord) {
        super(bookmarkRecord);
    }

    public String getURI() { return underlyingRecord.bookmarkURI; }
}

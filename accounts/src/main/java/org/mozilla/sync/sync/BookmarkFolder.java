/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO: method docs.
 */
public class BookmarkFolder extends BookmarkBase {

    private List<BookmarkFolder> subfolders = new ArrayList<>(); // mutable for ease of creation.
    private List<BookmarkRecord> bookmarks = new ArrayList<>(); // mutable for ease of creation.

    BookmarkFolder(final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord bookmarkRecord) {
        super(bookmarkRecord);
    }

    void makeImmutable() {
        subfolders = Collections.unmodifiableList(subfolders);
        bookmarks = Collections.unmodifiableList(bookmarks);
    }

    public List<BookmarkFolder> getSubfolders() { return subfolders; }
    public List<BookmarkRecord> getBookmarks() { return bookmarks; }

    static BookmarkFolder createRootFolder() {
        // Rather than create an alternative implementation for Bookmark*, I make an underlying record for the root,
        // even though it's a little gross and easier to break.
        final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord rootRawRecord = new org.mozilla.gecko.sync.repositories.domain.BookmarkRecord(null);
        rootRawRecord.guid = "places";
        rootRawRecord.title = "Bookmarks Root Folder"; // TODO: names & stuff.
        rootRawRecord.description = "The root bookmark folder";

        return new BookmarkFolder(rootRawRecord);
    }
}

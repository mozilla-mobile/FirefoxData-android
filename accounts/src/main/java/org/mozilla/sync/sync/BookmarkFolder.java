/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO: method docs.
 */
public class BookmarkFolder extends BookmarkBase {

    static final String ROOT_FOLDER_GUID = "places";

    private List<BookmarkFolder> subfolders = new ArrayList<>(); // mutable for ease of creation.
    private List<BookmarkRecord> bookmarks = new ArrayList<>(); // mutable for ease of creation.

    BookmarkFolder(@NonNull final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord bookmarkRecord) {
        super(bookmarkRecord);
    }

    @NonNull public List<BookmarkFolder> getSubfolders() { return subfolders; }
    @NonNull public List<BookmarkRecord> getBookmarks() { return bookmarks; }

    /** Mutates collections within this folder to immutable versions so they can be returned from the API. */
    void makeImmutable() {
        subfolders = Collections.unmodifiableList(subfolders);
        bookmarks = Collections.unmodifiableList(bookmarks);
    }

    /** @return a folder representing the root folder. */
    static BookmarkFolder createRootFolder() {
        // Rather than create an alternative implementation for Bookmark*, I make an underlying record for the root,
        // even though it's a little gross and easier to break.
        final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord rootRawRecord = new org.mozilla.gecko.sync.repositories.domain.BookmarkRecord(null);
        rootRawRecord.guid = ROOT_FOLDER_GUID;
        rootRawRecord.title = "Bookmarks Root Folder";
        rootRawRecord.description = "The root bookmark folder";

        return new BookmarkFolder(rootRawRecord);
    }
}

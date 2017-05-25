/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A bookmark folder data class, which has a name, description, and a
 * list of bookmarks and subfolders, among other attributes.
 */
public class BookmarkFolder extends BookmarkBase {

    static final String ROOT_FOLDER_GUID = "places";

    private final List<BookmarkFolder> subfolders = new ArrayList<>(); // will mutate the list to populate.
    private final List<BookmarkRecord> bookmarks = new ArrayList<>(); // will mutate the list to populate.

    BookmarkFolder(@NonNull final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord bookmarkRecord) {
        super(bookmarkRecord);
    }

    /**
     * Returns an immutable list of bookmark folders inside this folder.
     * @return a list of bookmark folders, or an empty list if there are no items.
     */
    @NonNull public List<BookmarkFolder> getSubfolders() { return subfolders; }

    /**
     * Returns an immutable list of bookmarks inside this folder.
     * @return a list of bookmarks, or an empty list if there are no items.
     * */
    @NonNull public List<BookmarkRecord> getBookmarks() { return bookmarks; }

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

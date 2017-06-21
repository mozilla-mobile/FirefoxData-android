/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/** A base implementation of a bookmark data class. */
class BookmarkBase {

    final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord underlyingRecord;

    BookmarkFolder parentFolder; // mutable for ease of creation.

    BookmarkBase(@NonNull final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord underlyingRecord) {
        this.underlyingRecord = underlyingRecord;
    }

    /**
     * Returns the title the user associated with this bookmark.
     * @return the title text or the empty string if there is no title.
     */
    @NonNull public String getTitle() { return StringUtils.emptyStrIfNull(underlyingRecord.title); }

    /**
     * Returns the description text the user associated with this bookmark.
     * @return the description text or the empty string if there is no description.
     */
    @NonNull public String getDescription() { return StringUtils.emptyStrIfNull(underlyingRecord.description); }

    /**
     * Returns the bookmark folder that contains this bookmark.
     * @return the parent bookmark folder or null if there is no parent folder.
     */
    @Nullable public BookmarkFolder getParentFolder() { return parentFolder; }
}

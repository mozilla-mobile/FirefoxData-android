/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.mozilla.util.StringUtils;

/**
 * TODO: method docs. ignore query & separator.
 */
class BookmarkBase {

    final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord underlyingRecord;

    BookmarkFolder parentFolder; // mutable for ease of creation.

    BookmarkBase(@NonNull final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord underlyingRecord) {
        this.underlyingRecord = underlyingRecord;
    }

    @NonNull public String getTitle() { return StringUtils.emptyStrIfNull(underlyingRecord.title); }
    @NonNull public String getDescription() { return StringUtils.emptyStrIfNull(underlyingRecord.description); }

    @Nullable public BookmarkFolder getParentFolder() { return parentFolder; }
}

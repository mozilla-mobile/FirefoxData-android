/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mozilla.fxa_data.impl.FirefoxSyncShared.LOGTAG;

/**
 * A bookmark data class, containing a title, description, and a URI, among other attributes.
 */
public class BookmarkRecord extends BookmarkBase {

    private final List<String> tags;

    BookmarkRecord(@NonNull final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord bookmarkRecord) {
        super(bookmarkRecord);
        tags = tagsJSONToList(underlyingRecord.tags);
    }

    /**
     * Gets the URI that this bookmark represents.
     * @return the URI for this bookmark or the empty String if the URI is empty.
     */
    @NonNull public String getURI() { return StringUtils.emptyStrIfNull(underlyingRecord.bookmarkURI); }

    /**
     * Gets the keyword search the user has associated with this bookmark.
     * @return the keyword for this bookmark or the empty String if there is no keyword.
     */
    @NonNull public String getKeyword() { return StringUtils.emptyStrIfNull(underlyingRecord.keyword); }

    /**
     * Gets a list of tags the user has associated with this bookmark.
     * @return a list of tags for this bookmark or an empty list if there are no tags.
     */
    @NonNull public List<String> getTags() { return tags; }

    private static List<String> tagsJSONToList(@Nullable final JSONArray tags) {
        if (tags == null || tags.size() == 0) { return Collections.emptyList(); }

        final List<String> stringList = new ArrayList<>(tags.size());
        for (final Object tagObj : tags) {
            if (!(tagObj instanceof String)) {
                Log.w(LOGTAG, "tagsJSONToList: ignoring tag of unknown type.");
                continue;
            }

            stringList.add((String) tagObj);
        }
        return stringList;
    }
}

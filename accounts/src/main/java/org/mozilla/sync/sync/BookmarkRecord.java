/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import org.json.simple.JSONArray;
import org.mozilla.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mozilla.sync.impl.FirefoxAccountShared.LOGTAG;

/**
 * TODO: method docs.
 */
public class BookmarkRecord extends BookmarkBase {

    private final List<String> tags;

    BookmarkRecord(@NonNull final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord bookmarkRecord) {
        super(bookmarkRecord);
        tags = Collections.unmodifiableList(tagsJSONToList(underlyingRecord.tags));
    }

    @NonNull public String getURI() { return StringUtils.emptyStrIfNull(underlyingRecord.bookmarkURI); }
    @NonNull public String getKeyword() { return StringUtils.emptyStrIfNull(underlyingRecord.keyword); }
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

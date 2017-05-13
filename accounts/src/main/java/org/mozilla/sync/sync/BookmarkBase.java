/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO: method docs. ignore query & separator.
 */
class BookmarkBase {

    final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord underlyingRecord;

    BookmarkFolder parentFolder; // mutable for ease of creation.

    private final List<String> tags;

    BookmarkBase(final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord underlyingRecord) {
        this.underlyingRecord = underlyingRecord;
        tags = Collections.unmodifiableList(tagsJSONToList(underlyingRecord.tags));
    }

    private static List<String> tagsJSONToList(final JSONArray tags) {
        // TODO:
        return new ArrayList<>();
    }

    // TODO: nullable?
    public String getTitle() { return underlyingRecord.title; }
    public String getDescription() { return underlyingRecord.description; }
    public String getKeyword() { return underlyingRecord.keyword; } // todo: common to folder?
    public List<String> getTags() { return tags; } // todo: common to folder?

    public BookmarkFolder getParentFolder() { return parentFolder; }
}

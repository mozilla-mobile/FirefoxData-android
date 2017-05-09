/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;

import java.util.List;

/**
 * TODO:
 */
public interface FirefoxSyncClient {
    // TODO: return type.
    List<HistoryRecord> getHistory();
    List<PasswordRecord> getPasswords();
    List<BookmarkRecord> getBookmarks();

    String getEmail(); // TODO: verify with server has not changed.
}

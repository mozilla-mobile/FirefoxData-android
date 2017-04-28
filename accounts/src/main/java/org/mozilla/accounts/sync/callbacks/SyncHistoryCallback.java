/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.callbacks;

import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;

import java.util.List;

/** A callback for a sync client history request. */
public interface SyncHistoryCallback extends BaseSyncCallback {
    void onReceive(List<HistoryRecord> historyRecords);
}

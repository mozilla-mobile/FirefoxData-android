/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync.commands;

import java.util.List;

/** A callback for a Sync command that returns the value of a collection. */
public interface SyncCollectionCallback<R> {
    void onReceive(List<R> records);
    void onError(Exception e);
}

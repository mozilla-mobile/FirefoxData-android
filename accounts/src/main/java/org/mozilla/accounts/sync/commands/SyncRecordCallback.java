/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import org.mozilla.util.ChainableCallable.ChainableCallableCallback;

import java.util.List;

/** A Callback for a Sync command that returns a list of records. */
public interface SyncRecordCallback<R> extends ChainableCallableCallback {
    void onReceive(List<R> records);
}

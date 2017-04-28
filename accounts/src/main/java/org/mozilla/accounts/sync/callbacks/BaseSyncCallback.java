/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.callbacks;

/** A base callback for a Sync Client request. */
public interface BaseSyncCallback {
    void onError(Exception e);
}

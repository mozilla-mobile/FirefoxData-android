/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import java.util.List;

/**
 * TODO: we use container to expand API later (e.g. last modified)
 */
public class SyncCollectionResult<T> {
    private final T result;

    public SyncCollectionResult(final T result) {
        this.result = result;
    }

    public T getResult() { return result; }
}

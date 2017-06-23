/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fxa_data.login;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Static container class for a components shared in the login module. */
class FirefoxDataLoginShared {
    private FirefoxDataLoginShared() {}

    /**
     * The Executor that all background operations (at the time of writing: 5/18/17) occur on.
     *
     * Ideally, this Executor would be visible to the whole library so we don't accidentally make a new one but
     * I'd rather not make it public.
     *
     * We could make an optimization by allowing the user to pass an Executor when they make a call that returns a
     * SyncClient (to allow thread re-use) but starting our own thread 1) simplifies the API, 2) simplifies our code
     * (because we can use this nice global executor rather than passing one around), 3) allows us to control the
     * threading model, and 4) I've never really tested with more than one-thread. I think the cost starting one
     * largely idle thread is negligible but if we get requests to clean it up, we could.
     */
    static final Executor executor = Executors.newSingleThreadExecutor();
}

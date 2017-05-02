/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FirefoxAccountShared {

    public static final String LOGTAG = "FirefoxAccount";
    public static final Executor executor = Executors.newSingleThreadExecutor();
}

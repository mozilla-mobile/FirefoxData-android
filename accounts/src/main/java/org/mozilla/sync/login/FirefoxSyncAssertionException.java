/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

/**
 * TODO:
 */
class FirefoxSyncAssertionException extends Exception {
    FirefoxSyncAssertionException(final String message) { super(message); }
    FirefoxSyncAssertionException(final String message, final Throwable cause) { super(message, cause); }
}

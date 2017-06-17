/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

/**
 * An Exception indicating an Assertion has failed. Semantically equivalent to {@link AssertionError},
 * this is useful when a callback only takes Exception (and its extensions).
 */
class FirefoxSyncAssertionException extends Exception {
    FirefoxSyncAssertionException(final String message) { super(message); }
    FirefoxSyncAssertionException(final String message, final Throwable cause) { super(message, cause); }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts;

import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;

/**
 * A collection of functions for operating on a FirefoxAccount.
 */
public class FirefoxAccountUtils {
    private FirefoxAccountUtils() {}

    public static boolean isMarried(final State accountState) {
        return accountState.getStateLabel() == StateLabel.Married;
    }

    /**
     * Asserts Married and returns the account state casted to Married.
     *
     * @return the given account state casted to {@link Married}.
     * @throws IllegalArgumentException if the account is not in the Married state.
     */
    public static Married getMarried(final State accountState) {
        if (!isMarried(accountState)) {
            throw new IllegalArgumentException("Expected account to be married.");
        }
        return (Married) accountState;
    }
}

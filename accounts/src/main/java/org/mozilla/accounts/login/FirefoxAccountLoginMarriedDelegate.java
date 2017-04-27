/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.login;

import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;

/**
 * A login delegate implementation that provides separates callbacks for Married or not Married account state.
 *
 * We use this abstraction because all we really care about is if the user is in the Married state
 * or not.
 */
public class FirefoxAccountLoginMarriedDelegate extends FirefoxAccountLoginDefaultDelegate {

    private final MarriedCallback callback;

    public interface MarriedCallback {
        void onNotMarried(FirefoxAccount account, State notMarriedState);
        void onMarried(FirefoxAccount updatedAccount, Married marriedState);
    }

    public FirefoxAccountLoginMarriedDelegate(final FirefoxAccount account, final MarriedCallback callback) {
        super(account);
        this.callback = callback;
    }

    @Override
    public void handleFinal(final State finalState) {
        if (finalState.getStateLabel() != StateLabel.Married) {
            Log.w(LOGTAG, "Unable to get to Married state.");
            callback.onNotMarried(account, finalState);
        } else {
            final Married marriedState = (Married) finalState;
            final FirefoxAccount marriedAccount = account.withNewState(marriedState);
            callback.onMarried(marriedAccount, marriedState);
        }
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.gecko.fxa.login.StateFactory;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.sync.impl.FirefoxAccountShared;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxAccountEndpointConfig;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * A store for a {@link FirefoxAccount}.
 *
 * This is a quick, temporary implementation to use for development. The real implementation should encrypt personal user
 * data, such as email, uid, and tokens. Also, the way the account is used may change the store & its API so I'm delaying
 * those decisions for now.
 *
 * TODO: replace me. See rules ^.
 * Note: iOS is encrypted with iOS keystore (uses pin from phone login) but Android is not.
 */
class FirefoxAccountSharedPrefsStore { //TODO: Maybe FirefoxAccountSession. Or FirefoxSyncSession.

    private static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    private static final String DEFAULT_STORE_NAME = "FirefoxAccountSharedPrefsStore";
    private static final String PREFS_BRANCH_PREFIX = "org.mozilla.accounts.";
    private static final int STORE_VERSION = 1; // for wiggle room with potential future revisions.

    private final SharedPreferences sharedPrefs;

    /** Create a FirefoxAccountSharedPrefsStore with the default name. */
    FirefoxAccountSharedPrefsStore(final Context context) {
        this(context, DEFAULT_STORE_NAME);
    }

    // Untested, but in theory this should allow support for multiple accounts.
    private FirefoxAccountSharedPrefsStore(final Context context, final String storeName) {
        this.sharedPrefs = context.getSharedPreferences(getPrefsBranch(storeName), 0);
    }

    /** Saves a FirefoxAccount to be restored with {@link #loadFirefoxAccount()}. */
    @AnyThread
    void saveFirefoxAccount(@NonNull final FirefoxAccount account) {
        sharedPrefs.edit()
                .putInt("version", STORE_VERSION)
                .putString("email", account.email)
                .putString("uid", account.uid)
                .putString("state-label", account.accountState.getStateLabel().name())
                .putString("state-json", account.accountState.toJSONObject().toJSONString())

                // Future builds can change the endpoints in their config so we only store the label
                // so we can pull in the latest endpoints.
                .putString("config-label", account.endpointConfig.label)
                .apply();
    }

    /** @return a FirefoxAccount or null on error. */
    @Nullable
    @AnyThread
    FirefoxAccount loadFirefoxAccount() {
        // TODO: helper method for readability.
        final State state;
        try {
            final StateLabel stateLabel = State.StateLabel.valueOf(sharedPrefs.getString("state-label", null)); // TODO: will throw.
            final ExtendedJSONObject stateJSON = new ExtendedJSONObject(sharedPrefs.getString("state-json", null));
            state = StateFactory.fromJSONObject(stateLabel, stateJSON);
        } catch (final NoSuchAlgorithmException | IOException | NonObjectJSONException | InvalidKeySpecException e) {
            Log.w(LOGTAG, "Unable to restore account state.");
            return null;
        }

        final String endpointConfigLabel = sharedPrefs.getString("config-label", "");
        final FirefoxAccountEndpointConfig endpointConfig;
        switch (endpointConfigLabel) { // We should probably use enums over Strings, but it's not worth the time.
            case "StableDev": endpointConfig = FirefoxAccountEndpointConfig.getStableDev(); break;
            case "LatestDev": endpointConfig = FirefoxAccountEndpointConfig.getLatestDev(); break;
            case "Stage": endpointConfig = FirefoxAccountEndpointConfig.getStage(); break;
            case "Production": endpointConfig = FirefoxAccountEndpointConfig.getProduction(); break;
            default: Log.w(LOGTAG, "Unable to restore endpoint config."); return null;
        }

        final String email = sharedPrefs.getString("email", null);
        final String uid = sharedPrefs.getString("uid", null);

        return new FirefoxAccount(email, uid, state, endpointConfig);
    }

    private static String getPrefsBranch(final String storeName) { return PREFS_BRANCH_PREFIX + storeName; };
}

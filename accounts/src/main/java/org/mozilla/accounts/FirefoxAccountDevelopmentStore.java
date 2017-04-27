/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.gecko.fxa.login.StateFactory;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
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
public class FirefoxAccountDevelopmentStore {

    private static final String LOGTAG = FirefoxAccountShared.LOGTAG;

    private static final String DEFAULT_STORE_NAME = "FirefoxAccountDevelopmentStore";
    private static final String PREFS_BRANCH_PREFIX = "org.mozilla.accounts.";

    private final String storeName;
    private final WeakReference<Context> contextWeakReference;

    /** Create a FirefoxAccountDevelopmentStore with the default name. */
    public FirefoxAccountDevelopmentStore(final Context context) {
        this(context, DEFAULT_STORE_NAME);
    }

    public FirefoxAccountDevelopmentStore(final Context context, final String storeName) {
        this.contextWeakReference = new WeakReference<Context>(context);
        this.storeName = storeName;
    }

    public void saveFirefoxAccount(final FirefoxAccount account) {
        final Context context = contextWeakReference.get();
        if (context == null) {
            // TODO: how handle? Can take application context instead?
            Log.w(LOGTAG, "Context unexpectedly null when saving account.");
            return;
        }

        final SharedPreferences prefs = context.getSharedPreferences(getPrefsBranch(), 0);
        prefs.edit()
                .putString("email", account.email)
                .putString("uid", account.uid)
                .putString("state-label", account.accountState.getStateLabel().name())
                .putString("state-json", account.accountState.toJSONObject().toJSONString())
                .putString("config-label", account.endpointConfig.label)
                .putString("config-authServer", account.endpointConfig.authServerURL.toString())
                .putString("config-oauthServer", account.endpointConfig.oauthServerURL.toString())
                .putString("config-profile", account.endpointConfig.profileServerURL.toString())
                .putString("config-signIn", account.endpointConfig.signInURL.toString())
                .putString("config-settings", account.endpointConfig.settingsURL.toString())
                .putString("config-forceAuth", account.endpointConfig.forceAuthURL.toString())
                .apply();
    }

    // TODO: docs, return null when empty.
    public FirefoxAccount loadFirefoxAccount() {
        final Context context = contextWeakReference.get();
        if (context == null) {
            // TODO: how handle?
            return null;
        }

        final SharedPreferences prefs = context.getSharedPreferences(getPrefsBranch(), 0);

        // TODO: helper method for readability.
        final State state;
        try {
            final StateLabel stateLabel = State.StateLabel.valueOf(prefs.getString("state-label", null)); // TODO: will throw.
            final ExtendedJSONObject stateJSON = new ExtendedJSONObject(prefs.getString("state-json", null));
            state = StateFactory.fromJSONObject(stateLabel, stateJSON);
        } catch (NoSuchAlgorithmException | IOException | NonObjectJSONException | InvalidKeySpecException e) {
            Log.w(LOGTAG, "Unable to restore account state.");
            return null;
        }

        // TODO: doc: we don't save all endpoints because individual ones can change in later build.
        // TODO: don't use Strings: StateFactory w/ enum or similar.
        final String endpointConfigLabel = prefs.getString("config-label", null);
        final FirefoxAccountEndpointConfig endpointConfig;
        switch (endpointConfigLabel) {
            case "StableDev": endpointConfig = FirefoxAccountEndpointConfig.getStableDev(); break;
            case "LatestDev": endpointConfig = FirefoxAccountEndpointConfig.getLatestDev(); break;
            case "Stage": endpointConfig = FirefoxAccountEndpointConfig.getStage(); break;
            case "Production": endpointConfig = FirefoxAccountEndpointConfig.getProduction(); break;
            default: Log.w(LOGTAG, "Unable to restore endpoint config."); return null;
        }

        final String email = prefs.getString("email", null);
        final String uid = prefs.getString("uid", null);

        return new FirefoxAccount(email, uid, state, endpointConfig);
    }

    private String getPrefsBranch() { return PREFS_BRANCH_PREFIX + storeName; };
}

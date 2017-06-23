/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.State.StateLabel;
import org.mozilla.gecko.fxa.login.StateFactory;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.fxa_data.impl.FirefoxDataShared;
import org.mozilla.fxa_data.impl.FirefoxAccount;
import org.mozilla.fxa_data.impl.FirefoxAccountEndpointConfig;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.mozilla.fxa_data.impl.FirefoxAccountEndpointConfig.LABEL_LATEST_DEV;
import static org.mozilla.fxa_data.impl.FirefoxAccountEndpointConfig.LABEL_PRODUCTION;
import static org.mozilla.fxa_data.impl.FirefoxAccountEndpointConfig.LABEL_STABLE_DEV;
import static org.mozilla.fxa_data.impl.FirefoxAccountEndpointConfig.LABEL_STAGE;

/**
 * A store for a {@link FirefoxAccountSession} based on {@link SharedPreferences}.
 *
 * This store is <b>unencrypted</b>. For future encryption discussion, see issue #5.
 *
 * This class is thread-safe in that no inconsistent data will be shown but call order (i.e. a load called
 * before a save will return the data before the save) is not guaranteed.
 */
class FirefoxAccountSessionSharedPrefsStore {

    private static final String LOGTAG = FirefoxDataShared.LOGTAG;

    private static final String DEFAULT_STORE_NAME = "FirefoxAccountSessionSharedPrefsStore";
    private static final String PREFS_BRANCH_PREFIX = "org.mozilla.accounts.";
    private static final int STORE_VERSION = 1; // for wiggle room with potential future revisions.

    private static final String KEY_VERSION = "version";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_UID = "uid";
    private static final String KEY_STATE_LABEL = "state-label";
    private static final String KEY_STATE_JSON = "state-json";
    private static final String KEY_ENDPOINT_CONFIG_LABEL = "endpoint-config-label";
    private static final String[] KEYS_TO_CLEAR_ON_ACCOUNT_REMOVAL = new String[] {
            KEY_VERSION,
            KEY_EMAIL,
            KEY_UID,
            KEY_STATE_LABEL,
            KEY_STATE_JSON,
            KEY_ENDPOINT_CONFIG_LABEL,
    };
    private static final String KEY_APPLICATION_NAME = "application-name";

    private final SharedPreferences sharedPrefs;

    /** Create a FirefoxAccountSessionSharedPrefsStore with the default name. */
    FirefoxAccountSessionSharedPrefsStore(final Context context) {
        this(context, DEFAULT_STORE_NAME);
    }

    // Untested, but in theory this should allow support for multiple accounts.
    private FirefoxAccountSessionSharedPrefsStore(final Context context, final String storeName) {
        this.sharedPrefs = context.getSharedPreferences(getPrefsBranch(storeName), 0);
    }

    private static String getPrefsBranch(final String storeName) { return PREFS_BRANCH_PREFIX + storeName; };

    /** Saves a {@link FirefoxAccountSession} to be restored with {@link #loadSession()}. */
    @AnyThread
    void saveSession(@NonNull final FirefoxAccountSession session) {
        final FirefoxAccount account = session.firefoxAccount;
        sharedPrefs.edit()
                .putInt(KEY_VERSION, STORE_VERSION)
                .putString(KEY_EMAIL, account.email)
                .putString(KEY_UID, account.uid)
                .putString(KEY_STATE_LABEL, account.accountState.getStateLabel().name())
                .putString(KEY_STATE_JSON, account.accountState.toJSONObject().toJSONString())

                // Future builds can change the endpoints in their config so we only store the label
                // so we can pull in the latest endpoints.
                .putString(KEY_ENDPOINT_CONFIG_LABEL, account.endpointConfig.label)
                .putString(KEY_APPLICATION_NAME, session.applicationName)
                .apply();
    }

    /**
     * @throws FailedToLoadSessionException if we're unable to load the account.
     * @return a FirefoxAccount.
     */
    @NonNull
    @AnyThread
    FirefoxAccountSession loadSession() throws FailedToLoadSessionException {
        if (sharedPrefs.getInt(KEY_VERSION, -1) < 0) { throw new FailedToLoadSessionException("account does not exist"); }

        final State state;
        try {
            final StateLabel stateLabel = State.StateLabel.valueOf(sharedPrefs.getString(KEY_STATE_LABEL, null));
            final ExtendedJSONObject stateJSON = new ExtendedJSONObject(sharedPrefs.getString(KEY_STATE_JSON, null));
            state = StateFactory.fromJSONObject(stateLabel, stateJSON);
        } catch (final NoSuchAlgorithmException | IOException | NonObjectJSONException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new FailedToLoadSessionException("unable to restore account state", e);
        }

        final String endpointConfigLabel = sharedPrefs.getString(KEY_ENDPOINT_CONFIG_LABEL, "");
        final FirefoxAccountEndpointConfig endpointConfig;
        switch (endpointConfigLabel) { // We should probably use enums over Strings, but it wasn't worth my time.
            case LABEL_STABLE_DEV: endpointConfig = FirefoxAccountEndpointConfig.getStableDev(); break;
            case LABEL_LATEST_DEV: endpointConfig = FirefoxAccountEndpointConfig.getLatestDev(); break;
            case LABEL_STAGE: endpointConfig = FirefoxAccountEndpointConfig.getStage(); break;
            case LABEL_PRODUCTION: endpointConfig = FirefoxAccountEndpointConfig.getProduction(); break;
            default: throw new FailedToLoadSessionException("unable to restore account - unknown endpoint label: " + endpointConfigLabel);
        }

        final String email = sharedPrefs.getString(KEY_EMAIL, null);
        final String uid = sharedPrefs.getString(KEY_UID, null);
        final FirefoxAccount firefoxAccount = new FirefoxAccount(email, uid, state, endpointConfig);

        final String applicationName = sharedPrefs.getString(KEY_APPLICATION_NAME, null);

        return new FirefoxAccountSession(firefoxAccount, applicationName);
    }

    /** Removes any saved {@link FirefoxAccountSession}. */
    @AnyThread
    void deleteStoredSession() {
        // Alternatively, we could call `sharedPrefs.edit().clear()`, but that's fragile, e.g. if we
        // started to store other metadata in here we wouldn't want to clear on account removal.
        final SharedPreferences.Editor editor = sharedPrefs.edit();
        for (final String key : KEYS_TO_CLEAR_ON_ACCOUNT_REMOVAL) {
            editor.remove(key);
        }
        editor.apply();
    }

    static class FailedToLoadSessionException extends Exception {
        private static final String MESSAGE_PREFIX = "loadSession: ";
        FailedToLoadSessionException(final String message) { super(MESSAGE_PREFIX + message); }
        FailedToLoadSessionException(final String message, final Throwable cause) { super(MESSAGE_PREFIX + message, cause); }
    }
}

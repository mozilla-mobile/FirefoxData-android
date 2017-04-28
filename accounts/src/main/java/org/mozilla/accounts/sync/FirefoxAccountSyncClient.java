/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import android.content.Context;
import android.content.SharedPreferences;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.sync.FirefoxAccountSyncTokenAccessor.TokenCallback;
import org.mozilla.accounts.sync.callbacks.BaseSyncCallback;
import org.mozilla.accounts.sync.callbacks.SyncHistoryCallback;
import org.mozilla.gecko.background.fxa.SkewHandler;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class FirefoxAccountSyncClient {

    private static final String SYNC_CONFIG_SHARED_PREFS_NAME = "org.mozilla.accounts.FirefoxSyncClient.syncConfig";

    private final FirefoxAccount account;

    public FirefoxAccountSyncClient(final FirefoxAccount account) {
        this.account = account;
    }


    public void getCollectionInfo() {
        // TODO
        //final URI uri = new URI(storageServerURI.toString() + "/info/collections");
    }

    public void getHistory(final Context context, final int limit, final SyncHistoryCallback callback) {
        // We make GetResourceTokenCallback non-anonymous to avoid leaking the Context.
        final SyncClientHistoryResourceDelegate resourceDelegate = new SyncClientHistoryResourceDelegate(1000, callback);
        getSyncToken(context, new GetResourceTokenCallback(context, resourceDelegate, callback));
    }

    private void getSyncToken(final Context context, final TokenCallback callback) {
        // TODO: we should store on-disk/in-memory & access that value. Should this be separate cache class?
        FirefoxAccountSyncTokenAccessor.get(context, account, callback);
    }

    private static class GetResourceTokenCallback implements TokenCallback {
        private final WeakReference<Context> contextWeakReference;
        private final SyncClientBaseResourceDelegate syncClientResourceDelegate;
        private final BaseSyncCallback callback;

        private GetResourceTokenCallback(final Context context, final SyncClientBaseResourceDelegate syncClientResourceDelegate,
                final BaseSyncCallback callback) {
            this.contextWeakReference = new WeakReference<>(context);
            this.syncClientResourceDelegate = syncClientResourceDelegate;
            this.callback = callback;
        }

        @Override
        public void onError(final Exception e) { callback.onError(e); }

        @Override
        public void onTokenReceived(final FirefoxAccount updatedAccount, final TokenServerToken token) {
            final Context context = contextWeakReference.get();
            if (context == null) {
                onError(new Exception("Received token & unable to continue: context is null"));
                return;
            }

            try {
                final URI storageServerURI = new URI(token.endpoint);
                final AuthHeaderProvider authHeaderProvider = getAuthHeaderProvider(token, storageServerURI);
                syncClientResourceDelegate.authHeaderProvider = authHeaderProvider;

                final SharedPreferences sharedPrefs = context.getSharedPreferences(SYNC_CONFIG_SHARED_PREFS_NAME, Utils.SHARED_PREFERENCES_MODE);
                final SyncConfiguration syncConfig = new SyncConfiguration(token.uid, authHeaderProvider, sharedPrefs, ((Married) updatedAccount.accountState).getSyncKeyBundle());
                syncConfig.setClusterURL(storageServerURI);
                syncClientResourceDelegate.syncConfig = syncConfig;

                final URI uri = new URI(storageServerURI.toString() + syncClientResourceDelegate.getResourcePath());
                final BaseResource resource = new BaseResource(uri);
                resource.delegate = syncClientResourceDelegate;
                resource.get();
            } catch (final NoSuchAlgorithmException | UnsupportedEncodingException | URISyntaxException | InvalidKeyException e) {
                callback.onError(e);
            }
        }

        private AuthHeaderProvider getAuthHeaderProvider(final TokenServerToken token, final URI storageServerURI) throws UnsupportedEncodingException {
            // We expect Sync to upload large sets of records. Calculating the
            // payload verification hash for these record sets could be expensive,
            // so we explicitly do not send payload verification hashes to the
            // Sync storage endpoint.
            final boolean includePayloadVerificationHash = false;

            // We compute skew over time using SkewHandler. This yields an unchanging
            // skew adjustment that the HawkAuthHeaderProvider uses to adjust its
            // timestamps. Eventually we might want this to adapt within the scope of a
            // global session.
            final String storageHostname = storageServerURI.getHost();
            final SkewHandler storageServerSkewHandler = SkewHandler.getSkewHandlerForHostname(storageHostname);
            final long storageServerSkew = storageServerSkewHandler.getSkewInSeconds();

            return new HawkAuthHeaderProvider(token.id, token.key.getBytes("UTF-8"), includePayloadVerificationHash,
                    storageServerSkew);
        }
    }
}

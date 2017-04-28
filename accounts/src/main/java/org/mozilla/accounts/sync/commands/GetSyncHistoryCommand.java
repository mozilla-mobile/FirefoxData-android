/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync.commands;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.accounts.sync.FirefoxAccountSyncConfig;
import org.mozilla.accounts.sync.callbacks.SyncHistoryCallback;
import org.mozilla.gecko.background.fxa.SkewHandler;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import static org.mozilla.accounts.sync.FirefoxAccountSyncClient.SYNC_CONFIG_SHARED_PREFS_NAME;

// TODO: docs.
// TODO: we don't want to precommand because we'll throw instead of callback on error.
public class GetSyncHistoryCommand extends SyncClientPreCommand {
    private final SyncClientHistoryResourceDelegate syncClientResourceDelegate;
    private final SyncHistoryCallback callback;

    public GetSyncHistoryCommand(final int itemLimit, final SyncHistoryCallback callback) {
        this.syncClientResourceDelegate = new SyncClientHistoryResourceDelegate(itemLimit, callback);
        this.callback = callback;
    }

    // TODO: no return type, can't throw exception. exceptions are sucking.
    @Override
    public FirefoxAccountSyncConfig call(final FirefoxAccountSyncConfig syncConfig) throws Exception {
        final Context context = syncConfig.contextWeakReference.get();
        if (context == null) {
            callback.onError(new Exception("Received token & unable to continue: context is null"));
            return null;
        }

        // TODO: set up code is shared.
        try {
            final URI storageServerURI = new URI(syncConfig.token.endpoint);
            final AuthHeaderProvider authHeaderProvider = getAuthHeaderProvider(syncConfig.token, storageServerURI);
            syncClientResourceDelegate.authHeaderProvider = authHeaderProvider;

            final SharedPreferences sharedPrefs = context.getSharedPreferences(SYNC_CONFIG_SHARED_PREFS_NAME, Utils.SHARED_PREFERENCES_MODE);
            // todo: OLD NECESSARY?
            final SyncConfiguration oldStyleSyncConfig = new SyncConfiguration(syncConfig.token.uid, authHeaderProvider, sharedPrefs,
                    ((Married) syncConfig.account.accountState).getSyncKeyBundle());
            oldStyleSyncConfig.setClusterURL(storageServerURI);
            syncClientResourceDelegate.syncConfig = oldStyleSyncConfig; // todo: name.

            final URI uri = new URI(storageServerURI.toString() + syncClientResourceDelegate.getResourcePath());
            final BaseResource resource = new BaseResource(uri);
            resource.delegate = syncClientResourceDelegate;
            resource.get();
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException | URISyntaxException | InvalidKeyException e) {
            callback.onError(e);
        }
        return null;
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

    private static class SyncClientHistoryResourceDelegate extends SyncClientBaseResourceDelegate {
        private final int itemLimit;
        private final SyncHistoryCallback callback;

        public SyncClientHistoryResourceDelegate(final int itemLimit, final SyncHistoryCallback callback) {
            this.itemLimit = itemLimit;
            this.callback = callback;
        }

        @Override public void handleError(final Exception e) { callback.onError(e); }

        @Override public String getResourcePath() { return "/storage/history?full=1&limit=" + itemLimit; }

        @Override
        public void handleResponse(final HttpResponse response) {
            Log.d(LOGTAG, response.toString());
            Scanner s = null;
            try {
                s = new Scanner(response.getEntity().getContent()).useDelimiter("\\A");
            } catch (IOException e) {
                e.printStackTrace();
            }
            String result = s.hasNext() ? s.next() : "";
            final JSONArray array;
            final HistoryRecordFactory fact = new HistoryRecordFactory();
            try {
                final KeyBundle bundle = syncConfig.getCollectionKeys().keyBundleForCollection("history"); // TODO: kill syncconfig.
                array = new JSONArray(result);
                for (int i = 0; i < array.length(); ++i) {
                    final JSONObject obj = array.getJSONObject(i);
                    final Record record = new HistoryRecord(obj.getString("id"));
                    final CryptoRecord crecord = new CryptoRecord(record);
                    crecord.payload = new ExtendedJSONObject(obj.getString("payload"));
                    crecord.setKeyBundle(bundle);
                    crecord.decrypt();
                    final HistoryRecord hrecord = (HistoryRecord) fact.createRecord(crecord);
                    Log.d(LOGTAG, hrecord.histURI);
                }
            } catch (JSONException | NonObjectJSONException | IOException | CryptoException | NoCollectionKeysSetException e) {
                e.printStackTrace();
            }
            Log.d(LOGTAG, result);
        }
    }
}

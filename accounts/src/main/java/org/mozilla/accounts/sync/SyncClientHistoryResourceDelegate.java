/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.accounts.sync.callbacks.SyncHistoryCallback;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;

import java.io.IOException;
import java.util.Scanner;

/**
 * A delegate for accessing a sync client's history.
 */
class SyncClientHistoryResourceDelegate extends SyncClientBaseResourceDelegate {
    private final int itemLimit;
    private final SyncHistoryCallback callback;

    SyncClientHistoryResourceDelegate(final int itemLimit, final SyncHistoryCallback callback) {
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
            final KeyBundle bundle = syncConfig.getCollectionKeys().keyBundleForCollection("history");
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

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import android.support.annotation.Nullable;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.fxa_data.FirefoxSyncException;
import org.mozilla.fxa_data.impl.FirefoxSyncRequestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of internal static functions for the Sync package.
 */
class FirefoxSyncUtils {
    private FirefoxSyncUtils() {}

    /**
     * Convenience method to make a get request to the given collection.
     *
     * @param collectionArgs The arguments for this get request. Note that "full=1" is included as a default arg.
     * @param delegate The callback for the request.
     */
    static void makeGetRequestForCollection(final FirefoxSyncConfig syncConfig, final String collectionName,
            @Nullable final Map<String, String> collectionArgs, final SyncBaseResourceDelegate delegate) throws FirefoxSyncException {
        final Map<String, String> allArgs = getDefaultArgs();
        if (collectionArgs != null) { allArgs.putAll(collectionArgs); }

        final URI uri;
        try {
            uri = FirefoxSyncRequestUtils.getCollectionURI(syncConfig.token, collectionName, null, allArgs);
        } catch (final URISyntaxException e) {
            // This is either programmer error (we incorrectly combined the components of the URI) or the
            // server passed us an invalid uri (in the token). Given that if the code worked when we wrote it,
            // it's most likely the latter, we provide that as the failure response.
            throw new FirefoxSyncException("Unable to create valid collection URI for request", e);
        }
        final BaseResource resource = new BaseResource(uri);
        resource.delegate = delegate;
        resource.get();
    }

    private static Map<String, String> getDefaultArgs() {
        final Map<String, String> args = new HashMap<String, String>();
        args.put("full", "1"); // get full data, not just IDs.
        return args;
    }
}

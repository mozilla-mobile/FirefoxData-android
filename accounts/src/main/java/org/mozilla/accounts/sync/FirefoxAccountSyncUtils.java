/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.sync;

import android.support.annotation.NonNull;
import org.mozilla.gecko.background.fxa.SkewHandler;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A group of util functions for the Sync servers.
 */
public class FirefoxAccountSyncUtils {
    private FirefoxAccountSyncUtils() {}

    public static URI getServerURI(@NonNull final TokenServerToken token) throws URISyntaxException {
        return new URI(token.endpoint);
    }

    public static URI getServerStorageURI(@NonNull final TokenServerToken token) throws URISyntaxException {
        return new URI(getServerURI(token).toString() + "/storage");
    }

    /**
     * Gets the URI associated with the given collection & id. Equivalent to
     * {@link org.mozilla.gecko.sync.GlobalSession#wboURI(java.lang.String, java.lang.String)}.
     */
    public static URI getCollectionURI(final TokenServerToken token, final String collection, final String id) throws URISyntaxException {
        return new URI(getServerStorageURI(token).toString() + "/" + collection + "/" + id);
    }

    public static HawkAuthHeaderProvider getAuthHeaderProvider(final TokenServerToken token) throws UnsupportedEncodingException, URISyntaxException {
        // We expect Sync to upload large sets of records. Calculating the
        // payload verification hash for these record sets could be expensive,
        // so we explicitly do not send payload verification hashes to the
        // Sync storage endpoint.
        final boolean includePayloadVerificationHash = false;

        // We compute skew over time using SkewHandler. This yields an unchanging
        // skew adjustment that the HawkAuthHeaderProvider uses to adjust its
        // timestamps. Eventually we might want this to adapt within the scope of a
        // global session.
        final URI storageServerURI = FirefoxAccountSyncUtils.getServerURI(token);
        final String storageHostname = storageServerURI.getHost();
        final SkewHandler storageServerSkewHandler = SkewHandler.getSkewHandlerForHostname(storageHostname);
        final long storageServerSkew = storageServerSkewHandler.getSkewInSeconds();

        return new HawkAuthHeaderProvider(token.id, token.key.getBytes("UTF-8"), includePayloadVerificationHash,
                storageServerSkew);
    }
}

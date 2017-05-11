/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync.commands;

import android.support.annotation.Nullable;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.sync.sync.FirefoxAccountSyncUtils;
import org.mozilla.util.ChainableCallable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Container file for SyncClientCommand classes. */
public class SyncClientCommands {
    private SyncClientCommands() {}

    private static final long ASYNC_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60); // todo

    /** A base-class for commands accessing collections from sync. */
    public static abstract class SyncClientCollectionCommand<T> extends ChainableCallable.AsyncChainableCallable<FirefoxAccountSyncConfig, List<T>> {

        public SyncClientCollectionCommand() { super(ASYNC_TIMEOUT_MILLIS); }

        /**
         * Convenience method to make a get request to the given collection.
         *
         * @param collectionArgs The arguments for this get request. Note that "full=1" is included as a default arg.
         * @param delegate The callback for the request.
         */
        protected static void makeGetRequestForCollection(final FirefoxAccountSyncConfig syncConfig, final String collectionName,
                @Nullable final Map<String, String> collectionArgs, final SyncClientBaseResourceDelegate delegate) throws URISyntaxException {
            final Map<String, String> allArgs = getDefaultArgs();
            if (collectionArgs != null) { allArgs.putAll(collectionArgs); }

            final URI uri = FirefoxAccountSyncUtils.getCollectionURI(syncConfig.token, collectionName, null, allArgs);
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

    /** A helper for handling pre-commands that begin an async request and need to block until completion. */
    public static abstract class SyncClientAsyncPreCommand extends ChainableCallable.AsyncChainableCallable<FirefoxAccountSyncConfig, FirefoxAccountSyncConfig> {
        protected SyncClientAsyncPreCommand() { super(ASYNC_TIMEOUT_MILLIS); }
    }
}

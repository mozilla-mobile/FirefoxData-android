/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import org.mozilla.sync.login.InternalFirefoxSyncLoginManagerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A collection of static functions with entry points to Firefox Sync operations.
 */
public class FirefoxSync {
    private FirefoxSync() {}

    private static final Set<WeakReference<Context>> seenContextReferences = new HashSet<>();

    /**
     * Gets a FirefoxSyncLoginManager, which grants access a Firefox Account user's Sync information.
     *
     * This function is intended to be called in initialization (such as {@link android.app.Activity#onCreate(Bundle)})
     * because it can only be called with a specific Context instance once.
     *
     * @param context The Context from which this LoginManager is being accessed.
     * @return A FirefoxSyncLoginManager.
     */
    public static FirefoxSyncLoginManager getLoginManager(@NonNull final Context context) {
        // We assume the internal implementation may return a new instance, rather than a singleton,
        // because the Context can change. However, because of this, a library user could misuse the
        // library by registering their callback and calling `onActivityResult` with two separate
        // instances. To prevent this, we ensure we've only seen a Context once.
        assertContextNotSeenAndUpdateSeen(context); // todo: this sucks - can we fix API instead?

        return InternalFirefoxSyncLoginManagerFactory.internalGetLoginManager(context);
    }

    private static void assertContextNotSeenAndUpdateSeen(@NonNull final Context context) {
        // We can't store references to Contexts without memory leaking so we store WeakReferences.
        // As such, we also will purge any dead weak references in our iteration.
        final List<WeakReference<Context>> nullContextReferences = new ArrayList<>(seenContextReferences.size());
        for (final WeakReference<Context> seenContextReference : seenContextReferences) {
            final Context seenContext = seenContextReference.get();
            if (seenContext == null) {
                nullContextReferences.add(seenContextReference);
                continue;
            }

            if (seenContext.equals(context)) {
                throw new IllegalStateException("getLoginManager was unexpectedly called twice with the same Context! " +
                        "Please keep a reference to the returned FirefoxSyncLoginManager instead.");
            }
        }

        seenContextReferences.removeAll(nullContextReferences);
        seenContextReferences.add(new WeakReference<>(context));
    }
}

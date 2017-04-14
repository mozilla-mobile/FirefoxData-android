/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import android.content.Context;
import android.support.annotation.RawRes;

import java.io.IOException;
import java.io.InputStream;

public class ResourcesUtil {

    public static String getStringFromRawRes(final Context context, @RawRes final int rawResource) throws IOException {
        final InputStream is = context.getResources().openRawResource(rawResource);
        return FileUtil.readStringFromInputStreamAndCloseStream(is, 4048);
    }

    public static String getStringFromRawResUnsafe(final Context context, @RawRes final int rawResource) {
        try {
            return getStringFromRawRes(context, rawResource);
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to String from resource: " + rawResource); // don't log exception to avoid leaking user data.
        }
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import java.io.Closeable;
import java.io.IOException;

// from m-c.
public class IOUtil {

    private IOUtil() {}

    public static void safeStreamClose(Closeable stream) {
        try {
            if (stream != null)
                stream.close();
        } catch (IOException e) { }
    }
}

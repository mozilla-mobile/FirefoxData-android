/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

// from m-c.
public class FileUtil {

    private FileUtil() {}

    /**
     * A generic solution to read from an input stream in UTF-8. This function will read from the stream until it
     * is finished and close the stream - this is necessary to close the wrapping resources.
     *
     * For a higher-level method, see {@link #readStringFromFile(File)}.
     *
     * Since this is generic, it may not be the most performant for your use case.
     *
     * @param bufferSize Size of the underlying buffer for read optimizations - must be > 0.
     */
    public static String readStringFromInputStreamAndCloseStream(final InputStream inputStream, final int bufferSize)
            throws IOException {
        if (bufferSize <= 0) {
            // Safe close: it's more important to alert the programmer of
            // their error than to let them catch and continue on their way.
            IOUtil.safeStreamClose(inputStream);
            throw new IllegalArgumentException("Expected buffer size larger than 0. Got: " + bufferSize);
        }

        final StringBuilder stringBuilder = new StringBuilder(bufferSize);
        final InputStreamReader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        try {
            int charsRead;
            final char[] buffer = new char[bufferSize];
            while ((charsRead = reader.read(buffer, 0, bufferSize)) != -1) {
                stringBuilder.append(buffer, 0, charsRead);
            }
        } finally {
            reader.close();
        }
        return stringBuilder.toString();
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Utils {

  private static final String LOG_TAG = "Utils";

  private static final SecureRandom sharedSecureRandom = new SecureRandom();

  // See <http://developer.android.com/reference/android/content/Context.html#getSharedPreferences%28java.lang.String,%20int%29>
  public static final int SHARED_PREFERENCES_MODE = 0;

  public static String generateGuid() {
    byte[] encodedBytes = Base64.encodeBase64(generateRandomBytes(9), false);
    return new String(encodedBytes, StringUtils.UTF_8).replace("+", "-").replace("/", "_");
  }

  /**
   * Helper to generate secure random bytes.
   *
   * @param length
   *        Number of bytes to generate.
   */
  public static byte[] generateRandomBytes(int length) {
    byte[] bytes = new byte[length];
    sharedSecureRandom.nextBytes(bytes);
    return bytes;
  }

  /**
   * Helper to convert a byte array to a hex-encoded string
   */
  public static String byte2Hex(final byte[] b) {
    return byte2Hex(b, 2 * b.length);
  }

  public static String byte2Hex(final byte[] b, int hexLength) {
    final StringBuilder hs = new StringBuilder(Math.max(2*b.length, hexLength));
    String stmp;

    for (int n = 0; n < hexLength - 2*b.length; n++) {
      hs.append("0");
    }

    for (int n = 0; n < b.length; n++) {
      stmp = Integer.toHexString(b[n] & 0XFF);

      if (stmp.length() == 1) {
        hs.append("0");
      }
      hs.append(stmp);
    }

    return hs.toString();
  }

  public static byte[] concatAll(byte[] first, byte[]... rest) {
    int totalLength = first.length;
    for (byte[] array : rest) {
      totalLength += array.length;
    }

    byte[] result = new byte[totalLength];
    int offset = first.length;

    System.arraycopy(first, 0, result, 0, offset);

    for (byte[] array : rest) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

  public static byte[] hex2Byte(String str, int byteLength) {
    byte[] second = hex2Byte(str);
    if (second.length >= byteLength) {
      return second;
    }
    // New Java arrays are zeroed:
    // http://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.12.5
    byte[] first = new byte[byteLength - second.length];
    return Utils.concatAll(first, second);
  }

  public static byte[] hex2Byte(String str) {
    if (str.length() % 2 == 1) {
      str = "0" + str;
    }

    byte[] bytes = new byte[str.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
    }
    return bytes;
  }

  // This lives until Bug 708956 lands, and we don't have to do it any more.
  public static long decimalSecondsToMilliseconds(String decimal) {
    try {
      return new BigDecimal(decimal).movePointRight(3).longValue();
    } catch (Exception e) {
      return -1;
    }
  }

  public static byte[] sha256(byte[] in)
      throws NoSuchAlgorithmException {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-256");
    return sha1.digest(in);
  }

  /**
   * Takes a URI, extracting URI components.
   * @param scheme the URI scheme on which to match.
   */
  @SuppressWarnings("deprecation")
  public static Map<String, String> extractURIComponents(String scheme, String uri) {
    if (uri.indexOf(scheme) != 0) {
      throw new IllegalArgumentException("URI scheme does not match: " + scheme);
    }

    // Do this the hard way to avoid taking a large dependency on
    // HttpClient or getting all regex-tastic.
    String components = uri.substring(scheme.length());
    HashMap<String, String> out = new HashMap<String, String>();
    String[] parts = components.split("&");
    for (int i = 0; i < parts.length; ++i) {
      String part = parts[i];
      if (part.length() == 0) {
        continue;
      }
      String[] pair = part.split("=", 2);
      switch (pair.length) {
      case 0:
        continue;
      case 1:
        out.put(URLDecoder.decode(pair[0]), null);
        break;
      case 2:
        out.put(URLDecoder.decode(pair[0]), URLDecoder.decode(pair[1]));
        break;
      }
    }
    return out;
  }

  // Because TextUtils.join is not stubbed.
  public static String toDelimitedString(String delimiter, Collection<? extends Object> items) {
    if (items == null || items.size() == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    int i = 0;
    int c = items.size();
    for (Object object : items) {
      sb.append(object.toString());
      if (++i < c) {
        sb.append(delimiter);
      }
    }
    return sb.toString();
  }

  public static void throwIfNull(Object... objects) {
    for (Object object : objects) {
      if (object == null) {
        throw new IllegalArgumentException("object must not be null");
      }
    }
  }

}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.KeyGenerator;

import org.mozilla.apache.commons.codec.binary.Base64;

public class KeyBundle {
    private static final String KEY_ALGORITHM_SPEC = "AES";
    private static final int    KEY_SIZE           = 256;

    private byte[] encryptionKey;
    private byte[] hmacKey;

    // These are the same for every sync key bundle.
    private static final byte[] EMPTY_BYTES      = {};
    private static final byte[] ENCR_INPUT_BYTES = {1};
    private static final byte[] HMAC_INPUT_BYTES = {2};

    public KeyBundle(byte[] encryptionKey, byte[] hmacKey) {
       this.setEncryptionKey(encryptionKey);
       this.setHMACKey(hmacKey);
    }

    /**
     * Make a KeyBundle with the specified base64-encoded keys.
     *
     * @return A KeyBundle with the specified keys.
     */
    public static KeyBundle fromBase64EncodedKeys(String base64EncryptionKey, String base64HmacKey) throws UnsupportedEncodingException {
      return new KeyBundle(Base64.decodeBase64(base64EncryptionKey.getBytes("UTF-8")),
                           Base64.decodeBase64(base64HmacKey.getBytes("UTF-8")));
    }

    /**
     * Make a KeyBundle with two random 256 bit keys (encryption and HMAC).
     *
     * @return A KeyBundle with random keys.
     */
    public static KeyBundle withRandomKeys() throws CryptoException {
      KeyGenerator keygen;
      try {
        keygen = KeyGenerator.getInstance(KEY_ALGORITHM_SPEC);
      } catch (NoSuchAlgorithmException e) {
        throw new CryptoException(e);
      }

      keygen.init(KEY_SIZE);
      byte[] encryptionKey = keygen.generateKey().getEncoded();
      byte[] hmacKey = keygen.generateKey().getEncoded();

      return new KeyBundle(encryptionKey, hmacKey);
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public byte[] getHMACKey() {
        return hmacKey;
    }

    public void setHMACKey(byte[] hmacKey) {
        this.hmacKey = hmacKey;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof KeyBundle)) {
        return false;
      }
      KeyBundle other = (KeyBundle) o;
      return Arrays.equals(other.encryptionKey, this.encryptionKey) &&
             Arrays.equals(other.hmacKey, this.hmacKey);
    }

    @Override
    public int hashCode() {
      throw new UnsupportedOperationException("No hashCode for KeyBundle.");
    }
}

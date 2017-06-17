/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync;

import org.mozilla.gecko.sync.net.SyncStorageResponse;

import android.content.SyncResult;

public class HTTPFailureException extends SyncException {
  private static final long serialVersionUID = -5415864029780770619L;
  public SyncStorageResponse response;

  public HTTPFailureException(SyncStorageResponse response) {
    this.response = response;
  }

  @Override
  public String toString() {
    String errorMessage;
    try {
      errorMessage = this.response.getErrorMessage();
    } catch (Exception e) {
      // Oh well.
      errorMessage = "[unknown error message]";
    }
    return "<HTTPFailureException " + this.response.getStatusCode() +
           " :: (" + errorMessage + ")>";
  }
}

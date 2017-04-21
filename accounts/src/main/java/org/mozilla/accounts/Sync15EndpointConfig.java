/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.URI;
import java.net.URISyntaxException;

public class Sync15EndpointConfig implements Parcelable {

    public final URI tokenServerURL;

    private Sync15EndpointConfig(final String tokenServerURL) {
        try {
            this.tokenServerURL = new URI(tokenServerURL);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Expected valid URI", e);
        }
    }

    public static Sync15EndpointConfig getProduction() {
        return new Sync15EndpointConfig(
                /* tokenServer */ "https://token.services.mozilla.com/1.0/sync/1.5"
        );
    }

    public static Sync15EndpointConfig getStage() {
        return new Sync15EndpointConfig(
                /* tokenServer */ "https://token.stage.mozaws.net/1.0/sync/1.5"
        );
    }

    // --- START PARCELABLE --- //
    @Override public int describeContents() { return 0; }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(tokenServerURL.toString());
    }

    public static Parcelable.Creator<Sync15EndpointConfig> CREATOR = new Parcelable.Creator<Sync15EndpointConfig>() {
        @Override
        public Sync15EndpointConfig createFromParcel(final Parcel source) {
            return new Sync15EndpointConfig(
                    source.readString()
            );
        }

        @Override public Sync15EndpointConfig[] newArray(final int size) { return new Sync15EndpointConfig[size]; }
    };
    // --- END PARCELABLE --- //
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Data class for storing the url endpoints associated with a Firefox account.
 *
 * Inspired by iOS' FirefoxAccountConfiguration:
 *   https://github.com/mozilla-mobile/firefox-ios/blob/748e137dfd5b020b56fc481b25e0d2366acb3df2/Account/FirefoxAccountConfiguration.swift
 */
public class FirefoxAccountEndpointConfig implements Parcelable {

    private static final String CONTEXT = "fx_ios_v1"; // TODO: update context - https://github.com/mozilla/fxa-content-server/issues/2137

    public final String label;

    public final URI authServerURL;
    public final URI oauthServerURL;
    public final URI profileServerURL;

    public final URI signInURL;
    public final URI settingsURL;
    public final URI forceAuthURL;

    public final Sync15EndpointConfig syncConfig;

    private FirefoxAccountEndpointConfig(final String label, final String authServerURL, final String oauthServerURL,
            final String profileServerURL, final String signInURL, final String settingsURL, final String forceAuthURL,
            final Sync15EndpointConfig syncConfig) {
        this.label = label;
        try {
            this.authServerURL = new URI(authServerURL);
            this.oauthServerURL = new URI(oauthServerURL);
            this.profileServerURL = new URI(profileServerURL);
            this.signInURL = new URI(signInURL);
            this.settingsURL = new URI(settingsURL);
            this.forceAuthURL = new URI(forceAuthURL);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Expected valid URI", e);
        }
        this.syncConfig = syncConfig;
    }

    /**
     * Gets an endpoint config for the stable dev servers; note <b>you will be unable to access sync data</b> with this
     * configuration: the sync servers (stage) don't share resources with these dev servers.
     */
    public static FirefoxAccountEndpointConfig getStableDev() {
        return new FirefoxAccountEndpointConfig(
                /* label */ "StableDev",
                /* authServer */ "https://stable.dev.lcip.org/auth/v1",
                /* oauthServer */ "https://oauth-stable.dev.lcip.org",
                /* profileServer */ "https://stable.dev.lcip.org/profile",
                /* signIn */ appendContextParam("https://stable.dev.lcip.org/signin?service=sync"),
                /* settings */ appendContextParam("https://stable.dev.lcip.org/settings"),
                /* forceAuth */ appendContextParam("https://stable.dev.lcip.org/force_auth?service=sync"),
                Sync15EndpointConfig.getStage() // TODO: is there a better server to point to? Here & `LatestDev`.
        );
    }

    /**
     * Gets an endpoint config for the latest dev servers; note <b>you will be unable to access sync data</b> with this
     * configuration: the sync servers (stage) don't share resources with these dev servers.
     */
    public static FirefoxAccountEndpointConfig getLatestDev() {
        return new FirefoxAccountEndpointConfig(
                /* label */ "LatestDev",
                /* authServer */ "https://latest.dev.lcip.org/auth/v1",
                /* oauthServer */ "https://oauth-latest.dev.lcip.org",
                /* profileServer */ "https://latest.dev.lcip.org/profile",
                /* signIn */ appendContextParam("https://latest.dev.lcip.org/signin?service=sync"),
                /* settings */ appendContextParam("https://latest.dev.lcip.org/settings"),
                /* forceAuth */ appendContextParam("https://latest.dev.lcip.org/force_auth?service=sync"),
                Sync15EndpointConfig.getStage()
        );
    }

    public static FirefoxAccountEndpointConfig getStage() {
        return new FirefoxAccountEndpointConfig(
                /* label */ "Stage",
                /* authServer */ "https://api-accounts.stage.mozaws.net/v1",
                /* oauthServer */ "https://oauth.stage.mozaws.net/v1",
                /* profileServer */ "https://profile.stage.mozaws.net/v1",
                /* signIn */ appendContextParam("https://accounts.stage.mozaws.net/signin?service=sync"),
                /* settings */ appendContextParam("https://accounts.stage.mozaws.net/settings"),
                /* forceAuth */ appendContextParam("https://accounts.stage.mozaws.net/force_auth?service=sync"),
                Sync15EndpointConfig.getStage()
        );
    }

    public static FirefoxAccountEndpointConfig getProduction() {
        return new FirefoxAccountEndpointConfig(
                /* label */ "Production",
                /* authServer */ "https://api.accounts.firefox.com/v1",
                /* oauthServer */ "https://oauth.accounts.firefox.com/v1",
                /* profileServer */ "https://profile.accounts.firefox.com/v1",
                /* signIn */ appendContextParam("https://accounts.firefox.com/signin?service=sync"),
                /* settings */ appendContextParam("https://accounts.firefox.com/settings"),
                /* forceAuth */ appendContextParam("https://accounts.firefox.com/force_auth?service=sync"),
                Sync15EndpointConfig.getProduction()
        );
    }

    private static String appendContextParam(final String url) {
        // TODO: better to append URL params with a dedicated url method.
        return url + (url.contains("?") ? "&" : "?") + "context=" + CONTEXT;
    }

    // --- START PARCELABLE --- //
    @Override public int describeContents() { return 0; }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(label);
        for (final URI uri : new URI[] {
                authServerURL,
                oauthServerURL,
                profileServerURL,
                signInURL,
                settingsURL,
                forceAuthURL,
        }) {
            dest.writeString(uri.toString());
        }
        dest.writeParcelable(syncConfig, flags);
    }

    public static final Parcelable.Creator<FirefoxAccountEndpointConfig> CREATOR = new Parcelable.Creator<FirefoxAccountEndpointConfig>() {
        @Override
        public FirefoxAccountEndpointConfig createFromParcel(final Parcel source) {
            return new FirefoxAccountEndpointConfig(
                    source.readString(),
                    source.readString(),
                    source.readString(),
                    source.readString(),
                    source.readString(),
                    source.readString(),
                    source.readString(),
                    Sync15EndpointConfig.CREATOR.createFromParcel(source)
            );
        }

        @Override public FirefoxAccountEndpointConfig[] newArray(final int size) { return new FirefoxAccountEndpointConfig[size]; }
    };
    // --- END PARCELABLE --- //
}

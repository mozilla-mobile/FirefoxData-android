/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.impl;

import org.junit.Test;

import static org.junit.Assert.*;

public class FirefoxSyncRequestUtilsTest {

    @Test
    public void testGetUserAgentMatchesRegex() throws Exception {
        final String applicationName = "NMX JUnit Test";

        // Expected format is Mobile-<OS>-Sync/(<form factor>; <OS> <OS-version>) (<Application-name>)
        // Note that the Android version is "null" in JUnit tests but shouldn't be on device.
        final String expectedUARegex =
                "Mobile-Android-Sync/\\([A-Za-z]+; Android [a-zA-Z0-9.]+\\) \\(" + applicationName + "\\)";
        final String actualUA = FirefoxSyncRequestUtils.getUserAgent(applicationName, false);
        assertTrue("Expected pattern to match actual user agent: " + actualUA, actualUA.matches(expectedUARegex));
    }

    @Test
    public void testGetUserAgentIndicatesPhoneFormFactor() throws Exception {
        final String actualPhoneUA = FirefoxSyncRequestUtils.getUserAgent("whatever", false);
        assertTrue("Expected phone form factor to be in actual user agent: " + actualPhoneUA, actualPhoneUA.contains("Mobile"));
    }

    @Test
    public void testGetUserAgentIndicatesTabletFormFactor() throws Exception {
        final String actualTabletUA = FirefoxSyncRequestUtils.getUserAgent("whatever", true);
        assertTrue("Expected tablet form factor to be in actual user agent: " + actualTabletUA, actualTabletUA.contains("Tablet"));
    }
}
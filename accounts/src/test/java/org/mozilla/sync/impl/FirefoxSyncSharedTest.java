/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.impl;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.AndroidDepedentTestCase;
import org.mozilla.FirefoxSyncTestHelpers;
import org.mozilla.util.DeviceUtils;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@PrepareForTest(DeviceUtils.class)
public class FirefoxSyncSharedTest extends AndroidDepedentTestCase {

    @Before
    public void mockStatic() {
        FirefoxSyncTestHelpers.mockDeviceUtils();
    }

    @Before
    public void resetState() {
        FirefoxSyncShared.setSessionApplicationName(null);
    }

    // The interaction between setSessionApplicationName & getUserAgent is fragile so we test that the desired values
    // are at least returned here and below. Ideally, we'd also test that the application name gets set when we expect
    // it to and is reset when we expect it to, but that's hard.
    @Test
    public void testGetUserAgentApplicationNameSet() throws Exception {
        final String applicationName = "Moonlight-Sonata";
        FirefoxSyncShared.setSessionApplicationName(applicationName);

        final String actualUA = FirefoxSyncShared.getUserAgent();
        assertTrue("Expected app name, " + applicationName + ", to be included in user agent: " + actualUA,
                actualUA.contains(applicationName));
    }

    @Test
    public void testGetUserAgentApplicationNameReset() throws Exception {
        // This test is dependent on testGetUserAgentApplicationNameSet.
        final String applicationName = "Symphony No. 5";
        FirefoxSyncShared.setSessionApplicationName(applicationName);

        final String actualUA = FirefoxSyncShared.getUserAgent();
        assertTrue("Expected app name, " + applicationName + ", to be included in user agent: " + actualUA,
                actualUA.contains(applicationName));

        // Now that we've verified the app name appears in the user agent, let's verify that we can reset the UA.
        FirefoxSyncShared.setSessionApplicationName(null);

        final String actualUAWithoutApp = FirefoxSyncShared.getUserAgent();
        assertFalse("Expected app name, " + applicationName + ", is not included in user agent: " + actualUAWithoutApp,
                actualUAWithoutApp.contains(applicationName));
    }
}
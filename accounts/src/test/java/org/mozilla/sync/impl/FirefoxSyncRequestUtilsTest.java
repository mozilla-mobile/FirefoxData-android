/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.impl;

import android.content.Context;
import android.content.res.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mozilla.util.DeviceUtils;
import org.mozilla.util.DeviceUtilsTestHelper;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirefoxSyncRequestUtilsTest {

    @Mock private Context mockDeviceUtilsContext;
    @Mock private Resources mockDeviceUtilsResources;

    @Before
    public void setUpDeviceUtils() {
        setUpDeviceUtils(false);
    }

    /** Tests can call this to override the default DeviceUtils values. */
    private void setUpDeviceUtils(final boolean isTablet) {
        DeviceUtilsTestHelper.resetInit();

        when(mockDeviceUtilsContext.getResources()).thenReturn(mockDeviceUtilsResources);
        when(mockDeviceUtilsResources.getBoolean(Mockito.anyInt())).thenReturn(isTablet); // A call to Resources.getBool(R.bool.is_xlarge);
        DeviceUtils.init(mockDeviceUtilsContext);
    }

    @Test
    public void testGetUserAgentMatchesRegex() throws Exception {
        final String applicationName = "NMX JUnit Test";

        // Expected format is Mobile-<OS>-Sync/(<form factor>; <OS> <OS-version>) (<Application-name>)
        // Note that the Android version is "null" in JUnit tests but shouldn't be on device.
        final String expectedUARegex =
                "Mobile-Android-Sync/\\([A-Za-z]+; Android [a-zA-Z0-9.]+\\) \\(" + applicationName + "\\)";
        final String actualUA = FirefoxSyncRequestUtils.getUserAgent(applicationName);
        assertTrue("Expected pattern to match actual user agent: " + actualUA, actualUA.matches(expectedUARegex));
    }

    // This implementation, test, & related test suck because we have to know the implementation
    // details, i.e. how DeviceUtils determines the form factor, in order to spoof it.
    @Test
    public void testGetUserAgentIndicatesPhoneFormFactor() throws Exception {
        setUpDeviceUtils(false);
        final String actualPhoneUA = FirefoxSyncRequestUtils.getUserAgent("whatever");
        assertTrue("Expected phone form factor to be in actual user agent: " + actualPhoneUA, actualPhoneUA.contains("Mobile"));
    }

    @Test
    public void testGetUserAgentIndicatesTabletFormFactor() throws Exception {
        setUpDeviceUtils(true);
        final String actualTabletUA = FirefoxSyncRequestUtils.getUserAgent("whatever");
        assertTrue("Expected tablet form factor to be in actual user agent: " + actualTabletUA, actualTabletUA.contains("Tablet"));
    }
}
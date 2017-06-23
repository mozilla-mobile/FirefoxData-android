/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.impl

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fxa_data.FxDataMocks
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(PowerMockRunner::class)
@PrepareForTest(DeviceUtils::class)
class FirefoxDataSharedTest {

    @Before
    fun mockStatic() {
        FxDataMocks.mockDeviceUtils()
    }

    @Before
    fun resetState() {
        FirefoxDataShared.setSessionApplicationName(null)
    }

    // The interaction between setSessionApplicationName & getUserAgent is fragile so we test that the desired values
    // are at least returned here and below. Ideally, we'd also test that the application name gets set when we expect
    // it to and is reset when we expect it to, but that's hard.
    @Test
    fun testGetUserAgentApplicationNameSet() {
        val applicationName = "Moonlight-Sonata"
        FirefoxDataShared.setSessionApplicationName(applicationName)

        val actualUA = FirefoxDataShared.getUserAgent()
        assertTrue("Expected app name, $applicationName, to be included in user agent: $actualUA",
                actualUA.contains(applicationName))
    }

    @Test
    fun testGetUserAgentApplicationNameReset() {
        // This test is dependent on testGetUserAgentApplicationNameSet.
        val applicationName = "Symphony No. 5"
        FirefoxDataShared.setSessionApplicationName(applicationName)

        val actualUA = FirefoxDataShared.getUserAgent()
        assertTrue("Expected app name, $applicationName, to be included in user agent: $actualUA",
                actualUA.contains(applicationName))

        // Now that we've verified the app name appears in the user agent, let's verify that we can reset the UA.
        FirefoxDataShared.setSessionApplicationName(null)

        val actualUAWithoutApp = FirefoxDataShared.getUserAgent()
        assertFalse("Expected app name, $applicationName, is not included in user agent: $actualUAWithoutApp",
                actualUAWithoutApp.contains(applicationName))
    }
}
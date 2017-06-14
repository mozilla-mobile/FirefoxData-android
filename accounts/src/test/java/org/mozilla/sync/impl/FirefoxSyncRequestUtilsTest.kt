/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.impl

import org.junit.Test

import org.junit.Assert.*

class FirefoxSyncRequestUtilsTest {

    @Test
    fun testGetUserAgentMatchesRegex() {
        val applicationName = "NMX JUnit Test"

        // Expected format is Mobile-<OS>-Sync/(<form factor>; <OS> <OS-version>) (<Application-name>)
        // Note that the Android version is "null" in JUnit tests but shouldn't be on device.
        val expectedUARegex = "Mobile-Android-Sync/\\([A-Za-z]+; Android [a-zA-Z0-9.]+\\) \\($applicationName\\)".toRegex()
        val actualUA = FirefoxSyncRequestUtils.getUserAgent(applicationName, false)
        assertTrue("Expected pattern to match actual user agent: " + actualUA, actualUA.matches(expectedUARegex))
    }

    @Test
    fun testGetUserAgentIndicatesPhoneFormFactor() {
        val actualPhoneUA = FirefoxSyncRequestUtils.getUserAgent("whatever", false)
        assertTrue("Expected phone form factor to be in actual user agent: " + actualPhoneUA, actualPhoneUA.contains("Mobile"))
    }

    @Test
    fun testGetUserAgentIndicatesTabletFormFactor() {
        val actualTabletUA = FirefoxSyncRequestUtils.getUserAgent("whatever", true)
        assertTrue("Expected tablet form factor to be in actual user agent: " + actualTabletUA, actualTabletUA.contains("Tablet"))
    }
}
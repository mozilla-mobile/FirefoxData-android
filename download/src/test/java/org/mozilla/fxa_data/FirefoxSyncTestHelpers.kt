/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data

import org.mozilla.fxa_data.impl.DeviceUtils
import org.powermock.api.mockito.PowerMockito

object FirefoxSyncTestHelpers {

    fun mockDeviceUtils() {
        PowerMockito.mockStatic(DeviceUtils::class.java)
        PowerMockito.`when`(DeviceUtils.isTablet()).thenReturn(false)
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla;

import org.mozilla.util.DeviceUtils;
import org.powermock.api.mockito.PowerMockito;

public class FirefoxSyncTestHelpers {
    private FirefoxSyncTestHelpers() {}

    public static void mockDeviceUtils() {
        PowerMockito.mockStatic(DeviceUtils.class);
        PowerMockito.when(DeviceUtils.isTablet()).thenReturn(false);
    }
}

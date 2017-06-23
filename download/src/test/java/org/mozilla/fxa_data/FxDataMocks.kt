/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data

import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mozilla.fxa_data.ext.withAccessible
import org.mozilla.fxa_data.impl.DeviceUtils
import org.mozilla.fxa_data.impl.FirefoxAccount
import org.mozilla.gecko.fxa.login.Married
import org.mozilla.gecko.fxa.login.State
import org.powermock.api.mockito.PowerMockito

object FxDataMocks {

    fun mockFirefoxAccount(): FirefoxAccount {
        // The constructor asserts accountState.getStateLabel() == Married - we create the account state here.
        val mockMarriedState = mock(Married::class.java)
        `when`(mockMarriedState.stateLabel).thenReturn(State.StateLabel.Married)

        val mockFirefoxAccount = mock(FirefoxAccount::class.java)

        // We can't pass the mock state in the constructor because we create a mock. Instead, we use reflection to
        // access and set the final field. A more correct way to fix this would be to create field accessor methods but
        // I didn't want to add the boilerplate.
        val fxAccountStateField = FirefoxAccount::class.java.getField("accountState")
        fxAccountStateField.withAccessible { it.set(mockFirefoxAccount, mockMarriedState) }

        return mockFirefoxAccount
    }

    fun mockDeviceUtils() {
        PowerMockito.mockStatic(DeviceUtils::class.java)

        // Get around the isInit assertion in isTablet.
        PowerMockito.`when`(DeviceUtils.isTablet()).thenReturn(false)
    }
}

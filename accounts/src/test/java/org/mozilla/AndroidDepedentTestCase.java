/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla;

import android.text.TextUtils;
import android.util.Log;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * A test case that makes the basic Android dependencies.
 *
 * To use individual functionality, see {@link AndroidTestHelpers}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class, Log.class})
public class AndroidDepedentTestCase {

    @Before
    public void mockAndroidDependencies() {
        AndroidTestHelpers.mockTextUtils();
        AndroidTestHelpers.mockLog();
    }
}

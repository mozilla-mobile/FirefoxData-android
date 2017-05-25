package org.mozilla;
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.text.TextUtils;
import android.util.Log;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import static org.mockito.Matchers.any;

/** A collection of static methods to help run tests based on Android. See also {@link AndroidDepedentTestCase}. */
public class AndroidTestHelpers {
    private AndroidTestHelpers() {}

    /**
     * Mocks {@link TextUtils}.
     *
     * Must annotate the test class with:
     * - {@code @RunWith(PowerMockRunner.class)}
     * - {@code @PrepareForTest(TextUtils.class)}
     *
     * via https://stackoverflow.com/a/38940253/2219998.
     */
    public static void mockTextUtils() {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return !(a != null && a.length() > 0);
            }
        });
    }

    /**
     * Mocks {@link Log}.
     *
     * Must annotate the test class with:
     * - {@code @RunWith(PowerMockRunner.class)}
     * - {@code @PrepareForTest(Log.class)}
     */
    public static void mockLog() {
        PowerMockito.mockStatic(Log.class);
    }
}

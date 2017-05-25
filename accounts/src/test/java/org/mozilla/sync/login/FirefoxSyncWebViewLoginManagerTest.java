/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FirefoxSyncWebViewLoginManagerTest {

    @Mock private Activity mockActivity;

    private FirefoxSyncLoginManager loginManager;

    @Before
    public void setUp() {
        loginManager = new FirefoxSyncWebViewLoginManager(Mockito.mock(Context.class));
    }

    @Test
    public void testPromptLoginStartsActivity() throws Exception {
        loginManager.promptLogin(mockActivity, "Bach", Mockito.mock(FirefoxSyncLoginManager.LoginCallback.class));

        // In refactorings, startActivityForResult(Intent, int, Bundle) could also be called.
        verify(mockActivity, atLeastOnce()).startActivityForResult(any(Intent.class), anyInt());
    }

}
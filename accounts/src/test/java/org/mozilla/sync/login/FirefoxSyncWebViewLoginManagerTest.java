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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.sync.FirefoxSyncClient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity.EXTRA_ACCOUNT;
import static org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity.EXTRA_FAILURE_REASON;

@RunWith(MockitoJUnitRunner.class)
public class FirefoxSyncWebViewLoginManagerTest {

    // TODO: use Kotlin!

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

    private void waitForOnActivityResultToComplete(final LoginCallbackSpy callback) throws InterruptedException {
        synchronized (callback) { callback.wait(2000); }
    }

    @Test
    public void testOnActivityResultDoesNotCallbackForNotOurIntent() throws Exception {
        // We need to register a callback to ensure it's not called after calling onActivityResult with an Intent
        // that is not ours.
        final LoginCallbackSpy callback = new LoginCallbackSpy();
        loginManager.promptLogin(mockActivity, "Chopin", callback); // TODO: use same request code.
        loginManager.onActivityResult(100, Activity.RESULT_OK, new Intent(Intent.ACTION_VIEW));

        waitForOnActivityResultToComplete(callback);

        assertFalse("Expected onSuccess to not be called", callback.wasSuccess);
        assertFalse("Expected onFailure to not be called", callback.wasFailure);
        assertFalse("Expected onUserCancelled to not be called", callback.wasCancelled);
    }

    private enum ActivityResult {
        SUCCESS, FAILURE, CANCELLED;
    }

    /** Returns the result of {@link org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity} with the expected format. */
    private Intent getPromptLoginResultIntent(final ActivityResult activityResult) {
        final Intent returnIntent = new Intent(FirefoxSyncWebViewLoginActivity.ACTION_WEB_VIEW_LOGIN_RETURN);
        if (activityResult == ActivityResult.SUCCESS) {
            returnIntent.putExtra(EXTRA_ACCOUNT, Mockito.mock(FirefoxAccount.class));
        }
        if (activityResult == ActivityResult.FAILURE) {
            returnIntent.putExtra(EXTRA_FAILURE_REASON, "The failure String");
        }
        return returnIntent;
    }

    @Test
    public void testOnActivityResultCallsSuccess() throws Exception { // todo
    }

    @Test
    public void testOnActivityResultCallsError() throws Exception {
        // todo: factor out promptLogin call.
        final ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(mockActivity).startActivityForResult(any(Intent.class), requestCodeCaptor.capture());

        final LoginCallbackSpy callback = new LoginCallbackSpy();
        loginManager.promptLogin(mockActivity, "Bach", callback); // registers callback.
        loginManager.onActivityResult(requestCodeCaptor.getValue(), FirefoxSyncWebViewLoginActivity.RESULT_ERROR,
                getPromptLoginResultIntent(ActivityResult.FAILURE));

        waitForOnActivityResultToComplete(callback);

        assertTrue("Expected onFailure to be called", callback.wasFailure);
    }

    @Test
    public void testOnActivityResultCallsUserCancelled() { // todo

    }

    private static class LoginCallbackSpy implements FirefoxSyncLoginManager.LoginCallback {
        boolean wasSuccess = false;
        boolean wasFailure = false;
        boolean wasCancelled = false;

        @Override public void onSuccess(final FirefoxSyncClient syncClient) { wasSuccess = true; onAsyncComplete(); }
        @Override public void onFailure(final FirefoxSyncLoginException e) { wasFailure = true; onAsyncComplete(); }
        @Override public void onUserCancel() { wasCancelled = true; onAsyncComplete(); }

        boolean wasCallbackCalled() { return wasSuccess || wasFailure || wasCancelled; }

        private void onAsyncComplete() {
            synchronized (this) { this.notifyAll(); }
        }
    }
}
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.runners.MockitoJUnitRunner
import org.mozilla.sync.impl.FirefoxAccount
import org.mozilla.sync.sync.FirefoxSyncClient

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.Matchers.any
import org.mockito.Matchers.anyInt
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity.EXTRA_ACCOUNT
import org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity.EXTRA_FAILURE_REASON
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class FirefoxSyncWebViewLoginManagerTest {

    @Mock private val mockActivity: Activity? = null

    private lateinit var loginManager: FirefoxSyncLoginManager

    @Before
    fun setUp() {
        loginManager = FirefoxSyncWebViewLoginManager(Mockito.mock(Context::class.java))
    }

    @Test
    fun testPromptLoginStartsActivity() {
        loginManager.promptLogin(mockActivity, "Bach", Mockito.mock(FirefoxSyncLoginManager.LoginCallback::class.java))

        // In refactorings, startActivityForResult(Intent, int, Bundle) could also be called.
        verify<Activity>(mockActivity, atLeastOnce()).startActivityForResult(any(Intent::class.java), anyInt())
    }

    private fun waitForOnActivityResultToComplete(callback: LoginCallbackSpy) {
        callback.asyncWaitLatch.await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testOnActivityResultDoesNotCallbackForNotOurIntent() {
        // We need to register a callback to ensure it's not called after calling onActivityResult with an Intent
        // that is not ours.
        val callback = LoginCallbackSpy()
        loginManager.promptLogin(mockActivity, "Chopin", callback) // TODO: use same request code.
        loginManager.onActivityResult(100, Activity.RESULT_OK, Intent(Intent.ACTION_VIEW))

        waitForOnActivityResultToComplete(callback)

        assertFalse("Expected onSuccess to not be called", callback.wasSuccess)
        assertFalse("Expected onFailure to not be called", callback.wasFailure)
        assertFalse("Expected onUserCancelled to not be called", callback.wasCancelled)
    }

    private enum class ActivityResult {
        SUCCESS, FAILURE, CANCELLED
    }

    /** Returns the result of [org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity] with the expected format.  */
    private fun getPromptLoginResultIntent(activityResult: ActivityResult): Intent {
        val returnIntent = Intent(FirefoxSyncWebViewLoginActivity.ACTION_WEB_VIEW_LOGIN_RETURN)
        if (activityResult == ActivityResult.SUCCESS) {
            returnIntent.putExtra(EXTRA_ACCOUNT, Mockito.mock(FirefoxAccount::class.java))
        }
        if (activityResult == ActivityResult.FAILURE) {
            returnIntent.putExtra(EXTRA_FAILURE_REASON, "The failure String")
        }
        return returnIntent
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityResultCallsSuccess() { // todo
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityResultCallsError() {
        // todo: factor out promptLogin call.
        val requestCodeCaptor = ArgumentCaptor.forClass(Int::class.java)
        doNothing().`when`<Activity>(mockActivity).startActivityForResult(any(Intent::class.java), requestCodeCaptor.capture())

        val callback = LoginCallbackSpy()
        loginManager.promptLogin(mockActivity, "Bach", callback) // registers callback.
        loginManager.onActivityResult(requestCodeCaptor.value, FirefoxSyncWebViewLoginActivity.RESULT_ERROR,
                getPromptLoginResultIntent(ActivityResult.FAILURE))

        waitForOnActivityResultToComplete(callback)

        assertTrue("Expected onFailure to be called", callback.wasFailure)
    }

    @Test
    fun testOnActivityResultCallsUserCancelled() { // todo

    }

    private class LoginCallbackSpy : FirefoxSyncLoginManager.LoginCallback {
        internal var wasSuccess = false
        internal var wasFailure = false
        internal var wasCancelled = false
        internal val wasCallbackCalled: Boolean
            get() = listOf(wasSuccess, wasFailure, wasCancelled).any()

        internal val asyncWaitLatch = CountDownLatch(1)

        override fun onSuccess(syncClient: FirefoxSyncClient) { wasSuccess = true; onAsyncComplete(); }
        override fun onFailure(e: FirefoxSyncLoginException) { wasFailure = true; onAsyncComplete(); }
        override fun onUserCancel() { wasCancelled = true; onAsyncComplete(); }

        private fun onAsyncComplete() {
            asyncWaitLatch.countDown()
        }
    }
}
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login

import android.app.Activity
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Matchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mozilla.gecko.fxa.login.Married
import org.mozilla.gecko.sync.CollectionKeys
import org.mozilla.gecko.tokenserver.TokenServerToken
import org.mozilla.sync.impl.FirefoxAccount
import org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity.EXTRA_ACCOUNT
import org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity.EXTRA_FAILURE_REASON
import org.mozilla.sync.sync.FirefoxSyncClient
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Sadly, we can't use this for @PrepareForTest because it's not a compile-time constant.
private val STATICALLY_MOCKED_CLASSES = listOf(
        FirefoxAccountUtils::class,
        FirefoxSyncTokenAccessor::class,
        FirefoxSyncCryptoKeysAccessor::class
)

/**
 * Tests for the [FirefoxSyncWebViewLoginManager].
 *
 * Ideas for additional tests:
 * - Verify API contracts:
 *   - Which thread are result callbacks called on?
 *   - Thread safety guarantees & which threads methods can be called from.
 * - Other methods: isSignedIn, loadStoredSyncAccount, signOut
 * - Mock network calls more granularly to better verify 1) error handling & 2) that results are properly constructed.
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest( // should duplicate STATICALLY_MOCKED_CLASSES
        FirefoxAccountUtils::class,
        FirefoxSyncTokenAccessor::class,
        FirefoxSyncCryptoKeysAccessor::class
)
class FirefoxSyncWebViewLoginManagerTest {

    @Mock private val mockActivity: Activity? = null

    private lateinit var loginManager: FirefoxSyncLoginManager

    @Before
    fun setUp() {
        loginManager = FirefoxSyncWebViewLoginManager(mock(FirefoxAccountSessionSharedPrefsStore::class.java))
    }

    @Test
    fun testPromptLoginStartsActivity() {
        loginManager.promptLogin(mockActivity, "Bach", mock(FirefoxSyncLoginManager.LoginCallback::class.java))

        // In refactorings, startActivityForResult(Intent, int, Bundle) could also be called.
        verify<Activity>(mockActivity, atLeastOnce()).startActivityForResult(any(Intent::class.java), anyInt())
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

    @Test
    fun testOnActivityResultCallsSuccess() {
        // If this test is failing, make sure no one has added additional network requests to the code!
        // For the longer explanation, see the definition of the function below.
        mockOnActivityResultSuccessNetworkCallsForSuccess()

        val callback = promptLoginAndOnActivityResultForResult(ActivityResult.SUCCESS)
        assertTrue("Expected onSuccess to be called", callback.wasSuccess)
    }

    private fun mockOnActivityResultSuccessNetworkCallsForSuccess() {
        // This is fragile: a successful onActivityResult makes several requests to the network via static methods
        // and we peek inside the implementation to know which methods contact the network in order to mock them so
        // we don't have to make real network requests. The "proper" way to test this would be to pass in (singleton?)
        // instances of classes that make the network calls and mock these objects rather than calling these fn
        // statically. Since there is a chain of calls, the dependencies are non-trivial, requiring a lot of boilerplate
        // code that would take a non-trivial amount of time to implement. Instead, we keep the implementation simple
        // and mock the static functions directly, facing the fact that this test can steathily break if we add
        // additional static calls.
        STATICALLY_MOCKED_CLASSES.forEach { PowerMockito.mockStatic(it.java) }

        val marriedCallback = ArgumentCaptor.forClass(FirefoxAccountUtils.MarriedLoginCallback::class.java)
        PowerMockito.`when`(FirefoxAccountUtils.advanceAccountToMarried(any(), any(), marriedCallback.capture())).then {
            marriedCallback.value.onMarried(mock(Married::class.java)) // success callback!
        }

        val syncTokenCallback = ArgumentCaptor.forClass(FirefoxSyncTokenAccessor.FirefoxSyncTokenServerClientDelegate::class.java)
        PowerMockito.`when`(FirefoxSyncTokenAccessor.getBlocking(any(), syncTokenCallback.capture())).then {
            syncTokenCallback.value.handleSuccess(mock(TokenServerToken::class.java))
        }

        val cryptoKeysCallback = ArgumentCaptor.forClass(FirefoxSyncCryptoKeysAccessor.CollectionKeysCallback::class.java)
        PowerMockito.`when`(FirefoxSyncCryptoKeysAccessor.getBlocking(any(), any(), cryptoKeysCallback.capture())).then {
            cryptoKeysCallback.value.onKeysReceived(mock(CollectionKeys::class.java)) // success callback!
        }
    }

    @Test
    fun testOnActivityResultCallsError() {
        val callback = promptLoginAndOnActivityResultForResult(ActivityResult.FAILURE)
        assertTrue("Expected onFailure to be called", callback.wasFailure)
    }

    @Test
    fun testOnActivityResultCallsUserCancelled() {
        val callback = promptLoginAndOnActivityResultForResult(ActivityResult.CANCELLED)
        assertTrue("Expected onCancelled to be called", callback.wasCancelled)
    }

    private fun waitForOnActivityResultToComplete(callback: LoginCallbackSpy) {
        callback.asyncWaitLatch.await(500, TimeUnit.MILLISECONDS) // these never hit the network so they can be fast.
    }

    /** Helper fn to call promptLogin & onActivityResult with a mocked intent of the given ActivityResult. */
    private fun promptLoginAndOnActivityResultForResult(result: ActivityResult): LoginCallbackSpy {
        val requestCodeCaptor = ArgumentCaptor.forClass(Int::class.java)
        doNothing().`when`<Activity>(mockActivity).startActivityForResult(any(Intent::class.java), requestCodeCaptor.capture())

        val resultCode = when (result) {
            ActivityResult.FAILURE -> FirefoxSyncWebViewLoginActivity.RESULT_ERROR
            ActivityResult.CANCELLED -> FirefoxSyncWebViewLoginActivity.RESULT_CANCELED
            ActivityResult.SUCCESS -> FirefoxSyncWebViewLoginActivity.RESULT_OK
        }

        val callback = LoginCallbackSpy()
        loginManager.promptLogin(mockActivity, "Bach", callback) // registers callback.
        loginManager.onActivityResult(requestCodeCaptor.value, resultCode, getPromptLoginResultIntent(result))

        waitForOnActivityResultToComplete(callback)

        return callback
    }

    /** Returns the result of [org.mozilla.sync.login.FirefoxSyncWebViewLoginActivity] with the expected format.  */
    private fun getPromptLoginResultIntent(activityResult: ActivityResult): Intent {
        val mockFirefoxAccount = mock(FirefoxAccount::class.java)
        `when`(mockFirefoxAccount.withNewState(any())).thenReturn(mockFirefoxAccount)

        val returnIntent = Intent(FirefoxSyncWebViewLoginActivity.ACTION_WEB_VIEW_LOGIN_RETURN)
        when (activityResult) {
            ActivityResult.SUCCESS -> returnIntent.putExtra(EXTRA_ACCOUNT, mockFirefoxAccount)
            ActivityResult.FAILURE -> returnIntent.putExtra(EXTRA_FAILURE_REASON, "The failure String")
            else -> Unit
        }
        return returnIntent
    }

    private class LoginCallbackSpy : FirefoxSyncLoginManager.LoginCallback {
        internal var wasSuccess = false
        internal var wasFailure = false
        internal var wasCancelled = false

        internal val asyncWaitLatch = CountDownLatch(1)

        override fun onSuccess(syncClient: FirefoxSyncClient) { wasSuccess = true; onAsyncComplete(); }
        override fun onFailure(e: FirefoxSyncException) { wasFailure = true; onAsyncComplete(); }
        override fun onUserCancel() { wasCancelled = true; onAsyncComplete(); }

        private fun onAsyncComplete() {
            asyncWaitLatch.countDown()
        }
    }
}

private enum class ActivityResult {
    SUCCESS, FAILURE, CANCELLED
}

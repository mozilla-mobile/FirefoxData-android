/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.login;

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.gecko.fxa.login.Married
import org.mozilla.gecko.fxa.login.StateFactory
import org.mozilla.fxa_data.impl.FirefoxAccount
import org.mozilla.fxa_data.impl.FirefoxAccountEndpointConfig
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirefoxAccountSessionSharedPrefsStoreTest {

    private lateinit var store: FirefoxAccountSessionSharedPrefsStore

    @Before
    fun setUp() {
        store = FirefoxAccountSessionSharedPrefsStore(RuntimeEnvironment.application)
    }

    @Test
    fun testStoreSessionAndLoadSessionReturnsSameData() {
        val expectedSession = getMockSession(email = "zzz@zzz.xyz", uid = "uid-123", appName = "FirefoxAccounts Test")
        store.saveSession(expectedSession)

        val loadedSession = store.loadSession()

        // FirefoxAccount* does not write a .equals because then we'd have to maintain a hashCode method.
        // Instead, we just test everything here.
        assertEquals(expectedSession.applicationName, loadedSession.applicationName)

        val expectedAccount = expectedSession.firefoxAccount
        val actualAccount = loadedSession.firefoxAccount
        assertEquals(expectedAccount.email, actualAccount.email)
        assertEquals(expectedAccount.uid, actualAccount.uid)

        // Ideally, we'd test more fields but this is probably good enough.
        assertEquals(expectedAccount.endpointConfig.label, actualAccount.endpointConfig.label)
        assertEquals(expectedAccount.accountState.stateLabel, actualAccount.accountState.stateLabel)
    }

    @Test(expected = FirefoxAccountSessionSharedPrefsStore.FailedToLoadSessionException::class)
    fun testLoadSessionWithNoPriorStoreWillThrow() {
        store.loadSession() // expected to throw.
    }

    @Test
    fun testDeleteStoredSessionAloneDoesNotThrow() {
        store.deleteStoredSession()
    }

    @Test(expected = FirefoxAccountSessionSharedPrefsStore.FailedToLoadSessionException::class)
    fun testLoadSessionAfterStoreAndDeleteWillThrow() {
        store.saveSession(getMockSession(email = "what@yeah.com", uid = "a-uid", appName = "App Is Cool"))
        store.deleteStoredSession()
        store.loadSession() // expected to throw.
    }

    private fun getMockSession(email: String, uid: String, appName: String): FirefoxAccountSession {
        val state = getMockState(email, uid)
        val account = FirefoxAccount(email, uid, state, FirefoxAccountEndpointConfig.getProduction())
        return FirefoxAccountSession(account, appName)
    }

    private fun getMockState(email: String, uid: String): Married {
        val idKeyPair = StateFactory.generateKeyPair()
        val kB = "a".repeat(32).toByteArray() // must be length 32.
        return Married(email, uid, byteArrayOf(), byteArrayOf(), kB, idKeyPair, "cert")
    }
}

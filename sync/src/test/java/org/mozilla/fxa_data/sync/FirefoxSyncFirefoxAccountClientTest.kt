/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.sync;

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Matchers.any
import org.mockito.Matchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mozilla.gecko.sync.CollectionKeys
import org.mozilla.gecko.tokenserver.TokenServerToken
import org.mozilla.fxa_data.impl.FirefoxAccount
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord as UnderlyingHistoryRecord

@RunWith(PowerMockRunner::class)
@PrepareForTest(FirefoxSyncHistory::class)
class FirefoxSyncFirefoxAccountClientTest {

    private lateinit var client: FirefoxSyncFirefoxAccountClient

    @Captor private lateinit var historyCallbackCaptor: ArgumentCaptor<OnSyncComplete<List<HistoryRecord>>>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        client = FirefoxSyncFirefoxAccountClient(mock(FirefoxAccount::class.java), mock(TokenServerToken::class.java),
                mock(CollectionKeys::class.java))
    }

    /**
     * Verifies that the value returned from [FirefoxSyncBookmarks.getBlocking] is returned directly through our library
     * API. Thus, if we test that method and it works, the API should work.
     *
     * Ideally, we also test this for [FirefoxSyncClient.getHistoryWithLimit] & the equivalents for bookmarks & password
     * but this is good enough for now.
     */
    @Test
    fun getAllHistoryReturnsValueFromStaticGetHistoryCall() {
        val historyOne = UnderlyingHistoryRecord("guid-1")
        historyOne.title = "Mozilla homepage"
        historyOne.histURI = "https://mozilla.org"

        val historyTwo = UnderlyingHistoryRecord("guid-2")
        historyTwo.title = "Google homepage"
        historyTwo.histURI = "https://google.com"

        val expectedHistory = listOf(historyOne, historyTwo).map { HistoryRecord(it) }
        val immutableExpectedHistory = Collections.unmodifiableList(expectedHistory) // ensure list is not modified by non-test code.
        val expectedResult = SyncCollectionResult<List<HistoryRecord>>(immutableExpectedHistory)

        PowerMockito.mockStatic(FirefoxSyncHistory::class.java)
        PowerMockito.`when`(FirefoxSyncHistory.getBlocking(any(), anyInt(), historyCallbackCaptor.capture())).then {
            historyCallbackCaptor.value.onSuccess(expectedResult)
        }

        val actualResult = client.allHistory
        val actualHistory = actualResult.result

        // This is referential equality because we don't implement HistoryRecord.equals (because then we have to
        // maintain it & hashCode methods). Since we make the history list immutable, this is the same as if the
        // lists are equal.
        assertEquals(expectedHistory, actualHistory)
    }
}
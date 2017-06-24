/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.download;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import org.mozilla.fxa_data.FirefoxData;
import org.mozilla.fxa_data.FirefoxDataException;
import org.mozilla.fxa_data.login.FirefoxDataLoginManager;

import java.util.List;

/**
 * An interface which allows a caller to retrieve data associated with a Sync account.
 *
 * Retrieve an instance through {@link FirefoxDataLoginManager}, which
 * can be obtained from the primary {@link FirefoxData} entry point.
 */
public interface FirefoxDataClient {

    // --- BOOKMARKS --- //
    /**
     * Retrieves all bookmarks associated with this Sync account.
     *
     * Inside the results container will be the root folder at the top of the bookmarks hierarchy.
     * This is a virtual folder: in Firefox, this folder's name, description, etc. are never seen
     * but its internal data can be seen by clicking "Show All Bookmarks". A list of bookmarks the
     * user has saved can be accessed with {@link BookmarkFolder#getBookmarks()} &
     * {@link BookmarkFolder#getSubfolders()}.
     *
     * This method is blocking and can time out.
     *
     * @return a container with the requested sync data; never null.
     * @throws FirefoxDataException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread
    FirefoxDataResult<BookmarkFolder> getAllBookmarks() throws FirefoxDataException;

    /**
     * Retrieves a limited number of bookmarks associated with this Sync account.
     *
     * Inside the results container will be the root folder at the top of the bookmarks hierarchy.
     * This is a virtual folder: in Firefox, this folder's name, description, etc. are never seen
     * but its internal data can be seen by clicking "Show All Bookmarks". A list of bookmarks the
     * user has saved can be accessed with {@link BookmarkFolder#getBookmarks()} &
     * {@link BookmarkFolder#getSubfolders()}.
     *
     * This method is blocking and can time out.
     *
     * @param itemLimit The maximum number of bookmarks to retrieve.
     * @return a container with the requested sync data; never null.
     * @throws FirefoxDataException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread
    FirefoxDataResult<BookmarkFolder> getBookmarksWithLimit(int itemLimit) throws FirefoxDataException;

    // --- HISTORY --- //
    /**
     * Retrieves all the history entries a user has created from visiting pages. The
     * results will be returned in most-recently visited to least-recently visited order.
     *
     * This method is blocking and can time out.
     *
     * @return a container with the requested sync data; never null.
     * @throws FirefoxDataException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread
    FirefoxDataResult<List<HistoryRecord>> getAllHistory() throws FirefoxDataException;

    /**
     * Retrieves a limited number of history entries a user has created from visiting pages. The
     * results will be returned in most-recently visited to least-recently visited order, with the
     * least-recently visited being omitted if the specified item limit is reached.
     *
     * This method is blocking and can time out.
     *
     * @param itemLimit The maximum number of history items to retrieve.
     * @return a container with the requested sync data; never null.
     * @throws FirefoxDataException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread
    FirefoxDataResult<List<HistoryRecord>> getHistoryWithLimit(int itemLimit) throws FirefoxDataException;

    // --- PASSWORDS --- //
    /**
     * Retrieves all the passwords the user has saved.
     *
     * This method is blocking and can time out.
     *
     * @return a container with the requested sync data; never null.
     * @throws FirefoxDataException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread
    FirefoxDataResult<List<PasswordRecord>> getAllPasswords() throws FirefoxDataException;

    /**
     * Retrieves a limited number of passwords the user has saved.
     *
     * This method is blocking and can time out.
     *
     * @param itemLimit The maximum number of passwords to retrieve.
     * @return a container with the requested sync data; never null.
     * @throws FirefoxDataException if there was an error retrieving the results.
     */
    @NonNull @WorkerThread
    FirefoxDataResult<List<PasswordRecord>> getPasswordsWithLimit(int itemLimit) throws FirefoxDataException;

    /**
     * Gets the email associated with this Sync Client. It is intended to be used in the UI to
     * notify a user which account they have logged in.
     *
     * This value can change and should never be used to uniquely identify a user.
     *
     * @return the email address associated with this account; this will never be null.
     * @throws FirefoxDataException if there was a failure retrieving the email address.
     */
    @NonNull String getEmail() throws FirefoxDataException;
}

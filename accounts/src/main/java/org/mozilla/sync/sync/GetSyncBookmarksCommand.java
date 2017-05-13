/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.json.JSONException;
import org.mozilla.sync.impl.FirefoxAccountSyncConfig;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecordFactory;
import org.mozilla.util.IOUtil;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gets the bookmarks for the associated account from Firefox Sync.
 */
class GetSyncBookmarksCommand extends SyncClientCommands.SyncClientCollectionCommand<BookmarkFolder> {

    private static final String BOOKMARKS_COLLECTION = "bookmarks";

    @Override
    public void initAsyncCall(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<SyncCollectionResult<BookmarkFolder>> onComplete) {
        final SyncClientBookmarksResourceDelegate resourceDelegate = new SyncClientBookmarksResourceDelegate(syncConfig, onComplete);
        try {
            makeGetRequestForCollection(syncConfig, BOOKMARKS_COLLECTION, null, resourceDelegate);
        } catch (final URISyntaxException e) {
            onComplete.onError(e);
        }
    }

    private static class SyncClientBookmarksResourceDelegate extends SyncClientBaseResourceDelegate<BookmarkFolder> {
        SyncClientBookmarksResourceDelegate(final FirefoxAccountSyncConfig syncConfig, final IOUtil.OnAsyncCallComplete<SyncCollectionResult<BookmarkFolder>> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final List<org.mozilla.gecko.sync.repositories.domain.BookmarkRecord> rawRecords;
            try {
                rawRecords = responseBodyToRawRecords(syncConfig, responseBody, BOOKMARKS_COLLECTION, new BookmarkRecordFactory());
            } catch (final NoCollectionKeysSetException | JSONException e) {
                onComplete.onError(e);
                return;
            }

            final BookmarkFolder rootBookmarkFolder = rawRecordsToBookmarksTree(rawRecords);
            onComplete.onSuccess(new SyncCollectionResult<>(rootBookmarkFolder));
        }

        private static BookmarkFolder rawRecordsToBookmarksTree(final List<org.mozilla.gecko.sync.repositories.domain.BookmarkRecord> rawRecords) {
            // Iterating over these a second time is inefficient (the first time creates the raw records list), but it
            // makes for cleaner code: fix if there are perf issues.
            //
            // This would be less error-prone if we did the immutable, recursive solution but we run the
            // risk of hitting a StackOverflowException. There are some work-arounds (Visitor pattern?)
            // but they're probably not worth the complexity.
            // todo: how to handle missing records? e.g. missing a parent b/c partial sync or corruption.
            final Map<String, BookmarkRecord> idToSeenBookmarks = new HashMap<>(rawRecords.size()); // Let's assume they'll mostly be bookmarks.
            final Map<String, BookmarkFolder> idToSeenFolders = new HashMap<>();
            for (final org.mozilla.gecko.sync.repositories.domain.BookmarkRecord rawRecord : rawRecords) {
                if (rawRecord.isFolder()) {
                    final BookmarkFolder folder = new BookmarkFolder(rawRecord);
                    mutateSeenBookmarkItems(folder, idToSeenBookmarks, idToSeenFolders);
                    idToSeenFolders.put(rawRecord.guid, folder);

                } else if (rawRecord.isBookmark()) {
                    final BookmarkRecord bookmark = new BookmarkRecord(rawRecord);
                    mutateSeenBookmarkItems(bookmark, idToSeenFolders);
                    idToSeenBookmarks.put(rawRecord.guid, bookmark);

                } else if (rawRecord.isQuery() ||
                        rawRecord.isSeparator() ||
                        rawRecord.isLivemark() ||
                        rawRecord.isMicrosummary()) {
                    // Do nothing.

                } else {
                    Log.w(LOGTAG, "Ignoring unknown bookmark raw record type: " + rawRecord.type);
                }
            }

            final BookmarkFolder rootFolder = createRootBookmarkFolder(idToSeenBookmarks, idToSeenFolders);
            makeFoldersImmutable(rootFolder, idToSeenFolders);
            return rootFolder;
        }

        private static void mutateSeenBookmarkItems(final BookmarkFolder folder,
                final Map<String, BookmarkRecord> idToSeenBookmarks, final Map<String, BookmarkFolder> idToSeenFolders) {
            final BookmarkFolder parentFolder = idToSeenFolders.get(folder.underlyingRecord.parentID);
            if (parentFolder != null) {
                folder.parentFolder = parentFolder;
                parentFolder.getSubfolders().add(folder);
            }

            for (final Object childIDObj : folder.underlyingRecord.children) {
                if (!(childIDObj instanceof String)) {
                    Log.w(LOGTAG, "Ignoring child ID obj of unknown type.");
                    continue;
                }
                final String childID = (String) childIDObj;

                final BookmarkRecord childRecord = idToSeenBookmarks.get(childID);
                if (childRecord != null) {
                    folder.getBookmarks().add(childRecord);
                    childRecord.parentFolder = folder;
                }
            }
        }

        private static void mutateSeenBookmarkItems(final BookmarkRecord bookmark,
                final Map<String, BookmarkFolder> idToSeenFolders) {
            final BookmarkFolder parentFolder = idToSeenFolders.get(bookmark.underlyingRecord.parentID);
            if (parentFolder != null) {
                bookmark.parentFolder = parentFolder;
                parentFolder.getBookmarks().add(bookmark);
            }
        }

        private static BookmarkFolder createRootBookmarkFolder(final Map<String, BookmarkRecord> idToSeenBookmarks,
                final Map<String, BookmarkFolder> idToSeenFolders) {
            final BookmarkFolder rootFolder = BookmarkFolder.createRootFolder();
            for (final BookmarkRecord bookmark : idToSeenBookmarks.values()) {
                if (bookmark.underlyingRecord.parentID.equals(BookmarkFolder.ROOT_FOLDER_GUID)) {
                    rootFolder.getBookmarks().add(bookmark);
                }
            }

            for (final BookmarkFolder folder : idToSeenFolders.values()) {
                if (folder.underlyingRecord.parentID.equals(BookmarkFolder.ROOT_FOLDER_GUID)) {
                    rootFolder.getSubfolders().add(folder);
                }
            }

            return rootFolder;
        }

        private static void makeFoldersImmutable(final BookmarkFolder rootFolder, final Map<String, BookmarkFolder> idToSeenBookmarks) {
            rootFolder.makeImmutable();
            for (final BookmarkFolder bookmarkFolder : idToSeenBookmarks.values()) {
                bookmarkFolder.makeImmutable();
            }
        }
    }
}

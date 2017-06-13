/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.sync;

import android.support.annotation.WorkerThread;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecordFactory;
import org.mozilla.sync.FirefoxSyncException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gets the bookmarks for the associated account from Firefox Sync.
 */
class FirefoxSyncBookmarks {

    private static final String BOOKMARKS_COLLECTION = "bookmarks";

    private FirefoxSyncBookmarks() {}

    /**
     * Gets the bookmarks associated with the given account.
     *
     * Both the request and the callback occur on the calling thread (this is unintuitive: issue #3).
     *
     * @param itemLimit The number of items to fetch. If < 0, fetches all items.
     */
    @WorkerThread // network request.
    static void getBlocking(final FirefoxSyncConfig syncConfig, final int itemLimit, final OnSyncComplete<BookmarkFolder> onComplete) {
        final SyncClientBookmarksResourceDelegate resourceDelegate = new SyncClientBookmarksResourceDelegate(syncConfig, onComplete);
        try {
            FirefoxSyncUtils.makeGetRequestForCollection(syncConfig, BOOKMARKS_COLLECTION, getArgs(itemLimit), resourceDelegate);
        } catch (final FirefoxSyncException e) {
            onComplete.onException(e);
        }
    }

    private static Map<String, String> getArgs(final int itemLimit) {
        if (itemLimit < 0) { return null; } // Fetch all items if < 0.

        final Map<String, String> args = new HashMap<>(1);
        args.put("limit", String.valueOf(itemLimit));
        return args;
    }

    private static class SyncClientBookmarksResourceDelegate extends SyncBaseResourceDelegate<BookmarkFolder> {
        SyncClientBookmarksResourceDelegate(final FirefoxSyncConfig syncConfig, final OnSyncComplete<BookmarkFolder> onComplete) {
            super(syncConfig, onComplete);
        }

        @Override
        public void handleResponse(final HttpResponse response, final String responseBody) {
            final List<org.mozilla.gecko.sync.repositories.domain.BookmarkRecord> rawRecords;
            try {
                rawRecords = responseBodyToRawRecords(syncConfig, responseBody, BOOKMARKS_COLLECTION, new BookmarkRecordFactory());
            } catch (final FirefoxSyncException e) {
                onComplete.onException(e);
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
            //
            // Note: we don't handle the case that bookmarks are corrupted or we retrieved a partial bookmarks list.
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

            return createRootBookmarkFolder(idToSeenBookmarks, idToSeenFolders);
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
            // Fetched bookmarks can be orphaned from corruption or because the user specified some number of bookmarks
            // to fetch. When we see an orphan, we add it to the root folder. This is problematic because it does not
            // accurately represent the user's bookmarks and the hierarchy can change across invocations but this was
            // simplest to implement now and easiest to change later (issue #9).
            final BookmarkFolder rootFolder = BookmarkFolder.createRootFolder();
            for (final BookmarkRecord bookmark : idToSeenBookmarks.values()) {
                if (bookmark.underlyingRecord.parentID.equals(BookmarkFolder.ROOT_FOLDER_GUID) ||
                        bookmark.underlyingRecord.parentID == null) { // orphan.
                    rootFolder.getBookmarks().add(bookmark);
                }
            }

            for (final BookmarkFolder folder : idToSeenFolders.values()) {
                if (folder.underlyingRecord.parentID.equals(BookmarkFolder.ROOT_FOLDER_GUID) ||
                        folder.underlyingRecord.parentID == null) { // orphan.
                    rootFolder.getSubfolders().add(folder);
                }
            }

            return rootFolder;
        }
    }
}

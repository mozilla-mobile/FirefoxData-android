package org.mozilla.accountsexample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.mozilla.gecko.background.common.log.writers.LogWriter;
import org.mozilla.sync.FirefoxSync;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.FirefoxSyncGetCollectionException;
import org.mozilla.sync.FirefoxSyncLoginManager;
import org.mozilla.sync.login.FirefoxSyncLoginException;
import org.mozilla.sync.sync.BookmarkFolder;
import org.mozilla.sync.sync.BookmarkRecord;
import org.mozilla.sync.sync.HistoryRecord;
import org.mozilla.sync.sync.PasswordRecord;

import java.util.List;

public class AccountsExampleActivity extends AppCompatActivity {

    private static final String LOGTAG = "AccountsExampleActivity";

    private FirefoxSyncLoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: bad b/c store context. but short-lived... if onActivityResult.
        loginManager = FirefoxSync.getLoginManager(this);
        loginManager.loadStoredSyncAccount(new FirefoxSyncLoginManager.LoginCallback() {
        //loginManager.promptLogin(this, "AccountsExample", new FirefoxSyncLoginManager.LoginCallback() {
            @Override
            public void onSuccess(final FirefoxSyncClient syncClient) {
                // TODO: maybe we should make sure the account is valid before returning here (sync token).
                Log.d(LOGTAG, "onSuccess: load stored account.");
                testSync(syncClient);
            }

            @Override
            public void onFailure(final FirefoxSyncLoginException e) {
                Log.d(LOGTAG, "onFailure: load stored account", e);
            }

            @Override // TODO: rm me!
            public void onUserCancel() { // TODO: rm me!
                Log.d(LOGTAG, "onUserCancel: load stored account");
            }
        });

        /*
        loginManager.promptLogin(this, "AccountsExample", new FirefoxSyncLoginManager.LoginCallback() {
            @Override
            public void onSuccess(final FirefoxSyncClient syncClient) {
                Log.d(LOGTAG, "On success!");
                testSync(syncClient);
            }

            @Override
            public void onFailure(final FirefoxSyncLoginException e) {
                Log.d(LOGTAG, "onFailure: ", e);
            }

            @Override
            public void onUserCancel() {
                Log.d(LOGTAG, "onUserCancel: ");
            }
        });
        */
    }
    
    private void testSync(final FirefoxSyncClient syncClient) {
        final List<HistoryRecord> receivedHistory;
        final List<PasswordRecord> receivedPasswords;
        final BookmarkFolder rootBookmark;
        try {
            receivedHistory = syncClient.getAllHistory().getResult();
            receivedPasswords = syncClient.getAllPasswords().getResult();
            rootBookmark = syncClient.getAllBookmarks().getResult();
        } catch (final FirefoxSyncGetCollectionException e) {
            Log.w(LOGTAG, "testSync: failure to receive! " + e.getFailureReason(), e); // TODO: print failure reason when printing exception? strip non-root Exceptions?
            return;
        }

        for (final HistoryRecord record : receivedHistory) {
            Log.d(LOGTAG, record.getTitle() + ": " + record.getURI());
        }
        for (final PasswordRecord record : receivedPasswords) {
            Log.d(LOGTAG, record.getUsername() + ": " + record.getPassword());
        }
        Log.d(LOGTAG, rootBookmark.getTitle());
        for (final BookmarkRecord record : rootBookmark.getBookmarks()) {
            Log.d(LOGTAG, "root child: " + record.getTitle() + ": " + record.getURI());
        }
        for (final BookmarkFolder folder : rootBookmark.getSubfolders()) {
            Log.d(LOGTAG, "root subfolder: " + folder.getTitle());
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loginManager.onActivityResult(requestCode, resultCode, data);
    }
}

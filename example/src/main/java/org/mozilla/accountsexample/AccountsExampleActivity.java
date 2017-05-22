package org.mozilla.accountsexample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.mozilla.sync.FirefoxSync;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.sync.FirefoxSyncGetCollectionException;
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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginManager = FirefoxSync.getLoginManager(this);
        //loginManager.loadStoredSyncAccount(new LoginManagerCallback());
        loginManager.promptLogin(this, "AccountsExample", new LoginManagerCallback());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loginManager.onActivityResult(requestCode, resultCode, data);
    }

    private static class LoginManagerCallback implements FirefoxSyncLoginManager.LoginCallback {
        @Override
        public void onSuccess(final FirefoxSyncClient syncClient) {
            Log.d(LOGTAG, "onSuccess: load stored account.");
            getSyncAndLog(syncClient);
        }

        // TODO: implement these failure cases later.
        @Override
        public void onFailure(final FirefoxSyncLoginException e) {
            Log.d(LOGTAG, "onFailure: load stored account", e);
            switch (e.getFailureReason()) {
                case ACCOUNT_NEEDS_VERIFICATION: // fall through
                case REQUIRES_LOGIN_PROMPT:
                    // Prompt the user later...
                    break;

                case USER_HAS_NO_LINKED_DEVICES: // fall through
                    // User has to connect some devices before we return success. If this was promptLogin,
                    // the account will be accessible with loadStoredSyncAccount in the future so (fall through)
                case NETWORK_ERROR: // fall through
                case SERVER_ERROR:
                    // Try again later...
                    break;

                case REQUIRES_BACKOFF:
                    final int backoffSeconds = e.getBackoffSeconds();
                    // Try again in at least ^ seconds.
                    break;

                case ASSERTION_FAILURE:
                    // Contact the devs and try again later!
                    break;

                case UNKNOWN:
                    // Try again later...
                    break;
            }
        }

        @Override
        public void onUserCancel() { // not called for loadStoredSyncAccount.
            Log.d(LOGTAG, "onUserCancel: load stored account");
        }

        private void getSyncAndLog(final FirefoxSyncClient syncClient) {
            final List<HistoryRecord> receivedHistory;
            final List<PasswordRecord> receivedPasswords;
            final BookmarkFolder rootBookmark;
            try {
                receivedHistory = syncClient.getAllHistory().getResult();
                receivedPasswords = syncClient.getAllPasswords().getResult();
                rootBookmark = syncClient.getAllBookmarks().getResult();
            } catch (final FirefoxSyncGetCollectionException e) {
                // We could switch on e.getFailureReason() if we wanted to do more specific handling, but
                // ultimately, failure means we should try again later.
                Log.w(LOGTAG, "testSync: failure to receive! " + e.getFailureReason(), e);
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
    }
}

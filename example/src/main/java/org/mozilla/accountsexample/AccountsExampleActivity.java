package org.mozilla.accountsexample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.mozilla.sync.FirefoxSync;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.FirefoxSyncLoginManager;
import org.mozilla.sync.FirefoxSyncLoginException;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;

import java.util.List;

public class AccountsExampleActivity extends AppCompatActivity {

    private static final String LOGTAG = "AccountsExampleActivity";

    FirefoxSyncLoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: bad b/c store context. but short-lived... if onActivityResult.
        loginManager = FirefoxSync.getLoginManager(this);
        loginManager.promptLogin(this, "AccountsExample", new FirefoxSyncLoginManager.LoginCallback() {
            @Override
            public void onSuccess(final FirefoxSyncClient syncClient) {
                Log.d(LOGTAG, "On success!");

                /*
                final List<HistoryRecord> receivedRecords = syncClient.getHistory();
                for (final HistoryRecord record : receivedRecords) {
                    Log.d(LOGTAG, record.title + ": " + record.histURI);
                }
                */

                /*
                final List<BookmarkRecord> receivedBookmarks = syncClient.getBookmarks();
                for (final BookmarkRecord record : receivedBookmarks) {
                    Log.d(LOGTAG, record.title + ": " + record.bookmarkURI);
                }
                */

                final List<PasswordRecord> receivedPasswords = syncClient.getPasswords();
                for (final PasswordRecord record : receivedPasswords) {
                    Log.d(LOGTAG, record.encryptedPassword + ": " + record.encryptedUsername);
                }
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
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loginManager.onActivityResult(requestCode, resultCode, data);
    }
}

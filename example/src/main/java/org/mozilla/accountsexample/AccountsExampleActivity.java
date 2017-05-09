package org.mozilla.accountsexample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.mozilla.sync.FirefoxAccount;
import org.mozilla.sync.FirefoxSync;
import org.mozilla.sync.FirefoxSyncClient;
import org.mozilla.sync.FirefoxSyncLoginManager;
import org.mozilla.sync.LoginSyncException;
import org.mozilla.sync.sync.FirefoxAccountSyncClient;
import org.mozilla.sync.sync.commands.SyncCollectionCallback;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
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
        loginManager = FirefoxSync.getLoginManager();
        loginManager.promptLogin(this, "AccountsExample", new FirefoxSyncLoginManager.LoginCallback() {
            @Override
            public void onSuccess(final FirefoxSyncClient syncClient) {
                Log.d(LOGTAG, "On success!");

            }

            @Override
            public void onFailure(final LoginSyncException e) {
                Log.d(LOGTAG, "onFailure: ", e);
            }
        });

                /*
        final Intent intent = new Intent(this, FirefoxAccountWebViewLoginActivity.class);
        intent.putExtra(FirefoxAccountWebViewLoginActivity.EXTRA_DEBUG_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getProduction());
        startActivityForResult(intent, 10); // TODO: request code.
        */
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loginManager.onActivityResult(requestCode, resultCode, data);

        /*
        if (resultCode == FirefoxAccountWebViewLoginActivity.RESULT_OK) {
            final FirefoxAccount account = new FirefoxAccountDevelopmentStore(this).loadFirefoxAccount();
            if (account == null) {
                Log.d("lol", "Nothing.");
            } else {
                Log.d("lol", account.uid);
                sync(account);
            }
        } else if (resultCode == FirefoxAccountWebViewLoginActivity.RESULT_CANCELED) {
            Log.d("lol", "User canceled login");
        } else {
            Log.d("lol", "error!");
        }
        */
    }

    private void sync(final FirefoxAccount account) {
        FirefoxAccountSyncClient client = new FirefoxAccountSyncClient(account);
        // TODO: should not be anonymous if don't want to leak context.
        client.getBookmarks(this, new SyncCollectionCallback<BookmarkRecord>() {
            @Override
            public void onReceive(final List<BookmarkRecord> receivedRecords) {
                Log.e(LOGTAG, "onReceive: bookmarks!");
                for (final BookmarkRecord record : receivedRecords) {
                    Log.d(LOGTAG, record.title + ": " + record.bookmarkURI);
                }
            }

            @Override public void onError(final Exception e) { Log.e(LOGTAG, "onError: error!", e); }
        });

        client.getHistory(this, 1000, new SyncCollectionCallback<HistoryRecord>() {
            @Override
            public void onReceive(final List<HistoryRecord> receivedRecords) {
                Log.e(LOGTAG, "onReceive: history!");
                for (final HistoryRecord record : receivedRecords) {
                    Log.d(LOGTAG, record.title + ": " + record.histURI);
                }
            }

            @Override public void onError(final Exception e) { Log.e(LOGTAG, "onError: error!", e); }
        });

        client.getPasswords(this, new SyncCollectionCallback<PasswordRecord>() {
            @Override
            public void onReceive(final List<PasswordRecord> receivedRecords) {
                Log.e(LOGTAG, "onReceive: passwords!");
                for (final PasswordRecord record : receivedRecords) {
                    Log.d(LOGTAG, record.encryptedPassword + ": " + record.encryptedUsername);
                }
            }

            @Override public void onError(final Exception e) { Log.e(LOGTAG, "onError: error!", e); }
        });
    }
}

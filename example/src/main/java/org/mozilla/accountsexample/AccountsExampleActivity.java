/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accountsexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.mozilla.sync.FirefoxSync;
import org.mozilla.sync.FirefoxSyncException;
import org.mozilla.sync.sync.FirefoxSyncClient;
import org.mozilla.sync.login.FirefoxSyncLoginManager;
import org.mozilla.sync.sync.BookmarkFolder;
import org.mozilla.sync.sync.BookmarkRecord;
import org.mozilla.sync.sync.HistoryRecord;
import org.mozilla.sync.sync.PasswordRecord;

import java.util.List;

public class AccountsExampleActivity extends AppCompatActivity {

    private static final String LOGTAG = "AccountsExampleActivity";

    private static final String KEY_HAS_USER_CANCELLED = "fx-sync-has-user-cancelled";

    private FirefoxSyncLoginManager loginManager;

    private Button signOutButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        signOutButton = (Button) findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.setEnabled(false);
                loginManager.signOut();
            }
        });

        // If we have an account, we want to refresh our data on start-up. If we don't have
        // an account, we'll prompt the user if they've manually cancelled a login request.
        loginManager = FirefoxSync.getLoginManager(this);
        final SharedPreferences sharedPrefs = getSharedPreferences("fx-sync", 0);
        final FirefoxSyncLoginManager.LoginCallback loginCallback = new LoginManagerCallback(sharedPrefs, signOutButton);
        if (loginManager.isSignedIn()) {
            loginManager.loadStoredSyncAccount(loginCallback);
        } else if (!sharedPrefs.getBoolean(KEY_HAS_USER_CANCELLED, false)){
            loginManager.promptLogin(this, "AccountsExample", loginCallback);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loginManager.onActivityResult(requestCode, resultCode, data);
    }

    private class LoginManagerCallback implements FirefoxSyncLoginManager.LoginCallback {
        private final SharedPreferences sharedPrefs;
        private final Button signOutButton;

        private LoginManagerCallback(final SharedPreferences sharedPrefs, final Button signOutButton) {
            this.sharedPrefs = sharedPrefs;
            this.signOutButton = signOutButton;
        }

        @Override
        public void onSuccess(final FirefoxSyncClient syncClient) {
            Log.d(LOGTAG, "onSuccess: load stored account.");
            getSyncAndLog(syncClient);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    signOutButton.setEnabled(true);
                }
            });
        }

        // TODO: implement these failure cases later.
        @Override
        public void onFailure(final FirefoxSyncException e) {
            Log.d(LOGTAG, "onFailure: load stored account", e);
            // Oh well, we'll try again next run.
        }

        @Override
        public void onUserCancel() { // not called for loadStoredSyncAccount.
            Log.d(LOGTAG, "onUserCancel: load stored account");
            sharedPrefs.edit().putBoolean(KEY_HAS_USER_CANCELLED, true).apply();
            // Tell user they can log in from the settings.
        }

        private void getSyncAndLog(final FirefoxSyncClient syncClient) {
            final List<HistoryRecord> receivedHistory;
            final List<PasswordRecord> receivedPasswords;
            final BookmarkFolder rootBookmark;
            try {
                receivedHistory = syncClient.getAllHistory().getResult();
                receivedPasswords = syncClient.getAllPasswords().getResult();
                rootBookmark = syncClient.getAllBookmarks().getResult();
            } catch (final FirefoxSyncException e) {
                // We could switch on e.getFailureReason() if we wanted to do more specific handling, but
                // ultimately, failure means we should try again later.
                Log.w(LOGTAG, "testSync: failure to receive! ", e);
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

package org.mozilla.accountsexample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.accounts.FirefoxAccountEndpointConfig;
import org.mozilla.accounts.login.FirefoxAccountLoginWebViewActivity;
import org.mozilla.accounts.sync.FirefoxAccountSyncClient;
import org.mozilla.accounts.sync.callbacks.SyncHistoryCallback;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AccountsExampleActivity extends AppCompatActivity {

    private static final String LOGTAG = "AccountsExampleActivity";

    // TODO: maybe sync should be a service.
    private final HandlerThread bgThread = new HandlerThread("background");
    private final Executor executor = Executors.newSingleThreadExecutor(); // TODO: merge bgTHread & executor?
    private Handler bgHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());

        final Intent intent = new Intent(this, FirefoxAccountLoginWebViewActivity.class);
        intent.putExtra(FirefoxAccountLoginWebViewActivity.EXTRA_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getStage());
        startActivityForResult(intent, 10); // TODO: request code.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bgThread.quit();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == FirefoxAccountLoginWebViewActivity.RESULT_OK) {
            final FirefoxAccount account = new FirefoxAccountDevelopmentStore(this).loadFirefoxAccount();
            if (account == null) {
                Log.d("lol", "Nothing.");
            } else {
                Log.d("lol", account.uid);
                sync(account);
            }
        } else if (resultCode == FirefoxAccountLoginWebViewActivity.RESULT_CANCELED) {
            Log.d("lol", "User canceled login");
        } else {
            Log.d("lol", "error!");
        }
    }

    private void sync(final FirefoxAccount account) {
        FirefoxAccountSyncClient client = new FirefoxAccountSyncClient(account);
        // TODO: should not be anonymous if don't want to leak context.
        client.getHistory(this, 1000, new SyncHistoryCallback() {
            @Override
            public void onReceive(final List<HistoryRecord> historyRecords) {
                Log.e(LOGTAG, "onReceive: error!");
            }

            @Override
            public void onError(final Exception e) {
                Log.e(LOGTAG, "onError: error!", e);
            }
        });
    }
}

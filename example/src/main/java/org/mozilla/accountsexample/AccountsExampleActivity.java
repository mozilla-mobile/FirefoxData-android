package org.mozilla.accountsexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.accounts.FirefoxAccountEndpointConfig;
import org.mozilla.accounts.login.FirefoxAccountLoginWebViewActivity;
import org.mozilla.gecko.background.fxa.FxAccountClient;
import org.mozilla.gecko.background.fxa.FxAccountClient20;
import org.mozilla.gecko.browserid.BrowserIDKeyPair;
import org.mozilla.gecko.fxa.login.Engaged;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.FxAccountLoginTransition;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.fxa.login.StateFactory;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;

public class AccountsExampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Intent intent = new Intent(this, FirefoxAccountLoginWebViewActivity.class);
        intent.putExtra(FirefoxAccountLoginWebViewActivity.EXTRA_ACCOUNT_CONFIG, FirefoxAccountEndpointConfig.getStableDev());
        startActivityForResult(intent, 10); // TODO: request code.
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
                advance(account);
            }
        } else if (resultCode == FirefoxAccountLoginWebViewActivity.RESULT_CANCELED) {
            Log.d("lol", "User canceled login");
        } else {
            Log.d("lol", "error!");
        }
    }

    private void advance(final FirefoxAccount account) {
        // TODO: better handle NetworkonMainthread exception?
        // TODO: Account store concurrency. (only allow one account?)
        // TODO: if not verified?
        HandlerThread ht = new HandlerThread("background");
        ht.start();
        Handler handler = new Handler(ht.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                new FxAccountLoginStateMachine().advance(account.accountState, State.StateLabel.Married, new FxAccountLoginStateMachine.LoginStateMachineDelegate() {
                    @Override
                    public FxAccountClient getClient() {
                        return new FxAccountClient20(account.endpointConfig.authServerURL.toString(), Executors.newSingleThreadExecutor());
                    }

                    @Override
                    public long getCertificateDurationInMilliseconds() { return 12 * 60 * 60 * 1000; }

                    @Override
                    public long getAssertionDurationInMilliseconds() { return 15 * 60 * 1000; }

                    @Override
                    public void handleTransition(final FxAccountLoginTransition.Transition transition, final State state) {
                        Log.d("lol", "transitioning: " + transition + " - " + state.getStateLabel().toString());
                    }

                    @Override
                    public void handleFinal(final State state) {
                        final FirefoxAccount newAccount = new FirefoxAccount(account.email, account.uid, state, account.endpointConfig);
                        new FirefoxAccountDevelopmentStore(AccountsExampleActivity.this).saveFirefoxAccount(newAccount);
                        Log.d("lol", "newAccount! " + state);
                    }

                    @Override
                    public BrowserIDKeyPair generateKeyPair() throws NoSuchAlgorithmException {
                        return StateFactory.generateKeyPair();
                    }
                });
            }
        });
    }
}

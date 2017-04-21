package org.mozilla.accountsexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.accounts.FirefoxAccountEndpointConfig;
import org.mozilla.accounts.login.FirefoxAccountLoginStateMachineDelegate;
import org.mozilla.accounts.login.FirefoxAccountLoginWebViewActivity;
import org.mozilla.gecko.background.fxa.FxAccountUtils;
import org.mozilla.gecko.background.fxa.SkewHandler;
import org.mozilla.gecko.browserid.JSONWebTokenUtils;
import org.mozilla.gecko.fxa.FxAccountConstants;
import org.mozilla.gecko.fxa.login.FxAccountLoginStateMachine;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.fxa.login.State;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SharedPreferencesClientsDataDelegate;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.sync.stage.GlobalSyncStage;
import org.mozilla.gecko.tokenserver.TokenServerClient;
import org.mozilla.gecko.tokenserver.TokenServerClientDelegate;
import org.mozilla.gecko.tokenserver.TokenServerException;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
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
        // TODO: what happens on failure?
        bgHandler.post(new Runnable() {
            @Override
            public void run() {
                // TODO: this reference is context - leak!.
                // TODO: style.
                new FxAccountLoginStateMachine().advance(account.accountState, State.StateLabel.Married, new FirefoxAccountLoginStateMachineDelegate(AccountsExampleActivity.this, account, new FirefoxAccountLoginStateMachineDelegate.LoginHandler() {
                    @Override
                    public void handleNotMarried(final State notMarriedState) {
                    }

                    @Override
                    public void handleMarried(final FirefoxAccount account, final Married married) {
                        sync(account, married);
                    }
                }));
            }
        });
    }

    private void sync(final FirefoxAccount account, final Married marriedState) {
        final TokenServerClientDelegate tokenServerClientDelegate = new TokenServerClientDelegate() {
            @Override public String getUserAgent() { return FxAccountConstants.USER_AGENT; }

            @Override
            public void handleFailure(final TokenServerException e) {
                Log.e(LOGTAG, "handleFailure: tokenException.", e);
            }

            @Override
            public void handleError(final Exception e) {
                Log.e(LOGTAG, "handleError: tokenException.", e);
            }

            @Override
            public void handleBackoff(final int backoffSeconds) {
                Log.w(LOGTAG, "handleBackoff: " + backoffSeconds);
            }

            @Override
            public void handleSuccess(final TokenServerToken token) {
                // We expect Sync to upload large sets of records. Calculating the
                // payload verification hash for these record sets could be expensive,
                // so we explicitly do not send payload verification hashes to the
                // Sync storage endpoint.
                final boolean includePayloadVerificationHash = false;
                final SharedPreferences sharedPrefs = getSharedPreferences("Sync-thing", Utils.SHARED_PREFERENCES_MODE); // TODO: path.

                final URI storageServerURI;
                final AuthHeaderProvider authHeaderProvider;
                final SyncConfiguration syncConfig;
                try {
                    // We compute skew over time using SkewHandler. This yields an unchanging
                    // skew adjustment that the HawkAuthHeaderProvider uses to adjust its
                    // timestamps. Eventually we might want this to adapt within the scope of a
                    // global session.
                    storageServerURI = new URI(token.endpoint);
                    final String storageHostname = storageServerURI.getHost();
                    final SkewHandler storageServerSkewHandler = SkewHandler.getSkewHandlerForHostname(storageHostname);
                    final long storageServerSkew = storageServerSkewHandler.getSkewInSeconds();

                    authHeaderProvider = new HawkAuthHeaderProvider(token.id, token.key.getBytes("UTF-8"), includePayloadVerificationHash, storageServerSkew);

                    syncConfig = new SyncConfiguration(token.uid, authHeaderProvider, sharedPrefs, marriedState.getSyncKeyBundle());
                    syncConfig.stagesToSync = null; // sync all.
                    syncConfig.setClusterURL(storageServerURI);
                    final GlobalSession session = new GlobalSession(syncConfig, new SessionCallback(),
                            AccountsExampleActivity.this, new SharedPreferencesClientsDataDelegate(sharedPrefs, AccountsExampleActivity.this)); // todo: CONTEXT
                    session.start(System.currentTimeMillis() + 60000L); // todo: CORRECT?
                } catch (final Exception e) {
                    Log.e(LOGTAG, "handleSuccess: Failed to sync", e);
                }
            }
        };

        final URI tokenServerURI = account.endpointConfig.syncConfig.tokenServerURL;
        final String assertion;
        try {
            assertion = marriedState.generateAssertion(FxAccountUtils.getAudienceForURL(tokenServerURI.toString()), JSONWebTokenUtils.DEFAULT_ASSERTION_ISSUER);
        } catch (NonObjectJSONException | IOException | GeneralSecurityException | URISyntaxException e) {
            Log.e(LOGTAG, "Failed to sync: ", e);
            return;
        }

        final TokenServerClient tokenServerClient = new TokenServerClient(tokenServerURI, executor);
        tokenServerClient.getTokenFromBrowserIDAssertion(assertion, true, marriedState.getClientState(), tokenServerClientDelegate);
    }

    private static class SessionCallback implements GlobalSessionCallback {
        @Override
        public void requestBackoff(final long backoff) {

        }

        @Override
        public void informUnauthorizedResponse(final GlobalSession globalSession, final URI oldClusterURL) {

        }

        @Override
        public void informUpgradeRequiredResponse(final GlobalSession session) {

        }

        @Override
        public void informMigrated(final GlobalSession session) {

        }

        @Override
        public void handleAborted(final GlobalSession globalSession, final String reason) {
            Log.e(LOGTAG, "handleAborted: global session apported: " + reason);
        }

        @Override
        public void handleError(final GlobalSession globalSession, final Exception ex) {
            Log.e(LOGTAG, "handleError: global session failed", ex);
        }

        @Override
        public void handleSuccess(final GlobalSession globalSession) {
            Log.d(LOGTAG, "handleSuccess: global session succeeded.");
        }

        @Override
        public void handleStageCompleted(final GlobalSyncStage.Stage currentState, final GlobalSession globalSession) {

        }

        @Override
        public void handleIncompleteStage(final GlobalSyncStage.Stage currentState, final GlobalSession globalSession) {

        }

        @Override
        public void handleFullSyncNecessary() {

        }

        @Override
        public boolean shouldBackOffStorage() {
            return false;
        }
    }
}

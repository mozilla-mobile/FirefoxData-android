package org.mozilla.accountsexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.accounts.FirefoxAccountEndpointConfig;
import org.mozilla.accounts.login.FirefoxAccountLoginWebViewActivity;
import org.mozilla.accounts.sync.FirefoxAccountSyncClient;
import org.mozilla.accounts.sync.FirefoxAccountSyncTokenAccessor;
import org.mozilla.gecko.background.fxa.SkewHandler;
import org.mozilla.gecko.fxa.login.Married;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.AuthHeaderProvider;
import org.mozilla.gecko.sync.net.BaseResource;
import org.mozilla.gecko.sync.net.BaseResourceDelegate;
import org.mozilla.gecko.sync.net.HawkAuthHeaderProvider;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecordFactory;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.tokenserver.TokenServerToken;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Scanner;
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
        FirefoxAccountSyncClient client = new FirefoxAccountSyncClient(this, account);
        client.ensureSyncToken(new FirefoxAccountSyncTokenAccessor.TokenCallback() {
            @Override
            public void onError(final Exception e) {
                Log.w(LOGTAG, "Could not retrieve sync token.", e);
            }

            @Override
            public void onTokenReceived(final TokenServerToken token) {
                final FirefoxAccount updatedAccount = new FirefoxAccountDevelopmentStore(AccountsExampleActivity.this).loadFirefoxAccount();

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

                    syncConfig = new SyncConfiguration(token.uid, authHeaderProvider, sharedPrefs, ((Married) updatedAccount.accountState).getSyncKeyBundle());
                    syncConfig.stagesToSync = null; // sync all.
                    syncConfig.setClusterURL(storageServerURI);


                    //final URI uri = new URI(storageServerURI.toString() + "/info/collections");
                    final URI uri = new URI(storageServerURI.toString() + "/storage/history?full=1&limit=1000");
                    final BaseResource resource = new BaseResource(uri);
                    resource.delegate = new BaseResourceDelegate(resource) {
                        @Override public AuthHeaderProvider getAuthHeaderProvider() { return authHeaderProvider; }
                        @Override public String getUserAgent() { return null; }

                        @Override
                        public void handleHttpResponse(final HttpResponse response) {
                            Log.d(LOGTAG, response.toString());
                            Scanner s = null;
                            try {
                                s = new Scanner(response.getEntity().getContent()).useDelimiter("\\A");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String result = s.hasNext() ? s.next() : "";
                            final JSONArray array;
                            final HistoryRecordFactory fact = new HistoryRecordFactory();
                            try {
                                syncConfig.setCollectionKeys(syncConfig.persistedCryptoKeys().keys()); // tODO: might be null & shit.
                                final KeyBundle bundle = syncConfig.getCollectionKeys().keyBundleForCollection("history");
                                array = new JSONArray(result);
                                for (int i = 0; i < array.length(); ++i) {
                                    final JSONObject obj = array.getJSONObject(i);
                                    final Record record = new HistoryRecord(obj.getString("id"));
                                    final CryptoRecord crecord = new CryptoRecord(record);
                                    crecord.payload = new ExtendedJSONObject(obj.getString("payload"));
                                    crecord.setKeyBundle(bundle);
                                    crecord.decrypt();
                                    final HistoryRecord hrecord = (HistoryRecord) fact.createRecord(crecord);
                                    Log.d(LOGTAG, hrecord.histURI);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                return;
                            } catch (NonObjectJSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (NoCollectionKeysSetException e) {
                                e.printStackTrace();
                            } catch (CryptoException e) {
                                e.printStackTrace();
                            }
                            Log.d(LOGTAG, result);
                        }

                        @Override
                        public void handleHttpProtocolException(final ClientProtocolException e) {
                            Log.e(LOGTAG, e.toString());
                        }

                        @Override
                        public void handleHttpIOException(final IOException e) {
                            Log.e(LOGTAG, e.toString());
                        }

                        @Override
                        public void handleTransportException(final GeneralSecurityException e) {
                            Log.e(LOGTAG, e.toString());
                        }
                    };
                    resource.get();
                } catch (final Exception e) {
                    Log.e(LOGTAG, "handleSuccess: Failed to sync", e);
                }
            }
        });
    }
}

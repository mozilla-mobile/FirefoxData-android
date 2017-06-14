/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.sync.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.sync.impl.FirefoxSyncShared;
import org.mozilla.gecko.R;
import org.mozilla.sync.impl.FirefoxAccount;
import org.mozilla.sync.impl.FirefoxAccountEndpointConfig;
import org.mozilla.util.ResourcesUtils;
import org.mozilla.util.WebViewUtils;

/**
 * An Activity that starts a web view and allows the user to log into their Firefox Account. This Activity has no
 * knowledge of existing Firefox Account state: if you start it, it attempt to prompt the user to log in.
 *
 * <b>This is not intended to be a public API</b>: please access via {@link org.mozilla.sync.FirefoxSync#getLoginManager(Context)}.
 *
 * At the time of writing, the login page maintains browser local storage and will pre-fill a previously entered
 * account email.
 *
 * Input Intent args:
 * <ul>
 *     <li>{@link #EXTRA_DEBUG_ACCOUNT_CONFIG}: Pass in an alternative {@link FirefoxAccountEndpointConfig} to point
 *     to non-production servers.</li>
 * </ul>
 *
 * The {@code resultCode} can be {@link #RESULT_ERROR}, or the standard {@link #RESULT_OK} & {@link #RESULT_CANCELED}.
 * The returned Intent will have the action {@link #ACTION_WEB_VIEW_LOGIN_RETURN}.
 *
 * Output Intent args:
 * <ul>
 *     <li>{@link #EXTRA_ACCOUNT}: on success, the returned account. This can be verified or unverified.</li>
 *     <li>{@link #EXTRA_FAILURE_REASON}: on error, a String explaining what went wrong.
 * </ul>
 *
 * The previous implementation, FxAccountWebFlowActivity & friends, are heavily dependent on Gecko, so we rewrote it.
 * This implementation is heavily inspired by Firefox for iOS's FxAContentViewController:
 *   https://github.com/mozilla-mobile/firefox-ios/blob/02467f8015e5936425dfc7355c290f94c56ea57a/Client/Frontend/Settings/FxAContentViewController.swift
 */
public class FirefoxSyncWebViewLoginActivity extends AppCompatActivity {

    private static final String LOGTAG = FirefoxSyncShared.LOGTAG;

    // Input values.
    static final String EXTRA_DEBUG_ACCOUNT_CONFIG = "org.mozilla.sync.login.extra.debug-account-config";

    // Return values.
    static final String ACTION_WEB_VIEW_LOGIN_RETURN = "org.mozilla.sync.login.action.web-view-login-return";
    static final String EXTRA_ACCOUNT = "org.mozilla.sync.login.extra.account";
    static final String EXTRA_FAILURE_REASON = "org.mozilla.sync.login.extra.failure-reason";
    static final int RESULT_ERROR = -2; // CANCELED (0) & OK (-1) on Activity super class.

    private static final String JS_INTERFACE_OBJ = "firefoxAccountLogin";

    private WebView webView;
    private String script;

    private FirefoxAccountEndpointConfig endpointConfig;
    private String webViewURL;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: we force portrait so we don't have to handle restoring the WebView -
        // see this activity declaration in the AndroidManifest for motivations.

        setContentView(R.layout.activity_fxaccount_login_web_view);
        setSupportActionBar((Toolbar) findViewById(R.id.fxaccount_login_toolbar));
        webView = (WebView) findViewById(R.id.fxaccount_login_web_view);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Code in this block must be called before initWebView.
        script = ResourcesUtils.getStringFromRawResUnsafe(this, R.raw.firefox_account_login);
        initFromIntent();

        initWebView();
    }

    private void initFromIntent() {
        final Intent intent = getIntent();
        endpointConfig = intent.getParcelableExtra(EXTRA_DEBUG_ACCOUNT_CONFIG);
        if (endpointConfig == null) {
            endpointConfig = FirefoxAccountEndpointConfig.getProduction();
        }

        webViewURL = endpointConfig.signInURL.toString();
    }

    private void initWebView() {
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // needed for FxA login.
        webView.setWebViewClient(new ScriptInjectionWebViewClient(script));

        // It's recommended JS interface is used on SDK min 17+ because on earlier versions the
        // page's JS can use reflection to call Java methods. However, since we trust the page,
        // we shouldn't be vulnerable.
        webView.addJavascriptInterface(new JSInterface(), JS_INTERFACE_OBJ);
        webView.loadUrl(webViewURL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // If we set a result, that value will be returned. Otherwise, RESULT_CANCELED is used.
        onBackPressed();
        return true;
    }

    private class JSInterface {
        @JavascriptInterface
        public void onCommand(final String command, final String messageID, final String data) {
            // API defined by https://github.com/mozilla/fxa-content-server/blob/master/docs/relier-communication-protocols/fx-webchannel.md
            if (command == null) { Log.e(LOGTAG, "onCommand: received null command. Ignoring..."); return; }

            switch (command) {
                case "fxaccounts:loaded": onLoaded(); break;
                case "fxaccounts:can_link_account": onCanLinkAccount(command, messageID, data); break;
                case "fxaccounts:login": onLogin(data); break;

                case "fxaccounts_delete_account": // fall through
                case "fxaccount:change_password": // fall through
                case "profile:change":
                    // These are known events but we should never receive them since we're just signing in.
                    Log.w(LOGTAG, "onCommand: ignoring known but unexpected command: " + command);
                    break;

                default:
                    Log.w(LOGTAG, "onCommand: ignoring unknown command: " + command);
            }
        }
    }

    private void onCanLinkAccount(final String command, final String messageId, final String inputData) {
        final JSONObject outputData = new JSONObject();
        try {
            // afaik, "can link account" asks us if the user should be able to sign into a specific account.
            // Since this class is ignorant of current sign in state, we always say "yes" & return true.
            outputData.put("ok", true);
        } catch (final JSONException e) {
            throw new IllegalStateException("Expected hard-coded JSONObject creation to be valid.");
        }
        injectResponse(command, messageId, outputData);
    }

    private void onLogin(@Nullable final String data) {
        // The user has signed in to a Firefox Account. We're done!
        final FirefoxAccount account = FirefoxAccount.fromWebFlow(endpointConfig, data);
        if (account == null) {
            Log.e(LOGTAG, "Account received from server is corrupted. Returning from login...");
            setResultForFailureReason("Account received from server is corrupted.");
            finish();
            return;
        }

        final Intent resultIntent = new Intent(ACTION_WEB_VIEW_LOGIN_RETURN);
        resultIntent.putExtra(EXTRA_ACCOUNT, account);
        setResult(RESULT_OK, resultIntent);

        if (!account.accountState.verified) {
            // User should stay in the flow to verify their account. However, we don't get notified when the account is
            // verified so the user has to manually close the view.
            return;
        }

        finish();
    }

    private void onLoaded() {
        // If we had a loading time-out (issue #2), here is where we would invalidate it.
    }

    private void injectResponse(final String command, final String messageID, final JSONObject data) {
        final String customEventArg = getCustomEventJSONStr(command, messageID, data);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // WebView methods must be called from UiThread.
                final String script = "window.dispatchEvent(" +
                        "new CustomEvent('WebChannelMessageToContent', " + customEventArg + "));";
                WebViewUtils.evalJS(webView, script);
            }
        });
    }

    /**
     * Returns the second argument to CustomEvent, as defined by:
     *   https://github.com/mozilla/fxa-content-server/blob/master/docs/relier-communication-protocols/fx-webchannel.md#response-format
     */
    private String getCustomEventJSONStr(final String command, final String messageID, @Nullable final JSONObject data) {
        final JSONObject obj = new JSONObject();
        final JSONObject messageObj = new JSONObject();
        final JSONObject detailObj = new JSONObject();
        try {
            messageObj.put("command", command);
            messageObj.put("messageId", messageID);
            if (data != null) { messageObj.put("data", data); }

            detailObj.put("message", messageObj);
            detailObj.put("id", "account_updates"); // it might be more correct to pass this in from JS, than hardcode it.

            obj.put("detail", detailObj);
        } catch (final JSONException e) {
            // Throw because this is largely hard-coded so likely to be developer error.
            throw new IllegalStateException("Failed to create injected JSON object", e);
        }
        return obj.toString();
    }

    private void setResultForFailureReason(final String failureReason) {
        final Intent resultIntent = new Intent(ACTION_WEB_VIEW_LOGIN_RETURN);
        resultIntent.putExtra(EXTRA_FAILURE_REASON, failureReason);
        setResult(RESULT_ERROR, resultIntent);
    }
}

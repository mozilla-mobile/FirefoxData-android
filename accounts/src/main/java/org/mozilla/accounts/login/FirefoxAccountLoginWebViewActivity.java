/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.login;

import android.app.Activity;
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
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountEndpointConfig;
import org.mozilla.gecko.R;
import org.mozilla.util.ResourcesUtil;
import org.mozilla.util.WebViewUtil;

/**
 * An Activity that starts a web view and allows the user to log into their Firefox Account.
 *
 * The previous implementation, FxAccountWebFlowActivity & friends, are heavily dependent on Gecko, so we rewrote it.
 * This implementation is heavily inspired by Firefox for iOS's FxAContentViewController:
 *   https://github.com/mozilla-mobile/firefox-ios/blob/02467f8015e5936425dfc7355c290f94c56ea57a/Client/Frontend/Settings/FxAContentViewController.swift
 *
 * TODO:
 *  - add loading timeout
 *  - assumes never used when user already logged in.
 *  - test more complex flows, like verification, failed password.
 *  - return or persist data.
 *  - add docs on how class should be used.
 */
public class FirefoxAccountLoginWebViewActivity extends AppCompatActivity {

    private static final String LOGTAG = "lol";

    public static final String EXTRA_ACCOUNT_CONFIG = "org.mozilla.accounts.config";

    private static final String JS_INTERFACE_OBJ = "firefoxAccountLogin";

    private WebView webView;
    private String script;

    private FirefoxAccountEndpointConfig endpointConfig;
    private String webViewURL;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fxaccount_login_web_view);
        setSupportActionBar((Toolbar) findViewById(R.id.fxaccount_login_toolbar));
        webView = (WebView) findViewById(R.id.fxaccount_login_web_view);

        // Code in this block must be called before initWebView.
        script = ResourcesUtil.getStringFromRawResUnsafe(this, R.raw.firefox_account_login);
        initFromIntent();

        initWebView();
    }

    private void initFromIntent() {
        final Intent intent = getIntent();
        endpointConfig = intent.getParcelableExtra(EXTRA_ACCOUNT_CONFIG);
        if (endpointConfig == null) { throw new IllegalArgumentException("Expected EXTRA_ACCOUNT_CONFIG with intent."); }

        webViewURL = endpointConfig.signInURL.toString();
    }

    private void initWebView() {
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // needed for FxA login.
        webView.setWebViewClient(new ScriptInjectionWebViewClient(script));
        webView.addJavascriptInterface(new JSInterface(), JS_INTERFACE_OBJ); // TODO: min SDK 17?
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

    private class JSInterface {
        @JavascriptInterface
        public void onCommand(final String command, final String data) {
            if (command == null) { Log.e(LOGTAG, "onCommand: received null command. Ignoring..."); return; }
            // TODO: ios has a work-around: if not loaded and command is not loaded, then call onLoaded again.

            Log.d(LOGTAG, "onCommand: " + command);
            switch (command) {
                case "can_link_account": onCanLinkAccount(); break;
                case "loaded": onLoaded(); break;
                case "login": onLogin(data); break;
                case "sessionStatus": onSessionStatus(); break;
                case "sign_out": onSignOut(); break;

                default:
                    Log.w(LOGTAG, "Received unknown command: " + command);
            }
        }
    }

    private void onCanLinkAccount() {
        // TODO: confirm a relink?
        injectMessage("can_link_account", true);
    }

    private void onSessionStatus() {
        // We're not signed in to a Firefox Account at this time, which we signal by returning an error.
        injectMessage("error");
    }

    private void onSignOut() {
        // We're not signed in to a Firefox Account at this time. We should never get a sign out message!
        injectMessage("error");
    }

    private void onLogin(@Nullable final String data) {
        // The user has signed in to a Firefox Account. We're done!
        injectMessage("login");

        // todo: error out before or after login message?
        final FirefoxAccount account = FirefoxAccount.fromWebFlow(endpointConfig, data);
        if (account == null) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        final Intent result = new Intent();
        //result.putExtra("extra.account", account);
        result.putExtra("lol", account.email);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private void onLoaded() {
        // todo: invalidate loading timeout.
    }

    private void injectMessage(final String statusStr) {
        injectMessage(statusStr, false);
    }

    private void injectMessage(final String statusStr, final boolean hasData) {
        final String jsonStr = getInjectionJSONStr(statusStr, hasData);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // WebView methods must be called from UiThread.
                final String script = "window.postMessage(" + jsonStr + ", '" + webViewURL + "');";
                WebViewUtil.evalJS(webView, script);
            }
        });
    }

    /**
     * @param hasData true if the "content" object should include {"data": {"ok": true}}, false otherwise.
     */
    private String getInjectionJSONStr(final String statusStr, final boolean hasData) {
        final JSONObject obj = new JSONObject();
        final JSONObject contentObj = new JSONObject();
        final JSONObject dataObj = new JSONObject();
        try {
            if (hasData) {
                dataObj.put("ok", true);
                contentObj.put("data", dataObj);
            }
            contentObj.put("status", statusStr);
            obj.put("content", contentObj);
            obj.put("type", "message");
        } catch (final JSONException e) {
            // Throw because this is largely hard-coded so likely to be developer error.
            throw new IllegalStateException("Failed to create injected JSON object", e);
        }
        return obj.toString();
    }
}

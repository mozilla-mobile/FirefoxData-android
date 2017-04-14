/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.accounts.login;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.mozilla.util.WebViewUtil;

/**
 * A WebViewClient that injects the given script into the first page it loads.
 */
class ScriptInjectionWebViewClient extends WebViewClient {

    private final String script;
    private boolean hasInjectedScript = false;

    ScriptInjectionWebViewClient(final String script) {
        this.script = script;
    }

    @Override
    public void onPageFinished(final WebView view, final String url) {
        // This method is called multiple times for some reason: ensure script is injected only once.
        if (!hasInjectedScript) {
            hasInjectedScript = true;
            WebViewUtil.evalJS(view, script);
        }
    }
}


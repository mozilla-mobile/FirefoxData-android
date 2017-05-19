/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

"use strict";

/*
 * Transfer fxa-content-server events to the embedding webview. API is defined by:
 *   https://github.com/mozilla/fxa-content-server/blob/master/docs/relier-communication-protocols/fx-webchannel.md
 */
function fxaHandleCommand(evt) {
    var detail = evt.detail;
    if (detail.id !== 'account_updates') {
        console.log('FirefoxAccount: ignoring unknown web channel event id: ' + evt.id);
        return;
    }

    var payload = detail.message;
    var data = JSON.stringify(payload.data); // Java can't receive objects so we stringify to avoid passing in params individually.
    firefoxAccountLogin.onCommand(payload.command, payload.messageId, data); // this object is defined by the embedding web view.
};

window.addEventListener("WebChannelMessageToChrome", fxaHandleCommand);

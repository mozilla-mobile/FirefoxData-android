/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Transfer fxa-content-server events to the embedding webview.
 */

"use strict";

function fxaHandleCommand(evt) {
    var detail = evt.detail;
    var data = JSON.stringify(detail.data); // Java can't receive objects so we stringify to avoid passing in params individually.
    firefoxAccountLogin.onCommand(detail.command, data); // this object is defined by the embedding web view.
};

window.addEventListener("FirefoxAccountsCommand", fxaHandleCommand);

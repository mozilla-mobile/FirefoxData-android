//#filter substitution
/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko;

import android.os.Build;

// We imported AppConstants.java.in, then rm'd stuff that got in the way and added todos to used stuff
// that I don't know what to set it to. TODO: clarify changes.

/**
 * A collection of constants that pertain to the build and runtime state of the
 * application. Typically these are sourced from build-time definitions (see
 * Makefile.in). This is a Java-side substitute for nsIXULAppInfo, amongst
 * other things.
 *
 * See also SysInfo.java, which includes some of the values available from
 * nsSystemInfo inside Gecko.
 */
// Normally, we'd annotate with @RobocopTarget.  Since AppConstants is compiled
// before RobocopTarget, we instead add o.m.g.AppConstants directly to the
// Proguard configuration.
public class AppConstants {
    public static final String ANDROID_PACKAGE_NAME = "@ANDROID_PACKAGE_NAME@";

    /**
     * Encapsulates access to compile-time version definitions, allowing
     * for dead code removal for particular APKs.
     */
    public static final class Versions {
        public static final int MIN_SDK_VERSION = 15; // TODO
        public static final int MAX_SDK_VERSION = 999; // TODO

        /*
         * The SDK_INT >= N check can only pass if our MAX_SDK_VERSION is
         * _greater than or equal_ to that number, because otherwise we
         * won't be installed on the device.
         *
         * If MIN_SDK_VERSION is greater than or equal to the number, there
         * is no need to do the runtime check.
         */
        public static final boolean feature16Plus = MIN_SDK_VERSION >= 16 || (MAX_SDK_VERSION >= 16 && Build.VERSION.SDK_INT >= 16);
        public static final boolean feature20Plus = MIN_SDK_VERSION >= 20 || (MAX_SDK_VERSION >= 20 && Build.VERSION.SDK_INT >= 20);

    }

    /**
     * The name of the Java class that launches the browser activity.
     */
    public static final String MOZ_ANDROID_BROWSER_INTENT_CLASS = "@MOZ_ANDROID_BROWSER_INTENT_CLASS@";

    public static final String MOZ_APP_DISPLAYNAME = "@MOZ_APP_DISPLAYNAME@";

}

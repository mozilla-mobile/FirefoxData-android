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
    public static final String MANGLED_ANDROID_PACKAGE_NAME = "@MANGLED_ANDROID_PACKAGE_NAME@";

    public static final String MOZ_ANDROID_SHARED_FXACCOUNT_TYPE = "@ANDROID_PACKAGE_NAME@_fxaccount";

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
        public static final boolean feature17Plus = MIN_SDK_VERSION >= 17 || (MAX_SDK_VERSION >= 17 && Build.VERSION.SDK_INT >= 17);
        public static final boolean feature19Plus = MIN_SDK_VERSION >= 19 || (MAX_SDK_VERSION >= 19 && Build.VERSION.SDK_INT >= 19);
        public static final boolean feature20Plus = MIN_SDK_VERSION >= 20 || (MAX_SDK_VERSION >= 20 && Build.VERSION.SDK_INT >= 20);
        public static final boolean feature21Plus = MIN_SDK_VERSION >= 21 || (MAX_SDK_VERSION >= 21 && Build.VERSION.SDK_INT >= 21);
        public static final boolean feature24Plus = MIN_SDK_VERSION >= 24 || (MAX_SDK_VERSION >= 24 && Build.VERSION.SDK_INT >= 24);

        /*
         * If our MIN_SDK_VERSION is 14 or higher, we must be an ICS device.
         * If our MAX_SDK_VERSION is lower than ICS, we must not be an ICS device.
         * Otherwise, we need a range check.
         */
        public static final boolean preMarshmallow = MAX_SDK_VERSION < 23 || (MIN_SDK_VERSION < 23 && Build.VERSION.SDK_INT < 23);
        public static final boolean preLollipop = MAX_SDK_VERSION < 21 || (MIN_SDK_VERSION < 21 && Build.VERSION.SDK_INT < 21);
        public static final boolean preJBMR2 = MAX_SDK_VERSION < 18 || (MIN_SDK_VERSION < 18 && Build.VERSION.SDK_INT < 18);
        public static final boolean preJBMR1 = MAX_SDK_VERSION < 17 || (MIN_SDK_VERSION < 17 && Build.VERSION.SDK_INT < 17);
        public static final boolean preJB = MAX_SDK_VERSION < 16 || (MIN_SDK_VERSION < 16 && Build.VERSION.SDK_INT < 16);
        public static final boolean preN = MAX_SDK_VERSION < 24 || (MIN_SDK_VERSION < 24 && Build.VERSION.SDK_INT < 24);
    }

    /**
     * The name of the Java class that represents the android application.
     */
    public static final String MOZ_ANDROID_APPLICATION_CLASS = "@MOZ_ANDROID_APPLICATION_CLASS@";
    /**
     * The name of the Java class that launches the browser activity.
     */
    public static final String MOZ_ANDROID_BROWSER_INTENT_CLASS = "@MOZ_ANDROID_BROWSER_INTENT_CLASS@";
    /**
     * The name of the Java class that launches the search activity.
     */
    public static final String MOZ_ANDROID_SEARCH_INTENT_CLASS = "@MOZ_ANDROID_SEARCH_INTENT_CLASS@";

    public static final String GRE_MILESTONE = "@GRE_MILESTONE@";

    public static final String MOZ_APP_ABI = "@MOZ_APP_ABI@";
    public static final String MOZ_APP_BASENAME = "@MOZ_APP_BASENAME@";

    // For the benefit of future archaeologists:
    // GRE_BUILDID is exactly the same as MOZ_APP_BUILDID unless you're running
    // on XULRunner, which is never the case on Android.
    public static final String MOZ_APP_BUILDID = "@MOZ_BUILDID@";
    public static final String MOZ_APP_ID = "@MOZ_APP_ID@";
    public static final String MOZ_APP_NAME = "@MOZ_APP_NAME@";
    public static final String MOZ_APP_VENDOR = "@MOZ_APP_VENDOR@";
    public static final String MOZ_APP_VERSION = "@MOZ_APP_VERSION@";
    public static final String MOZ_APP_DISPLAYNAME = "@MOZ_APP_DISPLAYNAME@";
    public static final String MOZ_APP_UA_NAME = "@MOZ_APP_UA_NAME@";

    // MOZILLA_VERSION is already quoted when it gets substituted in. If we
    // add additional quotes we end up with ""x.y"", which is a syntax error.
    public static final String MOZILLA_VERSION = "TODO"; // todo

    public static final String MOZ_MOZILLA_API_KEY = "@MOZ_MOZILLA_API_KEY@";
    public static final String MOZ_CHILD_PROCESS_NAME = "@MOZ_CHILD_PROCESS_NAME@";
    public static final String MOZ_UPDATE_CHANNEL = "@MOZ_UPDATE_CHANNEL@";
    public static final String OMNIJAR_NAME = "@OMNIJAR_NAME@";
    public static final String OS_TARGET = "@OS_TARGET@";
    public static final String TARGET_XPCOM_ABI = "@TARGET_XPCOM_ABI@";

    public static final String USER_AGENT_BOT_LIKE = "Redirector/" + AppConstants.MOZ_APP_VERSION +
        " (Android; rv:" + AppConstants.MOZ_APP_VERSION + ")";

    public static final String USER_AGENT_FENNEC_MOBILE = "Mozilla/5.0 (Android " +
        Build.VERSION.RELEASE + "; Mobile; rv:" +
        AppConstants.MOZ_APP_VERSION + ") Gecko/" +
        AppConstants.MOZ_APP_VERSION + " Firefox/" +
        AppConstants.MOZ_APP_VERSION;

    public static final String USER_AGENT_FENNEC_TABLET = "Mozilla/5.0 (Android " +
        Build.VERSION.RELEASE + "; Tablet; rv:" +
        AppConstants.MOZ_APP_VERSION + ") Gecko/" +
        AppConstants.MOZ_APP_VERSION + " Firefox/" +
        AppConstants.MOZ_APP_VERSION;

    public static final boolean NIGHTLY_BUILD = true; // TODO:

    public static final boolean DEBUG_BUILD = true; // TODO

    // Official corresponds, roughly, to whether this build is performed on
    // Mozilla's continuous integration infrastructure. You should disable
    // developer-only functionality when this flag is set.
    public static final boolean MOZILLA_OFFICIAL = false; // TODO
}

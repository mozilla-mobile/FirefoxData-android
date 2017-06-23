# FirefoxData for Android
[![BuddyBuild](https://dashboard.buddybuild.com/api/statusImage?appID=594d49dc1b97a400017fceb0&branch=master&build=latest)](https://dashboard.buddybuild.com/apps/594d49dc1b97a400017fceb0/build/latest?branch=master)
[![Download](https://api.bintray.com/packages/mcomella/FirefoxData/download/images/download.svg)](https://bintray.com/mcomella/FirefoxData/download/_latestVersion)

FirefoxData is an Android library that allows an application to easily access a
selection of the user's [Firefox Account][fxa] data:
* History
* Bookmarks
* Passwords

This library is also available on iOS (TODO: link).

## Installation
Be sure `jcenter` is present in your gradle repositories and add the following
to your module's `build.gradle`:
```
compile 'org.mozilla.fxa-data:thirdparty:0.0.1'
compile 'org.mozilla.fxa-data:gecko:0.0.1'
compile 'org.mozilla.fxa-data:download:0.0.1@aar'
```

## Quick start
Below is the simplest implementation, based on [SimpleExampleActivity][], which
is one of the examples in [the examples/ module][example].

```java
public class MainActivity extends AppCompatActivity {
    private FirefoxDataLoginManager loginManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // update for your implementation.

        // Store a reference to the login manager,
        // which is used to get a FirefoxDataClient.
        loginManager = FirefoxData.getLoginManager(this);

        final FirefoxDataLoginManager.LoginCallback callback = new ExampleLoginCallback();
        if (!loginManager.isSignedIn()) {
            loginManager.promptLogin(this, "Your app name", callback);
        } else {
            loginManager.loadStoredAccount(callback);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Required callback.
        loginManager.onActivityResult(requestCode, resultCode, data);
    }

    // Make sure this is static so we don't keep a reference
    // to Context, which can cause memory leaks.
    private static class ExampleLoginCallback implements FirefoxDataLoginManager.LoginCallback {
        @Override
        public void onSuccess(final FirefoxDataClient dataClient) {
            try {
                final List<HistoryRecord> history = dataClient.getAllHistory().getResult();
                for (final HistoryRecord record : history) {
                    Log.d("FxData", record.getTitle() + ": " + record.getURI());
                }
            } catch (final FirefoxDataException e) {
                Log.e("FxData", "failed to get data", e);
            }
        }

        @Override
        public void onFailure(final FirefoxDataException e) {
            Log.e("FxData", "Failed to get DataClient", e);
        }

        @Override
        public void onUserCancel() {
            Log.d("FxData", "User cancelled log-in attempt.");
        }
    }
}
```

### Explanation
**Notes on exposed APIs**:
* Classes exposed in the `org.mozilla.fxa_data.impl` package are not intended
for public consumption and are subject to change.
* Classes from the `thirdparty` and `gecko` modules are exposed due to [a known
issue][i-deps] and should not be considered public. All public APIs can be
found in the `org.mozilla.fxa_data` package, with the exception of classes in
the `impl` package which is private.

---

The [`FirefoxData`][ffData] class is the entry point to the library. It can
return a [`FirefoxDataLoginManager`][ffLm], which we recommend storing a
reference to in `onCreate`:
```java
    private FirefoxDataLoginManager loginManager;

    protected void onCreate(final Bundle savedInstanceState) {
        ...
        loginManager = FirefoxData.getLoginManager(this);
```

For successful logins, the following must also be called:
```java
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        ...
        loginManager.onActivityResult(requestCode, resultCode, data);
```

The `FirefoxDataLoginManager` allows you to sign into an account, access a
previously signed in account, or sign out of the signed in account. To log in,
e.g. when the user clicks a sign in button, you might call:
```java
    public void onClick(View v) {
        loginManager.promptLogin(MainActivity.this, "Your app name", callback);
    }
```

Firefox Accounts have a dashboard of all synced devices - your app name name is
displayed there so the user can identify which device is which.

The [`FirefoxDataLoginManager.LoginCallback`][ffLc] argument requires the
following methods:
* `onSuccess(FirefoxDataClient)`: called when the login was completed
successfully. The account will be saved for future use:
`FirefoxDataLoginManager.loadStoredAccount` can be used instead of
`promptLogin` so the user doesn't have to log in again and
`FirefoxDataLoginManager.isSignedIn` should now return `true`.
* `onFailure(FirefoxDataException)`: login (or loading a stored account) has
failed: it's probably best to try again later.
* `onUserCancel()`: called when the user cancelled a login attempt (it should
never be called for `loadStoredAccount`).

Notes:
* These callbacks are called from a background thread which is okay to block
* LoginCallback may leak memory if it stores a reference to the Context so
generally avoid using anonymous classes with it!

The [`FirefoxDataClient`][ffDc] given to `onSuccess` can be used to access the
user's data. It includes the following blocking methods (which are okay to call
from the callback):
* `getAllBookmarks()`
* `getBookmarksWithLimit(int)`
* `getAllHistory()`
* `getHistoryWithLimit(int)`
* `getAllPasswords()`
* `getPasswordsWithLimit(int)`

The `*WithLimit` methods can be used to return a subset of a user's data. This
can be useful if the user has a lot of data or you're making test queries.

Each method returns a [`DataCollectionResult<T>`][ffDcr], which wraps the
returned value (to allow for future API expansion). Call
`DataCollectionResult.getResult()` to return one of:
* [`BookmarkFolder`][ffBf]
* [`List<HistoryRecord>`][ffHr]
* [`List<PasswordRecord>`][ffPr]

History & passwords are largely self-explanatory but for bookmarks, we return
the root bookmark folder of a tree-like structure. You can access the actual
bookmarks with the following methods:
* [`List<BookmarkRecord> getBookmarks()`][ffBr]
* `BookmarkFolder getSubfolders()`

You should now have the data you need!

## Known issues
* [We require importing multiple dependencies, which exposes their APIs. We want
this to change to one import and no extra exposed APIs.][i-deps]
* We include some large dependencies: [appcompat][i-appcompat] &
[httpclientlib][i-httpclientlib]
* [We only support English at the moment][i-l10n]
* [We haven't investigated ProGuard configurations yet][i-proguard].
* [We'd like to host javadocs][i-javadoc]

## Questions? Feedback?
We'd love to hear from you!

If you have questions or feedback about using this library, you can catch us on
[Mozilla's IRC](https://wiki.mozilla.org/IRC) in the #mobile channel.

If you've found a bug, we'd love it if you [file an
issue](https://github.com/mozilla-mobile/FirefoxData-android/issues).

## Contributing to the repository

### Setting up a build
After cloning, in Android Studio, select "File -> Open". Find your
`./FirefoxAccounts-android` directory and select the `build.gradle` file in
that root directory. Running the default configuration should run the
`FirefoxDataInRecyclerViewExampleActivity`.

You may also want to add a run configuration for `SimpleExampleActivity`.

### Publishing to bintray
To publish, ensure you have a bintray account with the appropriate permissions,
add the following to a `./local.properties` file:
```
bintray.user=<username>
bintray.apikey=<api-key>
```

Increment the version number in `./gradle.properties` & run the following to
upload:
```
./publish.sh
```

### Coding notes
The code is laid out into a few modules:
* `download`: core module, includes the public API.
* `gecko/`: dependencies imported from fennec, largerly sync code
* `thirdparty/`: third-party in-tree dependencies
* `example/`: examples using the library


[SimpleExampleActivity]: https://github.com/mozilla-mobile/FirefoxData-android/blob/master/example/src/main/java/org/mozilla/fxa_data/example/SimpleExampleActivity.java
[example]: https://github.com/mozilla-mobile/FirefoxData-android/tree/master/example/src/main/java/org/mozilla/fxa_data/example
[fxa]: https://developer.mozilla.org/en-US/docs/Mozilla/Tech/Firefox_Accounts

[i-deps]: https://github.com/mozilla-mobile/FirefoxData-android/issues/12
[i-httpclientlib]: https://github.com/mozilla-mobile/FirefoxData-android/issues/4
[i-appcompat]: https://github.com/mozilla-mobile/FirefoxData-android/issues/13
[i-l10n]: https://github.com/mozilla-mobile/FirefoxData-android/issues/17
[i-proguard]: https://github.com/mozilla-mobile/FirefoxData-android/issues/16
[i-javadoc]: https://github.com/mozilla-mobile/FirefoxData-android/issues/18

[ffData]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/FirefoxData.java#L15
[ffLm]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/login/FirefoxDataLoginManager.java#L16
[ffLc]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/login/FirefoxDataLoginManager.java#L94
[ffDc]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/download/FirefoxDataClient.java#L15
[ffDcr]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/download/DataCollectionResult.java#L9

[ffBf]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/download/BookmarkFolder.java#L12
[ffBr]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/download/BookmarkRecord.java#L18
[ffHr]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/download/HistoryRecord.java#L9
[ffPr]: https://github.com/mozilla-mobile/FirefoxData-android/blob/f245ea97eb34373b39a8ade44103dd98f3c8b27a/fxa_data/src/main/java/org/mozilla/fxa_data/download/PasswordRecord.java#L9

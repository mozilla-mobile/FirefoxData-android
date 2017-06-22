# FirefoxData for Android
FirefoxData is an Android library that allows an application to easily access a
selection of the user's [Firefox Account][fxa] data:
* History
* Bookmarks
* Passwords

This library is also available on iOS (TODO: link).

## Installation
Add the following to gradle:

TODO: blocked on upload to jcenter & friends.

## Quick start
todo: API naming & update links (example).
todo: get javadoc. link to it.
todo: explain impl.
todo: explain code layout (modules).

Below is the simplest implementation, based on [SimpleExampleActivity][], which
is one of the examples in [the examples/ module][example].

```java
public class MainActivity extends AppCompatActivity {
    private FirefoxSyncLoginManager loginManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // update for your implementation.

        // Store a reference to the login manager,
        // which is used to get a FirefoxSyncClient.
        loginManager = FirefoxSync.getLoginManager(this);

        final FirefoxSyncLoginManager.LoginCallback callback = new ExampleLoginCallback(this);
        if (!loginManager.isSignedIn()) {
            loginManager.promptLogin(this, "Your app name", callback);
        } else {
            loginManager.loadStoredSyncAccount(callback);
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
    private static class ExampleLoginCallback implements FirefoxSyncLoginManager.LoginCallback {
        @Override
        public void onSuccess(final FirefoxSyncClient dataClient) {
            try {
                final List<HistoryRecord> history = dataClient.getAllHistory().getResult();
                for (final HistoryRecord record : history) {
                    Log.d("FxData", record.getTitle() + ": " + record.getURI());
                }
            } catch (final FirefoxSyncException e) {
                Log.e("FxData", "failed to get data", e);
            }
        }

        @Override
        public void onFailure(final FirefoxSyncException e) {
            Log.e("FxData", "Failed to get SyncClient", e);
        }

        @Override
        public void onUserCancel() {
            Log.d("FxData", "User cancelled log-in attempt.");
        }
    }
}
```

### Details
**Notes on exposed APIs**:
* Classes exposed in the `org.mozilla.fxa_data.impl` package are not intended
for public consumption and are subject to change.
* Classes from the `thirdparty` and `gecko` modules are exposed due to a known
issue (see Known Issues below) - these are also subject to change. Public APIs
can be found in the `org.mozilla.fxa_data` package, with the exception of
`impl`.

todo: this section.


The `FirefoxSync` class is the entry point to the library. Call:
```java
FirefoxSync.getLoginManager(context);
```

to get a reference to a `FirefoxSyncLoginManager`, which can be used in the
following ways:
user to log into an account or to access an account the user is already logged
into. It has the following methods:
* `promptLogin(activity, callerName, loginCallback)`: prompt the user to log
into an account
* `loadStoredSyncAccount(loginCallback)`: access an account the user

`FirefoxSyncLoginManager`

## Known issues
* [We require importing multiple dependencies, which exposes their APIs. We want
this to change to one import and no extra exposed APIs.][i-deps]
* We include some large dependencies: [appcompat][i-appcompat] &
[httpclientlib][i-httpclientlib]
* [We only support English at the moment][i-l10n]
* [We haven't investigated ProGuard configurations yet][i-proguard].

## Contributing to the repository

### Setting up a build
todo: explain how to load examples into IDE (run config!)/develop library.

### Publishing to bintray
To publish, ensure you have a bintray account with the appropriate permissions,
add the following to a ./local.properties file:

```
bintray.user=<username>
bintray.apikey=<api-key>
```

Increment the version number in gradle.properties & run the following to
upload:

```
./publish.sh
```

[SimpleExampleActivity]: https://github.com/mcomella/FirefoxAccounts-android/blob/master/example/src/main/java/org/mozilla/sync/example/SimpleExampleActivity.java
[example]: https://github.com/mcomella/FirefoxAccounts-android/tree/master/example/src/main/java/org/mozilla/sync/example
[fxa]: https://developer.mozilla.org/en-US/docs/Mozilla/Tech/Firefox_Accounts

[i-deps]: https://github.com/mozilla-mobile/FirefoxData-android/issues/12
[i-httpclientlib]: https://github.com/mozilla-mobile/FirefoxData-android/issues/4
[i-appcompat]: https://github.com/mozilla-mobile/FirefoxData-android/issues/13
[i-l10n]: https://github.com/mozilla-mobile/FirefoxData-android/issues/17
[i-proguard]: https://github.com/mozilla-mobile/FirefoxData-android/issues/16

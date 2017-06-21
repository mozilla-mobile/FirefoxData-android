# FirefoxData for Android
FirefoxData is a library that allows an application to easily access a
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
todo: explain how to load examples into IDE (run config!)/develop library.
todo: explain impl.

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
        public void onSuccess(final FirefoxSyncClient syncClient) {
            try {
                final List<HistoryRecord> history = syncClient.getAllHistory().getResult();
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
* Visibility of deps.
* import of multiple deps
* big size (some initiatives)
* l10n
* ProGuard

## Contributing to the repository
TODO: publish details.

[SimpleExampleActivity]: https://github.com/mcomella/FirefoxAccounts-android/blob/master/example/src/main/java/org/mozilla/sync/example/SimpleExampleActivity.java
[example]: https://github.com/mcomella/FirefoxAccounts-android/tree/master/example/src/main/java/org/mozilla/sync/example
[fxa]: https://developer.mozilla.org/en-US/docs/Mozilla/Tech/Firefox_Accounts

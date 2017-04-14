package org.mozilla.accountsexample;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import org.mozilla.accounts.login.FirefoxAccountLoginWebViewActivity;

public class AccountsExampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Intent intent = new Intent(this, FirefoxAccountLoginWebViewActivity.class);
        startActivityForResult(intent, 10);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Log.d("lol", data.getStringExtra("lol"));
        } else {
            Log.d("lol", "uh oh");
        }
    }
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fxa_data.example;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.mozilla.fxa_data.FirefoxSync;
import org.mozilla.fxa_data.FirefoxSyncException;
import org.mozilla.fxa_data.login.FirefoxSyncLoginManager;
import org.mozilla.fxa_data.sync.FirefoxSyncClient;
import org.mozilla.fxa_data.sync.HistoryRecord;
import org.mozilla.fxa_data.sync.SyncCollectionResult;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An example Activity that which displays a user's history in a {@link RecyclerView} after they log in.
 * If they're already logged in, the data will just be shown. Once the data is shown, the user can click
 * "Sign out" to sign out.
 *
 * This Activity will switch between four states:
 * - Prompt for sign in
 * - Loading
 * - Error logging in or fetching Firefox data
 * - Display user's history.
 *
 * This is a fairly realistic use case. For a simpler example, see {@link SimpleExampleActivity}.
 */
public class FirefoxDataInRecyclerViewExampleActivity extends AppCompatActivity {

    private enum UIState {
        SIGN_IN_PROMPT, LOADING, ERROR, SHOW_DATA
    }

    private FirefoxSyncLoginManager loginManager;
    private FirefoxHistoryAdapter historyAdapter;

    private boolean isWaitingForCallback = false;

    private List<View> containerViews;

    private View fxDataContainer;
    private RecyclerView fxDataView;
    private Button fxDataSignOutButton;

    private View loadingContainer;

    // For sign in & error states.
    private View explanationContainer;
    private TextView explanationView;
    private Button explanationButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firefox_data_in_recycler_view_example);
        getSupportActionBar().setTitle(FirefoxDataInRecyclerViewExampleActivity.class.getSimpleName());
        findViewsById();
        initFirefoxDataView();

        // Keep a reference to the login manager - it's the entry point for Firefox data.
        loginManager = FirefoxSync.getLoginManager(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Required callback.
        loginManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isWaitingForCallback) {
            updateUI(UIState.LOADING);
        } else if (!loginManager.isSignedIn()) {
            updateUI(UIState.SIGN_IN_PROMPT);
        } else {
            fetchFirefoxDataAndUpdateUI();
        }
    }

    private void fetchFirefoxDataAndUpdateUI() {
        isWaitingForCallback = true;
        updateUI(UIState.LOADING);
        loginManager.loadStoredSyncAccount(new LoginCallback(this));
    }

    // static so we don't keep a reference to Context, which could cause memory leaks.
    private static class LoginCallback implements FirefoxSyncLoginManager.LoginCallback {
        private final WeakReference<FirefoxDataInRecyclerViewExampleActivity> activityWeakReference;

        private LoginCallback(final FirefoxDataInRecyclerViewExampleActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(final FirefoxSyncClient syncClient) {
            final FirefoxDataInRecyclerViewExampleActivity activity = activityWeakReference.get();
            if (activity == null) { return; }

            activity.isWaitingForCallback = false;
            final SyncCollectionResult<List<HistoryRecord>> result;
            try {
                result = syncClient.getAllHistory();
            } catch (final FirefoxSyncException e) {
                activity.updateUI(UIState.ERROR, e);
                return;
            }

            activity.historyAdapter.setHistoryRecords(result.getResult());
            activity.updateUI(UIState.SHOW_DATA);
        }

        @Override
        public void onFailure(final FirefoxSyncException e) {
            final FirefoxDataInRecyclerViewExampleActivity activity = activityWeakReference.get();
            if (activity == null) { return; }

            activity.updateUI(UIState.ERROR, e);
        }

        @Override
        public void onUserCancel() {
            final FirefoxDataInRecyclerViewExampleActivity activity = activityWeakReference.get();
            if (activity == null) { return; }

            activity.isWaitingForCallback = false;
            activity.updateUI(UIState.SIGN_IN_PROMPT);
        }
    }

    private class SignInOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            loginManager.promptLogin(FirefoxDataInRecyclerViewExampleActivity.this,
                    getResources().getString(R.string.app_name),
                    new LoginCallback(FirefoxDataInRecyclerViewExampleActivity.this));
            isWaitingForCallback = true;
            updateUI(UIState.LOADING);
        }
    }

    private class SignOutOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            loginManager.signOut();
            updateUI(UIState.SIGN_IN_PROMPT);
        }
    }

    private void updateUI(final UIState uiState) {
        updateUI(uiState, null);
    }

    private void updateUI(final UIState uiState, @Nullable final Exception exception) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Reset UI state by removing all mutations we make.
                for (final View container : containerViews) {
                    container.setVisibility(View.GONE); // we'll unhide one of these.
                }

                explanationButton.setEnabled(true);

                // Mutate to the new state for the given arguments.
                if (uiState == UIState.SHOW_DATA) {
                    // Adapter takes care of data updates.
                    fxDataContainer.setVisibility(View.VISIBLE);
                } else if (uiState == UIState.SIGN_IN_PROMPT || uiState == UIState.ERROR) {
                    explanationContainer.setVisibility(View.VISIBLE);
                } else {
                    loadingContainer.setVisibility(View.VISIBLE);
                }

                switch (uiState) {
                    case SIGN_IN_PROMPT:
                        setExplanationUIResources(R.string.sign_in_explanation, R.string.sign_in_button, new SignInOnClickListener());
                        break;

                    case ERROR:
                        final Resources res = getResources();
                        setExplanationUIResources(res.getString(R.string.error_explanation, exception != null ? exception.toString() : "Unknown"),
                                res.getString(R.string.error_button),
                                new SignOutOnClickListener());
                        break;
                }

            }
        });
    }

    private void setExplanationUIResources(@StringRes final int explanationRes, @StringRes final int buttonTextRes,
            final View.OnClickListener onButtonClick) {
        final Resources res = getResources();
        setExplanationUIResources(res.getString(explanationRes), res.getString(buttonTextRes), onButtonClick);
    }

    private void setExplanationUIResources(final String explanation, final String buttonText, final View.OnClickListener onButtonClick) {
        explanationView.setText(explanation);
        explanationButton.setText(buttonText);
        explanationButton.setOnClickListener(onButtonClick);
    }

    private void initFirefoxDataView() {
        final int orientation = RecyclerView.VERTICAL;
        fxDataView.setLayoutManager(new LinearLayoutManager(this, orientation, false));
        fxDataView.addItemDecoration(new DividerItemDecoration(this, orientation));

        historyAdapter = new FirefoxHistoryAdapter();
        fxDataView.setAdapter(historyAdapter);

        fxDataSignOutButton.setOnClickListener(new SignOutOnClickListener());
    }

    private void findViewsById() {
        explanationContainer = findViewById(R.id.container_explanation);
        explanationView = (TextView) findViewById(R.id.explanation_text);
        explanationButton = (Button) findViewById(R.id.explanation_button);

        loadingContainer = findViewById(R.id.container_loading);

        fxDataContainer = findViewById(R.id.container_fx_data);
        fxDataView = (RecyclerView) findViewById(R.id.fx_data_list);
        fxDataSignOutButton = (Button) findViewById(R.id.fx_data_sign_out_button);

        containerViews = Collections.unmodifiableList(Arrays.asList(
                explanationContainer,
                loadingContainer,
                fxDataContainer
        ));
    }
}

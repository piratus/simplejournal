package net.piratus.simplejournal2.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import net.piratus.simplejournal2.R;
import net.piratus.simplejournal2.livejournal.LJMethod;
import net.piratus.simplejournal2.livejournal.LJResponseHandler;

import java.util.HashMap;

public class AuthenticationActivity extends AccountAuthenticatorActivity {
    private EditText mUsernameEdit = null;
    private EditText mPasswordEdit = null;

    private String username = null;
    private String password = null;

    private AccountManager accountManager = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType("net.piratus.simplejournal2");
        if (accounts.length > 0) {
            username = accounts[0].name;
            password = accountManager.getPassword(accounts[0]);

            returnResult();
        } else {
            setContentView(R.layout.login_activity);

            mUsernameEdit = (EditText) findViewById(R.id.username_edit);
            mPasswordEdit = (EditText) findViewById(R.id.password_edit);
        }
    }

    public void doLogin(View view) {
        username = mUsernameEdit.getText().toString();
        password = mPasswordEdit.getText().toString();

        final HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("username", username);
        data.put("password", password);
        data.put("ver", 1);

        new LJMethod("login", data, loginHandler).start();
    }

    private LJResponseHandler loginHandler = new LJResponseHandler(this) {
        @Override
        protected void onSuccess(HashMap<String, Object> data) {
            final AccountManager manager = AccountManager.get(AuthenticationActivity.this);
            final Account account = new Account(username, "net.piratus.simplejournal");

            manager.addAccountExplicitly(account, password, null);

            returnResult();
        }
    };

    private void returnResult() {
        final Intent resultIntent = new Intent();
        resultIntent.putExtra("username", username);
        resultIntent.putExtra("password", password);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
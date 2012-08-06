package net.piratus.simplejournal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import com.petebevin.markdown.MarkdownProcessor;
import net.piratus.simplejournal.auth.AuthenticationActivity;
import net.piratus.simplejournal.livejournal.LJMethod;
import net.piratus.simplejournal.livejournal.LJResponseHandler;
import net.piratus.simplejournal.livejournal.LJUtil;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * User: piratus
 * Date: Apr 17, 2010
 */
public class SimpleJournalActivity extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "SimpleJournalActivity";

    private static final int SEND_DIALOG = 1;
    private static final int HELP_DIALOG = 2;
    private static final int LOADING_DIALOG = 3;
    private static final int NO_CREDENTIALS_DIALOG = 4;
    private static final int CLEAR_DIALOG = 5;

    private static final int ERROR_NO_CREDENTIALS = 1;

    private boolean isEditing = false;
    private int itemID = -1;
    private String editingEventTime = null;

    private String username = null;
    private String password = null;

    private EditText subject;
    private EditText body;

    private Intent newEntryIntent = null; // not-null when there is a new text to post

    private boolean entryIsEmpty() {
        String subj = subject.getText().toString();
        String txt = body.getText().toString();
        return ((subj == null || subj.isEmpty())
                && (txt == null || txt.isEmpty()));
    }

    private void loadEntryFromIntent() {
        if (newEntryIntent != null && entryIsEmpty()) {
            Intent intent = newEntryIntent;
            String subj = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String txt = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (subj != null) {
                subject.getText().append(subj);
            }
            if (txt != null) {
                body.getText().append(txt);
            }
            newEntryIntent = null;
        }
    }

    /**
     * Interface stuff
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        subject = (EditText) findViewById(R.id.subject);
        body = (EditText) findViewById(R.id.post);

        startActivityForResult(new Intent(this, AuthenticationActivity.class), -1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        loadDraft();

        username = data.getStringExtra("username");
        password = data.getStringExtra("password");

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            newEntryIntent = intent;
            if (entryIsEmpty()) {
                loadEntryFromIntent();
            } else {
                showDialog(CLEAR_DIALOG); // it calls loadEntryFromIntent() on OK
            }
        }
    }

    @Override
    protected void onPause() {
        saveDraft();

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_send:
                saveDraft();
                doSendPost();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_edit:
                saveDraft();
                doEditPost();
                return true;
            case R.id.menu_help:
                showDialog(HELP_DIALOG);
                return true;
            case R.id.menu_clear:
                showDialog(CLEAR_DIALOG);
                return true;
        }

        return false;
    }

    private void doSendPost() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (username.equals("") || password.equals("")) {
            showDialog(NO_CREDENTIALS_DIALOG);
            return;
        }
        showDialog(SEND_DIALOG);

        final HashMap<String, Object> data = new HashMap<String, Object>();

        data.put("username", username);
        data.put("password", password);
        data.put("ver", 1);
        data.put("lineendings", "unix");

        data.put("subject", subject.getText().toString());

        String post = Typography.parse(body.getText().toString());
        final boolean preformatted = prefs.getBoolean("markdown", false);
        if (preformatted) {
            post = new MarkdownProcessor().markdown(post);
        }
        data.put("event", post);

        final String security = prefs.getString("security", "public");
        data.put("security", security);
        if (security.equals("usemask")) {
            data.put("allowmask", 1);
        }

        final HashMap<String, Object> options = new HashMap<String, Object>();
        options.put("opt_preformatted", preformatted);
        data.put("props", options);

        final LJMethod postevent;
        if (!isEditing) {
            data.put("clientversion", "SimpleJournal/0.4");
            data.putAll(LJUtil.getDate(new GregorianCalendar()));
            postevent = new LJMethod("postevent", data, handler);
        } else {
            data.put("itemid", itemID);
            data.putAll(LJUtil.getDateFromLJString(editingEventTime));
            postevent = new LJMethod("editevent", data, handler);
        }

        postevent.start();
    }

    private void doEditPost() {
        if (subject.getText().toString().length() > 0 || body.getText().toString().length() > 0) {
            new AlertDialog.Builder(SimpleJournalActivity.this)
                    .setMessage(R.string.text_entered)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok_doit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                            new EntryLoader().execute();
                        }
                    })
                    .setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                        }
                    }).show();
        } else {
            new EntryLoader().execute();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case LOADING_DIALOG:
                final ProgressDialog loadingDialog = new ProgressDialog(this);
                loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                loadingDialog.setMessage(getString(R.string.loading));
                loadingDialog.setCancelable(false);
                return loadingDialog;

            case SEND_DIALOG:
                final ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage(getResources().getString(R.string.sending));
                return progressDialog;

            case HELP_DIALOG:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.help_title)
                        .setMessage(R.string.markdown_help)
                        .create();

            case NO_CREDENTIALS_DIALOG:
                return new AlertDialog.Builder(SimpleJournalActivity.this)
                    .setMessage(R.string.user_data_not_set)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok_doit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                            startActivity(new Intent(SimpleJournalActivity.this, SettingsActivity.class));
                        }
                    })
                    .setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                        }
                    }).create();

            case CLEAR_DIALOG:
                return new AlertDialog.Builder(SimpleJournalActivity.this)
                        .setMessage(R.string.clear_entry)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok_doit, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                clearEntry();
                                loadEntryFromIntent();
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.cancel();
                            }
                        }).create();
        }

        return null;
    }

    /**
     * Responder for entry sending
     */
    private final LJResponseHandler handler = new LJResponseHandler(this) {
        @Override
        protected void onSuccess(HashMap<String, Object> data) {
            final String entryLink = (String) data.get("url");
            clearEntry();

            new AlertDialog.Builder(SimpleJournalActivity.this)
                    .setCancelable(false)
                    .setTitle(R.string.yay)
                    .setMessage(R.string.entry_posted)
                    .setPositiveButton(R.string.thanks, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                        }
                    })
                    .setNeutralButton(R.string.open_in_browser, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            startActivity(new Intent("android.intent.action.VIEW",
                                            Uri.parse(entryLink)));
                            dialog.cancel();
                        }
                    })
                    .show();

            dismissDialog(SEND_DIALOG);
        }

        @Override
        protected void onFail(String message) {
            dismissDialog(SEND_DIALOG);
            super.onFail(message);
        }
    };


    /**
     * Entry editing activity
     */
    private class EntryLoader extends AsyncTask<Void, Void, HashMap> {
        @Override
        protected void onPreExecute() {
            showDialog(LOADING_DIALOG);
        }

        @Override
        protected HashMap<String, Object> doInBackground(Void... objects) {
            HashMap<String, Object> result = new HashMap<String, Object>();

            if (username.equals("") || password.equals("")) {
                result.put("error_code", ERROR_NO_CREDENTIALS);
                return result;
            }

            final HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("username", username);
            data.put("password", password);
            data.put("ver", 1);
            data.put("lineendings", "unix");
            data.put("selecttype", "lastn");
            data.put("howmany", 1);

            final XMLRPCClient client = new XMLRPCClient("http://www.livejournal.com/interface/xmlrpc");

            try {
                result = (HashMap<String, Object>) client.call("LJ.XMLRPC.getevents", data);
            } catch (XMLRPCException e) {
                result = new HashMap<String, Object>();
                result.put("error", e.getMessage());
            }

            return result;
        }

        @Override
        protected void onPostExecute(HashMap result) {
            dismissDialog(LOADING_DIALOG);

            String error = null;

            if (!result.containsKey("events")) {
                error = getResources().getString(R.string.entry_load_failed);
            }

            if (result.containsKey("error_code") && result.get("error_code").equals(ERROR_NO_CREDENTIALS)) {
                showDialog(NO_CREDENTIALS_DIALOG);
                return;
            } else if (result.containsKey("error")) {
                error = (String) result.get("error");
            }

            if (error != null) {
                new AlertDialog.Builder(SimpleJournalActivity.this)
                        .setCancelable(false)
                        .setTitle(R.string.something_wrong)
                        .setMessage(R.string.failed_to_load)
                        .setPositiveButton(R.string.pitty, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.cancel();
                            }
                        })
                        .show();
                return;
            }

            final HashMap<String, Object> event = (HashMap<String, Object>) ((Object[])result.get("events"))[0];

            isEditing = true;
            itemID = (Integer) event.get("itemid");
            editingEventTime = (String) event.get("eventtime");
            subject.setText(extractString(event.get("subject")));
            body.setText(extractString(event.get("event")));
        }
    }

    /**
     * Utility methods
     */
    private void clearEntry() {
        itemID = -1;
        subject.getText().clear();
        body.getText().clear();
        isEditing = false;
        editingEventTime = null;
    }

    private void saveDraft() {
        final SharedPreferences.Editor prefs =
                PreferenceManager.getDefaultSharedPreferences(SimpleJournalActivity.this).edit();

        prefs.putString(Constants.DRAFT_SUBJECT, subject.getText().toString());
        prefs.putString(Constants.DRAFT_BODY, body.getText().toString());
        prefs.putBoolean(Constants.DRAFT_EDITING, isEditing);
        prefs.putString(Constants.DRAFT_EDITING_EVENT_TIME, editingEventTime);
        prefs.putInt(Constants.DRAFT_ITEMID, itemID);

        prefs.commit();
    }

    private void loadDraft() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SimpleJournalActivity.this);

        clearEntry();

        subject.getText().append(prefs.getString(Constants.DRAFT_SUBJECT, ""));
        body.getText().append(prefs.getString(Constants.DRAFT_BODY, ""));

        isEditing = prefs.getBoolean(Constants.DRAFT_EDITING, false);
        editingEventTime = prefs.getString(Constants.DRAFT_EDITING_EVENT_TIME, "");
        itemID = prefs.getInt(Constants.DRAFT_ITEMID, -1);

    }

    private static String extractString(Object data) {
        if (data instanceof byte[]) {
            return new String((byte[]) data);
        }

        return (String) data;
    }
}

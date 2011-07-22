package net.piratus.simplejournal;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * User: piratus
 * Date: May 4, 2010
 */
public class EditEntryActivity extends ListActivity {
    public static final String TAG = "EditEntryActivity";

    private static final int LOADING_DIALOG = 1;

    protected ListView list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list = getListView();
        new EntriesLoader().execute();
//        showDialog(LOADING_DIALOG);
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case LOADING_DIALOG:
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Loading latest entries");
                progressDialog.setCancelable(false);
                return progressDialog;
        }

        return null;
    }


    private class EntriesLoader extends AsyncTask<Void, Void, HashMap> {
        @Override
        protected void onPreExecute() {
            showDialog(LOADING_DIALOG);
        }

        @Override
        protected HashMap<String, Object> doInBackground(Void... objects) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(EditEntryActivity.this);

            final HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("username", prefs.getString("username", ""));
            data.put("password", prefs.getString("password", ""));
            data.put("ver", 1);
            data.put("lineendings", "unix");
            data.put("noprops", 1);
            data.put("selecttype", "lastn");
            data.put("howmany", 10);

            final XMLRPCClient client = new XMLRPCClient("http://www.livejournal.com/interface/xmlrpc");
            HashMap<String, Object> result;
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

            if (result == null || !result.containsKey("events")) {
                error = "No entries loaded";
            } else if (result.containsKey("error")) {
                error = (String) result.get("error");
            }

            if (error != null) {
                new AlertDialog.Builder(EditEntryActivity.this)
                        .setCancelable(false)
                        .setTitle("Something's wrong")
                        .setMessage("No entries loaded")
                        .setPositiveButton("That's a pitty", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.cancel();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialogInterface) {
                                EditEntryActivity.this.finish();
                            }
                        })
                        .show();
                return;
            }

            final ArrayList<HashMap<String, String>> listEntries = new ArrayList<HashMap<String, String>>();
            final Object[] events = (Object[]) result.get("events");

            for (Object event: events) {
                final HashMap<String, String> item = new HashMap<String, String>();
                final HashMap<String, Object> hMap = (HashMap<String, Object>) event;

                item.put("subject", extractString(hMap.get("subject")));
                item.put("event", extractString(hMap.get("event")));

                listEntries.add(item);
            }

            setListAdapter(new SimpleAdapter(EditEntryActivity.this,
                    listEntries, R.layout.one_entry,
                    new String[] {"subject", "event"},
                    new int[] {R.id.entrySubject, R.id.entryText}));
        }
    }

    private static String extractString(Object data) {
        final String result;
        if (data instanceof byte[]) {
            result = new String((byte[]) data);
        } else if (data instanceof String) {
            result = (String) data;
        } else {
            result = "";
        }

        return result;
    }
}

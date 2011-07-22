package net.piratus.simplejournal.livejournal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import net.piratus.simplejournal.R;

import java.util.HashMap;

/**
 * User: piratus
 * Date: May 6, 2010
 */
public abstract class LJResponseHandler extends Handler {
    protected final Context context;

    public LJResponseHandler(Context context) {
        this.context = context;
    }

    protected abstract void onSuccess(HashMap<String,Object> data);

    protected void onFail(String message) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(R.string.pitty, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(Message msg) {
        Bundle data = msg.getData();
        if (data.getBoolean("success", false)) {
            onSuccess((HashMap<String, Object>) data.getSerializable("result"));
        } else {
            onFail(data.getString("error"));
        }
    }
}

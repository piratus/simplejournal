package net.piratus.simplejournal.livejournal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCFault;

import java.util.HashMap;
import java.util.Map;

/**
 * User: piratus
 * Date: May 6, 2010
 */
public class LJMethod extends Thread {
    private static final String TAG = "LJMethod";

    private final Handler handler;
    private final XMLRPCClient client;

    private final String name;
    private final Map<String, Object> params;

    public LJMethod(String name, Map<String, Object> params, LJResponseHandler handler) {
        client = new XMLRPCClient("http://www.livejournal.com/interface/xmlrpc");

        this.handler = handler;
        this.name = "LJ.XMLRPC." + name;
        this.params = params;

        Log.d(TAG, params.toString());
    }

    public void run() {
        final Bundle bundle = new Bundle();
        final XMLRPCClient client = new XMLRPCClient("http://www.livejournal.com/interface/xmlrpc");

        try {
            @SuppressWarnings("unchecked")
            final HashMap<String, Object> result = (HashMap<String, Object>) client.call(name, params);

            Log.d(TAG, result.toString());

            bundle.putBoolean("success", true);
            bundle.putSerializable("result", result);
        } catch (XMLRPCFault e) {
            bundle.putBoolean("success", false);
            bundle.putString("error", e.getFaultString());
        } catch (Exception e) {
            bundle.putBoolean("success", false);
            bundle.putString("error", e.getLocalizedMessage());
        }

        final Message msg = handler.obtainMessage();
        msg.setData(bundle);
        handler.sendMessage(msg);
}
}

package net.piratus.simplejournal.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticationService extends Service {
    public IBinder onBind(Intent intent) {
        return new Authenticator(this).getIBinder();
    }
}

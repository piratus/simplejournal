package net.piratus.simplejournal2;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * User: piratus
 * Date: May 4, 2010
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

}


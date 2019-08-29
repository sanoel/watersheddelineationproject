package org.waterapps.watershed;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import org.waterapps.watershed.MainActivity;
import org.waterapps.watershed.R;


/**
 * Created by steve on 6/27/13.
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{
    static Preference demDirPref;
//    MainActivity mainActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        this.mainActivity = (MainActivity) MainActivity.context;
        addPreferencesFromResource(R.xml.settings_preferences);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences.registerOnSharedPreferenceChangeListener(this);
        preferences.edit().putBoolean("pref_key_fill_all", preferences.getBoolean("pref_key_fill_all", false)).commit();
        boolean fillAllPits = preferences.getBoolean("pref_key_fill_all", false);
        // Enable/Disable the rainfall amount edittext based on whether fill_all_pits is checked
        Preference pref = findPreference("pref_rainfall_amount");
        if (fillAllPits) {
            pref.setEnabled(false);
        } else {
            pref.setEnabled(true);
        }
        pref.setSummary(Float.toString(DelineationAppConfigs.rainfallDepth/0.0254f)+" Inches of Rainfall");
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_key_fill_all")) {
            boolean fillAllPits = sharedPreferences.getBoolean(key, false);
            if (fillAllPits) {
                Preference pref = findPreference("pref_rainfall_amount");
                pref.setEnabled(false);
            } else {
                Preference pref = findPreference("pref_rainfall_amount");
                pref.setEnabled(true);
            }
        } else if (key.equals("pref_rainfall_amount")) {
        	Preference pref = findPreference("pref_rainfall_amount");
            pref.setSummary(sharedPreferences.getString("pref_rainfall_amount", "1.0")+"-Inch, 24-Hour Storm");
        }
    }
    
//    public static void updateDemFolder() {
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(g);
//        demDirPref.setSummary(preferences.getString("dem_dir", "/dem"));
//    }
}
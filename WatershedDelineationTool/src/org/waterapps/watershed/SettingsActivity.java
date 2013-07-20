package org.waterapps.watershed;

import android.content.SharedPreferences;
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
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    static Preference demDirPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        MainActivity.prefs.registerOnSharedPreferenceChangeListener(this);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
        preferences.edit().putBoolean("pref_key_fill_all", preferences.getBoolean("pref_key_fill_all", true)).commit();
        boolean fillAllPits = preferences.getBoolean("pref_key_fill_all", true);
        if (fillAllPits) {
            Preference pref = findPreference("pref_rainfall_amount");
            pref.setEnabled(false);
        } else {
            Preference pref = findPreference("pref_rainfall_amount");
            pref.setEnabled(true);
        }
        Preference preference = findPreference("pref_rainfall_amount");
        Log.w("settingsoncreate rainfall depth", Double.toString(RainfallSimConfig.rainfallDepth/0.0254));
        Log.w("settingsoncreate - fill all?", Boolean.toString(MainActivity.watershedDataset.fillAllPits));
        preference.setSummary(Double.toString(RainfallSimConfig.rainfallDepth/0.0254)+"-Inch, 24-Hour Storm");
//        preference = findPreference("dem_dir");
//        demDirPref = preference;
//        preference.setSummary(preferences.getString("dem_dir", "/dem"));
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (key.equals("pref_key_fill_all")) {
            boolean fillAllPits = sharedPreferences.getBoolean(key, true);
            if (fillAllPits) {
                WatershedDataset.fillAllPits = fillAllPits;    
                Preference pref = findPreference("pref_rainfall_amount");
                pref.setEnabled(false);
            } else {
            	WatershedDataset.fillAllPits = fillAllPits;    
                Preference pref = findPreference("pref_rainfall_amount");
                pref.setEnabled(true);
            }
        } else if (key.equals("pref_key_dem_trans_level")) {
            int alpha = sharedPreferences.getInt(key, 50);
            MainActivity.setDemAlpha(1 - (float) alpha / 100.0f);
        } else if (key.equals("pref_rainfall_amount")) {
        	RainfallSimConfig.setDepth(Double.parseDouble(sharedPreferences.getString("pref_rainfall_amount", "100.0")));
        	Preference pref = findPreference("pref_rainfall_amount");
            pref.setSummary(sharedPreferences.getString("pref_rainfall_amount", "1.0")+"-Inch, 24-Hour Storm");
        } else if (key.equals("pref_key_catchments_trans_level")) {
            int alpha = sharedPreferences.getInt(key, 50);
            MainActivity.setCatchmentsAlpha(1 - (float) alpha / 100.0f);     
        }
    }
    public static void updateDemFolder() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
        demDirPref.setSummary(preferences.getString("dem_dir", "/dem"));
    }
}
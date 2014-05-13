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
    MainActivity mainActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preferences);
        MainActivity.prefs.registerOnSharedPreferenceChangeListener(this);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
        preferences.edit().putBoolean("pref_key_fill_all", preferences.getBoolean("pref_key_fill_all", false)).commit();
        boolean fillAllPits = preferences.getBoolean("pref_key_fill_all", false);
        Preference pref = findPreference("pref_rainfall_amount");
        if (fillAllPits) {
            pref.setEnabled(false);
        } else {
            pref.setEnabled(true);
        }
        pref.setSummary(Float.toString(RainfallSimConfig.rainfallDepth/0.0254f)+"-Inch, 24-Hour Storm");
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if (key.equals("pref_key_fill_all")) {
            boolean fillAllPits = sharedPreferences.getBoolean(key, false);
            if (fillAllPits) {
                WatershedDataset.fillAllPits = fillAllPits;    
                Preference pref = findPreference("pref_rainfall_amount");
                pref.setEnabled(false);
            } else {
            	WatershedDataset.fillAllPits = fillAllPits;    
                Preference pref = findPreference("pref_rainfall_amount");
                pref.setEnabled(true);
            }
        } else if (key.equals("pref_key_dem_vis")) {
            MainActivity.demVisible = sharedPreferences.getBoolean("pref_key_dem_vis", true);
        } else if (key.equals("pref_key_pits_vis")) {
            MainActivity.pitsVisible = sharedPreferences.getBoolean("pref_key_pits_vis", true);
        } else if (key.equals("pref_key_delin_vis")) {
        	MainActivity.delineationVisible = sharedPreferences.getBoolean("pref_key_pits_vis", true);
        } else if (key.equals("pref_key_puddle_vis")) {
        	MainActivity.puddleVisible = sharedPreferences.getBoolean("pref_key_pits_vis", true);
        } else if (key.equals("pref_key_dem_trans_level")) {
            int alpha = sharedPreferences.getInt(key, 50);
            mainActivity.setDemAlpha(1 - (float) alpha / 100.0f);
        } else if (key.equals("pref_key_delin_trans_level")) {
            int alpha = sharedPreferences.getInt(key, 50);
            mainActivity.setDelineationAlpha(1 - (float) alpha / 100.0f);
        } else if (key.equals("pref_key_puddle_trans_level")) {
            int alpha = sharedPreferences.getInt(key, 50);
            mainActivity.setPuddleAlpha(1 - (float) alpha / 100.0f);
        } else if (key.equals("pref_key_catchments_trans_level")) {
            int alpha = sharedPreferences.getInt(key, 50);
            mainActivity.setCatchmentsAlpha(1 - (float) alpha / 100.0f);
        } else if (key.equals("pref_rainfall_amount")) {
        	RainfallSimConfig.setDepth(Float.parseFloat(sharedPreferences.getString("pref_rainfall_amount", "1.0")));
        	Preference pref = findPreference("pref_rainfall_amount");
            pref.setSummary(sharedPreferences.getString("pref_rainfall_amount", "1.0")+"-Inch, 24-Hour Storm");
            MainActivity.watershedDataset.recalculatePitsForNewRainfall();
        }
    }
    public static void updateDemFolder() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
        demDirPref.setSummary(preferences.getString("dem_dir", "/dem"));
    }
}
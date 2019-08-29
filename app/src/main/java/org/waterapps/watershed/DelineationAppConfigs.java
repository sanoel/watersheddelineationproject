package org.waterapps.watershed;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DelineationAppConfigs {
	
//	static SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
	static float rainfallDuration = 24.0f; // hours
	static float rainfallDepth = 0.0254f; // cm
	static float rainfallIntensity = rainfallDepth / rainfallDuration;
	static float uniformDrainagePercentage = 0.5f;
	
	static void setDepth(float depthInInches) {
	  DelineationAppConfigs.rainfallDepth = depthInInches * 0.0254f; // convert inches to meters
	  DelineationAppConfigs.rainfallIntensity = DelineationAppConfigs.rainfallDepth / DelineationAppConfigs.rainfallDuration;
	}
	
	static void setDuration(float duration) {
		  DelineationAppConfigs.rainfallDuration = duration; // hours
		  DelineationAppConfigs.rainfallIntensity = DelineationAppConfigs.rainfallDepth / DelineationAppConfigs.rainfallDuration;
	}
	
	static void setDrainage(float drainage) {
		  DelineationAppConfigs.uniformDrainagePercentage = drainage / 100; // percentage
	}
}

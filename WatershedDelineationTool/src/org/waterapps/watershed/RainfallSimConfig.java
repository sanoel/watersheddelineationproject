package org.waterapps.watershed;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class RainfallSimConfig {
	static SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
	static float rainfallDuration = 24.0f; // hours
	static float rainfallDepth = 0.0254f; // cm
	static float rainfallIntensity = rainfallDepth / rainfallDuration;
	static float uniformDrainagePercentage = 0.5f;
	
	static void setDepth(float depthInInches) {
	  RainfallSimConfig.rainfallDepth = depthInInches * 0.0254f; // convert inches to centimeters
	  RainfallSimConfig.rainfallIntensity = RainfallSimConfig.rainfallDepth / RainfallSimConfig.rainfallDuration;
	}
	
	static void setDuration(float duration) {
		  RainfallSimConfig.rainfallDuration = duration; // hours
		  RainfallSimConfig.rainfallIntensity = RainfallSimConfig.rainfallDepth / RainfallSimConfig.rainfallDuration;
		}
	
	static void setDrainage(float drainage) {
		  RainfallSimConfig.uniformDrainagePercentage = drainage / 100; // percentage
		}
}

package org.waterapps.watersheddelineation;

public class RainfallSimConfig {
	static double rainfallDuration = 24.0; // hours
	static double rainfallDepth = 1.0 * 0.0254; // cm
	static double rainfallIntensity = rainfallDepth / rainfallDuration;
	static double uniformDrainagePercentage = 0.5;
	
	static void setDepth(double depth) {
	  RainfallSimConfig.rainfallDepth = depth * 0.0254; // convert inches to centimeters
	  RainfallSimConfig.rainfallIntensity = RainfallSimConfig.rainfallDepth / RainfallSimConfig.rainfallDuration;
	}
	
	static void setDuration(double duration) {
		  RainfallSimConfig.rainfallDuration = duration; // hours
		  RainfallSimConfig.rainfallIntensity = RainfallSimConfig.rainfallDepth / RainfallSimConfig.rainfallDuration;
		}
	
	static void setDrainage(double drainage) {
		  RainfallSimConfig.uniformDrainagePercentage = drainage / 100; // percentage
		}
}

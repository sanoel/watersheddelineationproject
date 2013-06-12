package com.precisionag.watersheddelineationtool;

public class FilledDataset {
	FlowDirectionCell[][] fillFlowDirection;
	double[][] fillDEM;
	double[][] fillDrainage;
	PitRaster fillPits;
	double cellSize;
	double rainfallDuration;
	double rainfallDepth;
	double rainfallIntensity;
	
	// Constructor
	public FilledDataset(double[][] inputDEM, double[][] inputDrainage, FlowDirectionCell[][] inputFlowDirection, PitRaster inputPits, double inputCellSize, double inputRainfallDuration, double inputRainfallDepth) {
		fillDEM = inputDEM;
		fillDrainage = inputDrainage;
		fillFlowDirection = inputFlowDirection;
		fillPits = inputPits;
		cellSize = inputCellSize;
		rainfallDuration = inputRainfallDuration;
		rainfallDepth = inputRainfallDepth;
		rainfallIntensity = rainfallDepth/rainfallDuration;
	}
}

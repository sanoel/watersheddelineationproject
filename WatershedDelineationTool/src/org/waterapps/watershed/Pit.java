package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;

public class Pit implements Comparable{
	int pitId; // pit identification number.  Negatives flow off the DEM edge while positive value pits have storage
//	int color;
	List<Point> allPointsList; // this is used to speed up several calculations when pits merge: Re-IDing the two merging pits, and finding new retention volume critical to causal sequential depression filling
	List<Point> pitBorderIndicesList; // list of indices along the border of the depression.  Used to find new spillover elevation when merger takes place
	float spilloverElevation = Float.NaN; // threshold elevation before the depression will overflow
	Point pitOutletPoint; // spillover elevation and 
	Point outletSpilloverFlowDirection;  // cell to which this pit will overflow
	float retentionVolume; // used to calculate spillover time.  Derived from elevation difference from spillover elevation to pit cells lower than spillover elevation
	float filledVolume; // critical to know when pits merge and calculate 
	float spilloverTime; // used to order depressions
	float pitDrainageRate; // NOT USED CURRENTLY (left null) for accumulation rate calculation
//	float netAccumulationRate; // rate of water accumulation

	// Constructor method for initial pit identification
//	public Pit(List<Point> indicesDrainingToPit, int color, int pitId) {
	public Pit(List<Point> indicesDrainingToPit, int pitId) {
		this.allPointsList = indicesDrainingToPit;
//		this.color = color;
		this.pitId = pitId;
	}
	
	public void completePitConstruction(float[][] drainage, float inputCellSizeX, float inputCellSizeY, float[][] inputDem, int[][] inputPitIdMatrix) {
		float cellSizeX = inputCellSizeX;
		float cellSizeY = inputCellSizeY;
		float[][] DEM = inputDem;
		int[][] pitIdMatrix = inputPitIdMatrix;
//		float[][]drainage = inputDrainage;
				
		// Border-dependent variables and calculations
		pitBorderIndicesList = new ArrayList<Point>(allPointsList);
		for (int i = 0; i < allPointsList.size(); i++) {
			Point currentPoint = new Point(allPointsList.get(i));
			int r = currentPoint.y;
			int c = currentPoint.x;
			boolean onBorder = false;
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					if (currentPoint.y+y > pitIdMatrix.length-1 || currentPoint.y+y < 0 || currentPoint.x+x > pitIdMatrix[0].length-1 || currentPoint.x+x < 0) {
						continue;
					}
					if (pitIdMatrix[r+y][c+x] != pitIdMatrix[r][c] || (r == pitIdMatrix.length-1 || r == 0 || c == pitIdMatrix[0].length-1 || c == 0)) {
						float currentElevation = DEM[r][c];
						float neighborElevation = DEM[r+y][c+x];
						onBorder = true;
						if ((Float.isNaN(spilloverElevation)) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
							spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
							pitOutletPoint = currentPoint;
							outletSpilloverFlowDirection = new Point(c+x, r+y);
//							pitIdOverflowingInto = pitIdMatrix[r+y][c+x];
						}
					}
				}
			}
			if (onBorder == false) {
				pitBorderIndicesList.remove(currentPoint);
			}
		}
		
		// Volume/elevation-dependent variables and calculations
		retentionVolume = 0.0f;
		for (int listIdx = 0; listIdx < allPointsList.size(); listIdx++) {
			Point currentPoint = new Point(allPointsList.get(listIdx));
			int r = currentPoint.y;
			int c = currentPoint.x;
			if (DEM[r][c] < spilloverElevation) {
				retentionVolume = retentionVolume + ((spilloverElevation-DEM[r][c])*cellSizeX*cellSizeY);
			}
		}
		filledVolume = 0.0f;
		pitDrainageRate = 0.0f;
		float netAccumulationRate = (RainfallSimConfig.rainfallIntensity * allPointsList.size() * cellSizeX*cellSizeY);//cubic meters per hour          - pitDrainageRate
		spilloverTime = retentionVolume / netAccumulationRate; //hours
	}
	
	public void completeNegativePitConstruction(float[][] drainage, float inputCellSizeX, float inputCellSizeY, float[][] inputDem, int[][] inputPitIdMatrix) {
		float cellSizeX = inputCellSizeX;
		float cellSizeY = inputCellSizeY;
		float[][] DEM = inputDem;
		int[][] pitIdMatrix = inputPitIdMatrix;
//		float[][]drainage = inputDrainage;
				
		// Border-dependent variables and calculations
		pitBorderIndicesList = new ArrayList<Point>(allPointsList);
		for (int i = 0; i < allPointsList.size(); i++) {
			Point currentPoint = new Point(allPointsList.get(i));
			int r = currentPoint.y;
			int c = currentPoint.x;
			boolean onBorder = false;
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					if (currentPoint.y+y > pitIdMatrix.length-1 || currentPoint.y+y < 0 || currentPoint.x+x > pitIdMatrix[0].length-1 || currentPoint.x+x < 0) {
						continue;
					}
					if (pitIdMatrix[r+y][c+x] != pitIdMatrix[r][c] || (r == pitIdMatrix.length-1 || r == 0 || c == pitIdMatrix[0].length-1 || c == 0)) {
						float currentElevation = DEM[r][c];
						float neighborElevation = DEM[r+y][c+x];
						onBorder = true;
						if ((Float.isNaN(spilloverElevation)) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
							spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
							pitOutletPoint = currentPoint;
							outletSpilloverFlowDirection = new Point(c+x, r+y);
//							pitIdOverflowingInto = pitIdMatrix[r+y][c+x];
						}
					}
				}
			}
			if (onBorder == false) {
				pitBorderIndicesList.remove(currentPoint);
			}
		}
		
		// Volume/elevation-dependent variables and calculations
		retentionVolume = 0.0f;
		filledVolume = 0.0f;
		pitDrainageRate = 0.0f;
//		float netAccumulationRate = (RainfallSimConfig.rainfallIntensity * allPointsList.size() * cellSizeX*cellSizeY) - pitDrainageRate;//cubic meters per hour          - pitDrainageRate
//		spilloverTime = retentionVolume / netAccumulationRate; //hours
//		if (spilloverTime <= 0.0) {
			spilloverTime = Float.POSITIVE_INFINITY;
//		}
	}
		
	public String toString() {
		return " \nPit ID: " + Integer.toString(pitId) +
			   " \nArea: " + Integer.toString(allPointsList.size()) + 
			   " \nVolume: " + Float.toString(retentionVolume) +
			   " \nFilled Volume: " + Float.toString(filledVolume) +
//			   " \nNet Accumulation Rate: " + Float.toString(netAccumulationRate) +
			   " \nSpillover Time: " + Float.toString(spilloverTime) + 
			   " \nSpillover Elevation: " + Float.toString(spilloverElevation) +
			   " \nPit Outlet Point: " + pitOutletPoint.toString() +
			   " \nOutlet Spillover Flow Direction: " + outletSpilloverFlowDirection.toString() +
//			   " \nPit Overflowing Into: " + Integer.toString(pitIdOverflowingInto) +
			   " \nRainfall Intensity: " + Float.toString(RainfallSimConfig.rainfallIntensity);
	}
	
	@Override
	public int compareTo(Object obj) {
		Pit f = (Pit) obj;
		
		if (spilloverTime > f.spilloverTime) {
			return 1;
		}
		else if (spilloverTime < f.spilloverTime) {
			return -1;
		}
		else {
			return 0;
		}
	}

}
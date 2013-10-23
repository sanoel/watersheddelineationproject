package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;

public class Pit implements Comparable{
	// single pit cell variables
	int pitId;
//	Point pitBottomPoint;
	int color;
	// Whole pit depression variables
	List<Point> allPointsList;
	// Border-dependent variables and calculations
	List<Point> pitBorderIndicesList;
	float spilloverElevation = Float.NaN;
	int pitIdOverflowingInto;
	Point pitOutletPoint;
	Point outletSpilloverFlowDirection; 
	// Volume/elevation-dependent variables and calculations
	double retentionVolume;
	double filledVolume;
	double spilloverTime;
//	int cellCountToBeFilled; // at spillover, the area of inundated cells (# of cells)	
	double pitDrainageRate;
	double netAccumulationRate;

	// Constructor method for initial pit identification
	public Pit(List<Point> indicesDrainingToPit, int color, int pitId) {
		this.allPointsList = indicesDrainingToPit;
		this.color = color;
		this.pitId = pitId;
	}
	
	public void completePitConstruction(float[][] drainage, float inputCellSizeX, float inputCellSizeY, float[][] inputDem, int[][] inputPitIdMatrix) {
		double cellSizeX = inputCellSizeX;
		float cellSizeY = inputCellSizeY;
		float[][] DEM = inputDem;
		int[][] pitIdMatrix = inputPitIdMatrix;
//		double[][]drainage = inputDrainage;
				
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
					if (currentPoint.y+y >= pitIdMatrix.length-1 || currentPoint.y+y <= 0 || currentPoint.x+x >= pitIdMatrix[0].length-1 || currentPoint.x+x <= 0) {
						continue;
					}
					if (pitIdMatrix[r+y][c+x] != pitIdMatrix[r][c] || (r == pitIdMatrix.length-1 || r == 0 || c == pitIdMatrix[0].length-1 || c == 0)) {
						double currentElevation = DEM[r][c];
						double neighborElevation = DEM[r+y][c+x];
						onBorder = true;
						if ((Float.isNaN(spilloverElevation)) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
							spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
							pitOutletPoint = currentPoint;
							outletSpilloverFlowDirection = new Point(c+x, r+y);
							pitIdOverflowingInto = pitIdMatrix[r+y][c+x];
						}
					}
				}
			}
			if (onBorder == false) {
				pitBorderIndicesList.remove(currentPoint);
			}
		}
		
		// Volume/elevation-dependent variables and calculations
		retentionVolume = 0.0;
		for (int listIdx = 0; listIdx < allPointsList.size(); listIdx++) {
			Point currentPoint = new Point(allPointsList.get(listIdx));
			int r = currentPoint.y;
			int c = currentPoint.x;
			if (DEM[r][c] < spilloverElevation) {
				retentionVolume = retentionVolume + ((spilloverElevation-DEM[r][c])*cellSizeX*cellSizeY);
			}
		}
		filledVolume = 0.0;
		pitDrainageRate = 0.0;
		netAccumulationRate = (RainfallSimConfig.rainfallIntensity * allPointsList.size() * cellSizeX*cellSizeY);//cubic meters per hour          - pitDrainageRate
		spilloverTime = retentionVolume / netAccumulationRate; //hours
	}
	
	public void completeNegativePitConstruction(float[][] drainage, float inputCellSizeX, float inputCellSizeY, float[][] inputDem, int[][] inputPitIdMatrix) {
		double cellSizeX = inputCellSizeX;
		float cellSizeY = inputCellSizeY;
		float[][] DEM = inputDem;
		int[][] pitIdMatrix = inputPitIdMatrix;
//		double[][]drainage = inputDrainage;
				
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
					if (currentPoint.y+y >= pitIdMatrix.length-1 || currentPoint.y+y <= 0 || currentPoint.x+x >= pitIdMatrix[0].length-1 || currentPoint.x+x <= 0) {
						continue;
					}
					if (pitIdMatrix[r+y][c+x] != pitIdMatrix[r][c] || (r == pitIdMatrix.length-1 || r == 0 || c == pitIdMatrix[0].length-1 || c == 0)) {
						double currentElevation = DEM[r][c];
						double neighborElevation = DEM[r+y][c+x];
						onBorder = true;
						if ((Float.isNaN(spilloverElevation)) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
							spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
							pitOutletPoint = currentPoint;
							outletSpilloverFlowDirection = new Point(c+x, r+y);
							pitIdOverflowingInto = pitIdMatrix[r+y][c+x];
						}
					}
				}
			}
			if (onBorder == false) {
				pitBorderIndicesList.remove(currentPoint);
			}
		}
		
		// Volume/elevation-dependent variables and calculations
		retentionVolume = 0.0;
		filledVolume = 0.0;
		pitDrainageRate = Double.POSITIVE_INFINITY;
		netAccumulationRate = (RainfallSimConfig.rainfallIntensity * allPointsList.size() * cellSizeX*cellSizeY) - pitDrainageRate;//cubic meters per hour          - pitDrainageRate
//		spilloverTime = retentionVolume / netAccumulationRate; //hours
//		if (spilloverTime <= 0.0) {
			spilloverTime = Double.POSITIVE_INFINITY;
//		}
	}
		
	public String toString() {
		return " \nPit ID: " + Integer.toString(pitId) +
			   " \nArea: " + Integer.toString(allPointsList.size()) + 
			   " \nVolume: " + Double.toString(retentionVolume) +
			   " \nFilled Volume: " + Double.toString(filledVolume) +
			   " \nNet Accumulation Rate: " + Double.toString(netAccumulationRate) +
			   " \nSpillover Time: " + Double.toString(spilloverTime) + 
			   " \nSpillover Elevation: " + Double.toString(spilloverElevation) +
			   " \nPit Outlet Point: " + pitOutletPoint.toString() +
			   " \nOutlet Spillover Flow Direction: " + outletSpilloverFlowDirection.toString() +
			   " \nPit Overflowing Into: " + Integer.toString(pitIdOverflowingInto) +
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
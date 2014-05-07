package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Point;
import android.util.Log;

public class Pit implements Comparable{
	int pitId; // pit identification number.  Negatives flow off the DEM edge while positive value pits have storage
	Point pitPoint;
//	List<Point> allPointsList; // this is used to speed up several calculations when pits merge: Re-IDing the two merging pits, and finding new retention volume critical to causal sequential depression filling
	List<Point> pitBorderIndicesList; // list of indices along the border of the depression.  Used to find new spillover elevation when merger takes place
	float spilloverElevation = Float.NaN; // threshold elevation before the depression will overflow
	Point pitOutletPoint; // spillover elevation and 
	Point outletSpilloverFlowDirection;  // cell to which this pit will overflow
	int area;
	float retentionVolume; // used to calculate spillover time.  Derived from elevation difference from spillover elevation to pit cells lower than spillover elevation
	float filledVolume; // critical to know when pits merge and calculate 
	float spilloverTime; // used to order depressions
	float pitDrainageRate; // NOT USED CURRENTLY (left null) for accumulation rate calculation

	// Constructor method for initial pit identification
//	public Pit(List<Point> indicesDrainingToPit, int pitId, Point pitPoint) {
//		this.allPointsList = indicesDrainingToPit;
//		this.pitPoint = pitPoint;
//		this.pitId = pitId;
//	}
	
//	public void completePitConstruction(float[][] drainage, float cellSize, float[][] dem, int[][] pitIdMatrix) {				
//		// Border-dependent variables and calculations
//		pitBorderIndicesList = new ArrayList<Point>(allPointsList);
//		for (int i = 0; i < allPointsList.size(); i++) {
//			Point currentPoint = new Point(allPointsList.get(i));
//			int r = currentPoint.y;
//			int c = currentPoint.x;
//			boolean onBorder = false;
//			for (int x = -1; x < 2; x++) {
//				for (int y = -1; y < 2; y++){
//					if (x == 0 && y == 0) {
//						continue;
//					}
//					if (currentPoint.y+y > pitIdMatrix.length-1 || currentPoint.y+y < 0 || currentPoint.x+x > pitIdMatrix[0].length-1 || currentPoint.x+x < 0) {
//						continue;
//					}
//					if (pitIdMatrix[r+y][c+x] != pitIdMatrix[r][c] || (r == pitIdMatrix.length-1 || r == 0 || c == pitIdMatrix[0].length-1 || c == 0)) {
//						float currentElevation = dem[r][c];
//						float neighborElevation = dem[r+y][c+x];
//						onBorder = true;
//						if ((Float.isNaN(spilloverElevation)) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
//							spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
//							pitOutletPoint = currentPoint;
//							outletSpilloverFlowDirection = new Point(c+x, r+y);
////							pitIdOverflowingInto = pitIdMatrix[r+y][c+x];
//						}
//					}
//				}
//			}
//			if (onBorder == false) {
//				pitBorderIndicesList.remove(currentPoint);
//			}
//		}
//		
//		// Volume/elevation-dependent variables and calculations
//		retentionVolume = 0.0f;
//		for (int listIdx = 0; listIdx < allPointsList.size(); listIdx++) {
//			Point currentPoint = new Point(allPointsList.get(listIdx));
//			int r = currentPoint.y;
//			int c = currentPoint.x;
//			if (dem[r][c] < spilloverElevation) {
//				retentionVolume = retentionVolume + ((spilloverElevation-dem[r][c])*cellSize*cellSize);
//			}
//		}
//		filledVolume = 0.0f;
//		pitDrainageRate = 0.0f;
//		float netAccumulationRate = (RainfallSimConfig.rainfallIntensity * allPointsList.size() * cellSize*cellSize);//cubic meters per hour          - pitDrainageRate
//		spilloverTime = retentionVolume / netAccumulationRate; //hours
//	}
//	
//	public void completeNegativePitConstruction(float[][] drainage, float cellSize, float[][] dem, int[][] pitIdMatrix) {	
//		// Border-dependent variables and calculations
//		pitOutletPoint = pitPoint;
//		spilloverElevation = dem[pitOutletPoint.y][pitOutletPoint.x];
//		
//		pitBorderIndicesList = new ArrayList<Point>(allPointsList);
//		for (int i = 0; i < allPointsList.size(); i++) {
//			Point currentPoint = new Point(allPointsList.get(i));
//			int r = currentPoint.y;
//			int c = currentPoint.x;
//			boolean onBorder = false;
//			for (int x = -1; x < 2; x++) {
//				for (int y = -1; y < 2; y++){
//					if (x == 0 && y == 0) {
//						continue;
//					}
//					if (currentPoint.y+y > pitIdMatrix.length-1 || currentPoint.y+y < 0 || currentPoint.x+x > pitIdMatrix[0].length-1 || currentPoint.x+x < 0) {
//						continue;
//					}
//					if (pitIdMatrix[r+y][c+x] != pitIdMatrix[r][c] || (r == pitIdMatrix.length-1 || r == 0 || c == pitIdMatrix[0].length-1 || c == 0)) {
//						onBorder = true;
//					}
//				}
//			}
//			if (onBorder == false) {
//				pitBorderIndicesList.remove(currentPoint);
//			}
//		}
//		
//		// Volume/elevation-dependent variables and calculations
//		retentionVolume = 0.0f;
//		filledVolume = 0.0f;
//		pitDrainageRate = 0.0f;
//		spilloverTime = Float.POSITIVE_INFINITY;
//	}
	
	public Pit(int pitId, Point pitPoint) {
		this.pitPoint = pitPoint;
		this.pitId = pitId;
	}
	
//	public Pit(Pit pit) {
//		this.pitPoint = pit.pitPoint;
//		this.pitId = pit.pitId;
//		this.pitBorderIndicesList = pit.pitBorderIndicesList;
//		this.spilloverElevation = pit.spilloverElevation;
//		this.pitOutletPoint = pit.pitOutletPoint; 
//		this.outletSpilloverFlowDirection = pit.outletSpilloverFlowDirection;
//		this.area = pit.area;
//		this.retentionVolume = pit.retentionVolume;
//		this.filledVolume = pit.filledVolume;  
//		this.spilloverTime = pit.spilloverTime; 
//		this.pitDrainageRate = pit.pitDrainageRate;
//	}
	
	public void completePitConstruction(List<Point> indicesDrainingToPit, float[][] drainage, float cellSize, float[][] dem, int[][] pitIdMatrix) {		
		area = indicesDrainingToPit.size();
				
		// Border-dependent variables and calculations
		pitBorderIndicesList = new ArrayList<Point>(indicesDrainingToPit);
		for (int i = 0; i < indicesDrainingToPit.size(); i++) {
			Point currentPoint = new Point(indicesDrainingToPit.get(i));
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
					if (pitIdMatrix[r+y][c+x] != pitId || (r == pitIdMatrix.length-1 || r == 0 || c == pitIdMatrix[0].length-1 || c == 0)) {
						float currentElevation = dem[r][c];
						float neighborElevation = dem[r+y][c+x];
						onBorder = true;
						if ((Float.isNaN(spilloverElevation)) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
							spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
							pitOutletPoint = currentPoint;
							outletSpilloverFlowDirection = new Point(c+x, r+y);
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
		float netAccumulationRate = (RainfallSimConfig.rainfallIntensity * indicesDrainingToPit.size() * cellSize*cellSize);//cubic meters per hour          - pitDrainageRate
		spilloverTime = retentionVolume / netAccumulationRate; //hours
	}
	
	public void completeNegativePitConstruction(List<Point> indicesDrainingToPit, float[][] drainage, float cellSize, float[][] dem, int[][] pitIdMatrix) {		
		area = indicesDrainingToPit.size();
				
		// Border-dependent variables and calculations
		pitOutletPoint = pitPoint;
		spilloverElevation = dem[pitOutletPoint.y][pitOutletPoint.x];
		
		pitBorderIndicesList = new ArrayList<Point>(indicesDrainingToPit);
		for (int i = 0; i < indicesDrainingToPit.size(); i++) {
			Point currentPoint = new Point(indicesDrainingToPit.get(i));
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
					if (pitIdMatrix[r+y][c+x] != pitId || (r == pitIdMatrix.length-1 || r == 0 || c == pitIdMatrix[0].length-1 || c == 0)) {
						onBorder = true;
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
		spilloverTime = Float.POSITIVE_INFINITY;
	}
	
	public String toString() {
		return " \nPit ID: " + Integer.toString(pitId) +
//			   " \nArea: " + Integer.toString(allPointsList.size()) + 
//			   " \nArea: " + Integer.toString(area) +
			   " \nVolume: " + Float.toString(retentionVolume) +
			   " \nFilled Volume: " + Float.toString(filledVolume) +
			   " \nSpillover Time: " + Float.toString(spilloverTime) + 
			   " \nSpillover Elevation: " + Float.toString(spilloverElevation) +
			   " \nPit Point: " + pitPoint.toString() +
			   " \nPit Outlet Point: " + pitOutletPoint.toString() +
			   " \nOutlet Spillover Flow Direction: " + outletSpilloverFlowDirection.toString() +
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
package org.waterapps.watersheddelineation;

import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

public class Pit implements Comparable{
	// single pit cell variables
	int pitID;
	double pitBottomElevation;
	Point pitBottomPoint;
	int color;
	// Whole pit depression variables
	List<Point> allPointsList;
	int areaCellCount;
	// Border-dependent variables and calculations
	List<Point> pitBorderIndicesList;
	double minOutsidePerimeterElevation; // Cells
	double minInsidePerimeterElevation;
	float spilloverElevation = Float.NaN;
	int pitIdOverflowingInto;
	Point pitOutletPoint; // in the pit (corresponds to point where #3 occurs)
	Point outletSpilloverFlowDirection; 
	// Volume/elevation-dependent variables and calculations
	double retentionVolume;
	double filledVolume;
	double spilloverTime;
	int cellCountToBeFilled; // at spillover, the area of inundated cells (# of cells)	
	double pitDrainageRate;
	double netAccumulationRate;

	// Constructor method for pits that are merging
//	public Pit(WatershedDataset watershedDataset) {
//		//Identify The Two Merging Pits
//		Pit overflowingPit = watershedDataset.pits.pitDataList.get(0);
////		Log.w("Pit", Integer.toString(watershedDataset.fillPits.pitDataList.get(0).pitIdOverflowingInto));`
//		Pit pitOverflowedInto = watershedDataset.pits.pitDataList.get(watershedDataset.pits.getIndexOf(watershedDataset.pits.pitDataList.get(0).pitIdOverflowingInto));
//
//		double cellSize = watershedDataset.cellSize;
//		double rainfallIntensity = DelineationSettings.rainfallIntensity;
//		double[][] DEM = watershedDataset.DEM;
//		int[][] pits = watershedDataset.pits.pitIDMatrix;
//		double[][] drainage = watershedDataset.drainage;
//		
//		allPointsList = new ArrayList<Point>();
//		// single pit cell variables
//		pitID = watershedDataset.pits.getMaximumPitID() + 1;
//		pitBottomElevation = pitOverflowedInto.pitBottomElevation;
//		pitBottomPoint = pitOverflowedInto.pitBottomPoint;
//		// New pit takes color of the pit that is being overflowed into
//		pitColor = pitOverflowedInto.pitColor;
//		// Whole pit depression variables
//		allPointsList.addAll(pitOverflowedInto.allPointsList);
//		allPointsList.addAll(overflowingPit.allPointsList);
//		areaCellCount = allPointsList.size();
//		// Border-dependent variables and calculations
//		pitBorderIndicesList = new ArrayList<Point>(pitOverflowedInto.pitBorderIndicesList);
//		pitBorderIndicesList.addAll(overflowingPit.pitBorderIndicesList);
//		for (int i = 0; i < pitBorderIndicesList.size(); i++) {
//			Point currentPoint = pitBorderIndicesList.get(i);
//			int r = currentPoint.y;
//			int c = currentPoint.x;
//			boolean onBorder = false;
//			for (int x = -1; x < 2; x++) {
//				for (int y = -1; y < 2; y++){
//					if (x == 0 && y == 0) {
//						continue;}
//					if (pits[r+y][c+x] != pits[r][c]) {
//						double currentElevation = new double(DEM[r][c]);
//						double neighborElevation = new double(DEM[r+y][c+x]);
//						onBorder = true;
//						if ((spilloverElevation == null) || (currentElevation.doubleValue() <= spilloverElevation.doubleValue() && neighborElevation.doubleValue() <= spilloverElevation.doubleValue())) {
//							minOutsidePerimeterElevation = neighborElevation;
//							minInsidePerimeterElevation = currentElevation;
//							spilloverElevation = neighborElevation.max(currentElevation);
//							pitOutletPoint = currentPoint;
//							outletSpilloverFlowDirection = new Point(c+x, r+y);
//							pitIdOverflowingInto = pits[r+y][c+x];
//						}
//					}
//				}
//			}
//			if (onBorder == false) {
//				pitBorderIndicesList.remove(currentPoint);
//			}
//		}
//		// Volume/elevation-dependent variables and calculations
//		filledVolume = overflowingPit.retentionVolume;
//		retentionVolume = filledVolume;
//		cellCountToBeFilled = 0;
//		for (int i = 0; i < allPointsList.size(); i++) {
//			Point currentPoint = allPointsList.get(i);
//			int r = currentPoint.y;
//			int c = currentPoint.x;
//			if (DEM[r][c] < spilloverElevation.doubleValue()) {
//				retentionVolume = retentionVolume.add(spilloverElevation.subtract(new double(DEM[r][c]), MathContext.DECIMAL64).multiply(cellSize.pow(2, MathContext.DECIMAL64), MathContext.DECIMAL64), MathContext.DECIMAL64);
//				cellCountToBeFilled = cellCountToBeFilled + 1;
//			}
//		}
//		//Sum the drainage taking place in the pit
//		pitDrainageRate = new double(0);
//		for (int listIdx = 0; listIdx < allPointsList.size(); listIdx++) {
//			Point currentPoint = allPointsList.get(listIdx);
//			int r = currentPoint.y;
//			int c = currentPoint.x;
//			pitDrainageRate = pitDrainageRate.add(new double(drainage[r][c]), MathContext.DECIMAL64);
//		}
//		netAccumulationRate = (rainfallIntensity.multiply(areaCellCount)).subtract(pitDrainageRate, MathContext.DECIMAL64);
//		spilloverTime = retentionVolume.divide(cellSize.pow(2, MathContext.DECIMAL64).multiply(netAccumulationRate, MathContext.DECIMAL64), MathContext.DECIMAL64);
//	}

	// Constructor method for initial pit identification
	public Pit(float[][] drainage, double inputCellSize, float[][] inputDem, FlowDirectionCell[][] inputFlowDirection, int[][] inputPits, Point pitPoint, int inputPitID) {
		double cellSize = inputCellSize;
		double rainfallIntensity = RainfallSimConfig.rainfallIntensity;
		float[][] DEM = inputDem;
		FlowDirectionCell[][] flowDirection = inputFlowDirection;
		int[][] pits = inputPits;
		
//		double[][]drainage = inputDrainage;
		allPointsList = new ArrayList<Point>();
		
		// single pit cell variables
		pitID = inputPitID;
//		Log.w("Pit,pitID", Integer.toString(pitID));
		pitBottomElevation = DEM[pitPoint.y][pitPoint.x];
//		Log.w("Pit-pitBottomElevation", pitBottomElevation.toString());
		pitBottomPoint = pitPoint;
//		Log.w("Pit-pitBottomIndex", pitBottomIndex.toString());

		// assign the pit a random color
		Random random = new Random();
		int red = random.nextInt(255);
		int green = random.nextInt(255);
		int blue = random.nextInt(255);
		color = Color.rgb(red,green,blue);
		// Whole pit depression variables
		allPointsList.add(pitBottomPoint);

		allPointsList = findCellsDrainingToPoint(flowDirection, allPointsList);
		areaCellCount = allPointsList.size();
//		Log.w("Pit-pitID", Integer.toString(pitID));		
//		Log.w("Pit-allPointsList", allPointsList.toString());
		
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
						continue;}
					if (pits[r+y][c+x] != pits[r][c]) {
						double currentElevation = DEM[r][c];
						double neighborElevation = DEM[r+y][c+x];
						onBorder = true;
						if ((Double.isNaN(spilloverElevation)) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
							minOutsidePerimeterElevation = neighborElevation;
							minInsidePerimeterElevation = currentElevation;
							spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
							pitOutletPoint = currentPoint;
							outletSpilloverFlowDirection = new Point(c+x, r+y);
							pitIdOverflowingInto = pits[r+y][c+x];
//							Log.w("PitID , overflowPitID", Integer.toString(pits[r][c]) + " : " + Integer.toString(pits[r+y][c+x]) + " " + currentPoint.toString()+ " " + outletSpilloverFlowDirection.toString());
						}
					}
				}
			}
			if (onBorder == false) {
				pitBorderIndicesList.remove(currentPoint);
			}
		}
//		Log.w("Pit-minOutsidePerimeterElevation", Double.toString(minOutsidePerimeterElevation));
//		Log.w("Pit-minInsidePerimeterElevation", Double.toString(minInsidePerimeterElevation));
//		Log.w("Pit-spilloverElevation", Double.toString(spilloverElevation));
//		Log.w("Pit-pitOutletPoint", pitOutletPoint.toString());
//		Log.w("Pit-outletSpilloverFlowDirection", outletSpilloverFlowDirection.toString());
//		Log.w("Pit-pitIdOverflowingInto", Integer.toString(pitIdOverflowingInto));
		
		// Volume/elevation-dependent variables and calculations
		retentionVolume = 0;
		cellCountToBeFilled = 0;
		for (int listIdx = 0; listIdx < allPointsList.size(); listIdx++) {
			Point currentPoint = new Point(allPointsList.get(listIdx));
			int r = currentPoint.y;
			int c = currentPoint.x;
			if (DEM[r][c] < spilloverElevation) {
				retentionVolume = retentionVolume + ((spilloverElevation-DEM[r][c])*Math.pow(cellSize, 2));
				cellCountToBeFilled = cellCountToBeFilled + 1;
			}
		}
		filledVolume = 0;
		pitDrainageRate = 0;
		netAccumulationRate = (rainfallIntensity * areaCellCount * Math.pow(cellSize, 2)); //cubic meters per hour          - pitDrainageRate
		spilloverTime = retentionVolume / netAccumulationRate; //hours
//		Log.e("Pit-ID", Integer.toString(pitID));
//		Log.w("Pit-rainfallIntensity", Double.toString(rainfallIntensity));
//		Log.w("Pit-areaCellCont", Integer.toString(areaCellCount));
//		Log.w("Pit-retentionVolume", Double.toString(retentionVolume));
//		Log.w("Pit-netAccumulationRate", Double.toString(netAccumulationRate));
//		Log.w("Pit-spilloverTime", Double.toString(spilloverTime));
	}
	
	public List<Point> findCellsDrainingToPoint(FlowDirectionCell[][] flowDirection, List<Point> indicesDrainingToPit) {
		List<Point> indicesToCheck = new ArrayList<Point>();
		indicesToCheck.add(indicesDrainingToPit.get(0));
		while (!indicesToCheck.isEmpty()) {
			int r = indicesToCheck.get(0).y;
			int c = indicesToCheck.get(0).x;
			indicesToCheck.remove(0);
			if (flowDirection[r][c].parentList.isEmpty()){
				continue;
			}
			for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
				indicesDrainingToPit.add(flowDirection[r][c].parentList.get(i));
				indicesToCheck.add(flowDirection[r][c].parentList.get(i));
			}
		}
		return indicesDrainingToPit;
	}
	
	@Override
	public int compareTo(Object o) {
		Pit f = (Pit) o;
		
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
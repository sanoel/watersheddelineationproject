package com.precisionag.watersheddelineationtool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.Color;
import android.graphics.Point;

public class Pit implements Comparable{
	// single pit cell variables
	int pitID;
	BigDecimal pitBottomElevation;
	Point pitBottomPoint;
	int pitColor;
	// Whole pit depression variables
	List<Point> allPointsList;
	BigDecimal areaCellCount;
	// Border-dependent variables and calculations
	List<Point> pitBorderIndicesList;
	BigDecimal minOutsidePerimeterElevation; // Cells
	BigDecimal minInsidePerimeterElevation;
	BigDecimal spilloverElevation;
	int pitIdOverflowingInto;
	Point pitOutletPoint; // in the pit (corresponds to point where #3 occurs)
	Point outletSpilloverFlowDirection; 
	// Volume/elevation-dependent variables and calculations
	BigDecimal retentionVolume;
	BigDecimal filledVolume;
	BigDecimal spilloverTime;
	int cellCountToBeFilled; // at spillover, the area of inundated cells (# of cells)	
	BigDecimal pitDrainageRate;
	BigDecimal netAccumulationRate;

	// Constructor method for pits that are merging
	public Pit(WatershedDataset watershedDataset) {
		//Identify The Two Merging Pits
		Pit overflowingPit = watershedDataset.pits.pitDataList.get(0);
//		Log.w("Pit", Integer.toString(watershedDataset.fillPits.pitDataList.get(0).pitIdOverflowingInto));`
		Pit pitOverflowedInto = watershedDataset.pits.pitDataList.get(watershedDataset.pits.getIndexOf(watershedDataset.pits.pitDataList.get(0).pitIdOverflowingInto));

		BigDecimal cellSize = watershedDataset.cellSize;
		BigDecimal rainfallIntensity = watershedDataset.rainfallIntensity;
		double[][] DEM = watershedDataset.DEM;
		int[][] pits = watershedDataset.pits.pitIDMatrix;
		double[][] drainage = watershedDataset.drainage;
		
		allPointsList = new ArrayList<Point>();
		// single pit cell variables
		pitID = watershedDataset.pits.getMaximumPitID() + 1;
		pitBottomElevation = pitOverflowedInto.pitBottomElevation;
		pitBottomPoint = pitOverflowedInto.pitBottomPoint;
		// New pit takes color of the pit that is being overflowed into
		pitColor = pitOverflowedInto.pitColor;
		// Whole pit depression variables
		allPointsList.addAll(pitOverflowedInto.allPointsList);
		allPointsList.addAll(overflowingPit.allPointsList);
		areaCellCount = new BigDecimal(allPointsList.size());
		// Border-dependent variables and calculations
		pitBorderIndicesList = new ArrayList<Point>(pitOverflowedInto.pitBorderIndicesList);
		pitBorderIndicesList.addAll(overflowingPit.pitBorderIndicesList);
		for (int i = 0; i < allPointsList.size(); i++) {
			Point currentPoint = allPointsList.get(i);
			int r = currentPoint.y;
			int c = currentPoint.x;
			boolean onBorder = false;
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;}
					if (pits[r+y][c+x] != pits[r][c]) {
						BigDecimal currentElevation = new BigDecimal(DEM[r][c]);
						BigDecimal neighborElevation = new BigDecimal(DEM[r+y][c+x]);
						onBorder = true;
						if ((spilloverElevation == null) || (currentElevation.doubleValue() <= spilloverElevation.doubleValue() && neighborElevation.doubleValue() <= spilloverElevation.doubleValue())) {
							minOutsidePerimeterElevation = neighborElevation;
							minInsidePerimeterElevation = currentElevation;
							spilloverElevation = neighborElevation.max(currentElevation);
							pitOutletPoint = currentPoint;
							outletSpilloverFlowDirection = new Point(c+x, r+y);
							pitIdOverflowingInto = pits[r+y][c+x];
						}
					}
				}
			}
			if (onBorder == false) {
				pitBorderIndicesList.remove(currentPoint);
			}
		}
		// Volume/elevation-dependent variables and calculations
		filledVolume = overflowingPit.retentionVolume;
		retentionVolume = filledVolume;
		cellCountToBeFilled = 0;
		for (int i = 0; i < allPointsList.size(); i++) {
			Point currentPoint = allPointsList.get(i);
			int r = currentPoint.y;
			int c = currentPoint.x;
			if (DEM[r][c] < spilloverElevation.doubleValue()) {
				retentionVolume = retentionVolume.add(spilloverElevation.subtract(new BigDecimal(DEM[r][c]), MathContext.DECIMAL64).multiply(cellSize.pow(2, MathContext.DECIMAL64), MathContext.DECIMAL64), MathContext.DECIMAL64);
				cellCountToBeFilled = cellCountToBeFilled + 1;
			}
		}
		//Sum the drainage taking place in the pit
		pitDrainageRate = new BigDecimal(0);
		for (int listIdx = 0; listIdx < allPointsList.size(); listIdx++) {
			Point currentPoint = allPointsList.get(listIdx);
			int r = currentPoint.y;
			int c = currentPoint.x;
			pitDrainageRate = pitDrainageRate.add(new BigDecimal(drainage[r][c]), MathContext.DECIMAL64);
		}
		netAccumulationRate = (rainfallIntensity.multiply(areaCellCount)).subtract(pitDrainageRate, MathContext.DECIMAL64);
		spilloverTime = retentionVolume.divide(cellSize.pow(2, MathContext.DECIMAL64).multiply(netAccumulationRate, MathContext.DECIMAL64), MathContext.DECIMAL64);
	}

	// Constructor method for initial pit identification
	public Pit(double[][] inputDrainage, BigDecimal inputCellSize, double[][] inputDEM, FlowDirectionCell[][] inputFlowDirection, int[][] inputPits, Point pitPoint, int inputPitID, BigDecimal inputRainfallIntensity) {
		BigDecimal cellSize = inputCellSize;
		BigDecimal rainfallIntensity = inputRainfallIntensity;
		double[][] DEM = inputDEM;
		FlowDirectionCell[][] flowDirection = inputFlowDirection;
		int[][] pits = inputPits;
		
//		double[][]drainage = inputDrainage;
		allPointsList = new ArrayList<Point>();
		// single pit cell variables
		pitID = inputPitID;
		
//		Log.w("Pit,pitID", Integer.toString(pitID));
		pitBottomElevation = new BigDecimal(DEM[pitPoint.y][pitPoint.x]);
//		Log.w("Pit-pitBottomElevation", pitBottomElevation.toString());
		pitBottomPoint = pitPoint;
//		Log.w("Pit-pitBottomIndex", pitBottomIndex.toString());

		// assign the pit a random color
		Random random = new Random();
		int red = random.nextInt(255);
		int green = random.nextInt(255);
		int blue = random.nextInt(255);
		pitColor = Color.rgb(red,green,blue);
		// Whole pit depression variables
		allPointsList.add(pitBottomPoint);

		allPointsList = findCellsDrainingToPoint(flowDirection, allPointsList);
		areaCellCount = new BigDecimal(allPointsList.size());
//		Log.w("Pit-areaCellCount", areaCellCount.toString());
		
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
						BigDecimal currentElevation = new BigDecimal(DEM[r][c]);
						BigDecimal neighborElevation = new BigDecimal(DEM[r+y][c+x]);
						onBorder = true;
						if ((spilloverElevation == null) || (currentElevation.doubleValue() <= spilloverElevation.doubleValue() && neighborElevation.doubleValue() <= spilloverElevation.doubleValue())) {
							minOutsidePerimeterElevation = neighborElevation;
							minInsidePerimeterElevation = currentElevation;
							spilloverElevation = neighborElevation.max(currentElevation);
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
//		Log.e("Pit Creation, current pit == overflow pit?", Boolean.toString(this.pitID==this.pitIdOverflowingInto)+ " " + Integer.toString(this.pitID));
//		Log.w("Pit-minOutsidePerimeterElevation", minOutsidePerimeterElevation.toString());
//		Log.w("Pit-minInsidePerimeterElevation", minInsidePerimeterElevation.toString());
//		Log.w("Pit-spilloverElevation", spilloverElevation.toString());
//		Log.w("Pit-pitOutletPoint", pitOutletPoint.toString());
//		Log.w("Pit-outletSpilloverFlowDirection", outletSpilloverFlowDirection.toString());
//		Log.w("Pit-allindices", allPointsList.toString());
//		Log.w("Pit-pitIdOverflowingInto", Integer.toString(pitIdOverflowingInto));
		
		// Volume/elevation-dependent variables and calculations
		retentionVolume = new BigDecimal(0);
		cellCountToBeFilled = 0;
		for (int listIdx = 0; listIdx < allPointsList.size(); listIdx++) {
			Point currentPoint = new Point(allPointsList.get(listIdx));
			int r = currentPoint.y;
			int c = currentPoint.x;
			if (DEM[r][c] < spilloverElevation.doubleValue()) {
				retentionVolume = retentionVolume.add(spilloverElevation.subtract(new BigDecimal(DEM[r][c]), MathContext.DECIMAL64).multiply(cellSize.pow(2, MathContext.DECIMAL64), MathContext.DECIMAL64), MathContext.DECIMAL64);
				cellCountToBeFilled = cellCountToBeFilled + 1;
			}
		}
		filledVolume = new BigDecimal(0);
		pitDrainageRate = new BigDecimal(0);
		netAccumulationRate = (rainfallIntensity.multiply(areaCellCount, MathContext.DECIMAL64).multiply(cellSize.pow(2, MathContext.DECIMAL64), MathContext.DECIMAL64)).subtract(pitDrainageRate, MathContext.DECIMAL64); //cubic meters per hour
		spilloverTime = retentionVolume.divide(netAccumulationRate, MathContext.DECIMAL64); //hours
//		Log.w("Pit-netAccumulationRate", netAccumulationRate.toString());
//		Log.w("Pit-spilloverTime", spilloverTime.toString());
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
		
		if (spilloverTime.doubleValue() > f.spilloverTime.doubleValue()) {
			return 1;
		}
		else if (spilloverTime.doubleValue() < f.spilloverTime.doubleValue()) {
			return -1;
		}
		else {
			return 0;
		}
	}

}
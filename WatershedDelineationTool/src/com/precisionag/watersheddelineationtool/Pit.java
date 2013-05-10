package com.precisionag.watersheddelineationtool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.Color;

public class Pit {

	double[][] DEM;
	double[][] flowDirection;
	double[][] drainage;
	int[][] pits;
	int cellSize;
	double rainfallIntensity;

	// single pit cell variables
	int pitID;
	double pitBottomElevation;
	int pitBottomIndex;
	Color pitColor;
	// Whole pit depression variables
	List<Integer> allPitIndicesList;
	int areaCellCount;
	// Border-dependent variables and calculations
	List<Integer> pitBorderIndicesList;
	double minOutsidePerimeterElevation; // Cells
	double minInsidePerimeterElevation;
	double overflowPointElevation;
	int spilloverPitID;
	int pitOutletIndex; // in the pit (corresponds to point where #3 occurs)
	double outletSpilloverFlowDirection; 
	// Volume/elevation-dependent variables and calculations
	double retentionVolume;
	double filledVolume;
	double spilloverTime;
	int cellCountToBeFilled; // at spillover, the area of inundated cells (# of cells)	
	double pitDrainageRate;
	double netAccumulationRate;



	// constructor method
	@SuppressWarnings("static-access")
	public Pit(double[][] inputDrainage, int inputCellSize, double[][] inputDEM, double[][] inputFlowDirection, int[][] inputPits, int pitIndex, int inputPitID, double inputRainfallIntensity) {

		cellSize = inputCellSize;
		rainfallIntensity = inputRainfallIntensity;
		DEM = inputDEM;
		flowDirection = inputFlowDirection;
		pits = inputPits;
		drainage = inputDrainage;
		int rowSize = DEM[0].length;
		int columnSize = DEM.length;	

		// single pit cell variables
		pitID = inputPitID;
		pitBottomElevation = DEM[linearToTwoDIndexing(pitIndex, rowSize)[0]][linearToTwoDIndexing(pitIndex, rowSize)[0]];
		pitBottomIndex = pitIndex;
		// assign the pit a random color
		Random random = new Random();
		int red = random.nextInt(255);
		int blue = random.nextInt(255);
		int green = random.nextInt(255);
		pitColor.rgb(red,green,blue);
		// Whole pit depression variables
		allPitIndicesList = findCellsDrainingToPoint(pitIndex, rowSize, columnSize, flowDirection, allPitIndicesList);
		areaCellCount = allPitIndicesList.size();
//		// Border-dependent variables and calculations
		pitBorderIndicesList = allPitIndicesList;
		double spilloverElevation = Double.NaN;
		for (int listIdx = 0; listIdx < allPitIndicesList.size(); listIdx++) {
			int currentCellIndex = allPitIndicesList.get(listIdx);
			int r = linearToTwoDIndexing(currentCellIndex, rowSize)[0];
			int c = linearToTwoDIndexing(currentCellIndex, rowSize)[1];
			boolean onBorder = true;
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;}
					if (r+y > rowSize || r+y < 1 || c+x > columnSize || c+x < 1) {
						continue;}
					if (pits[r+y][c+x] != pits[r][c]) {
						double currentElevation = DEM[r][c];
						double neighborElevation = DEM[r+y][c+x];
						onBorder = true;
						if (Double.isNaN(spilloverElevation) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
							minOutsidePerimeterElevation = neighborElevation;
							minInsidePerimeterElevation = currentElevation;
							spilloverElevation = Math.max(neighborElevation, currentElevation);
							pitOutletIndex = currentCellIndex;
							double angle = Math.atan2(y,x);
							outletSpilloverFlowDirection = angle % 2*Math.PI;
							spilloverPitID = pits[r+y][c+x];
						}
					}
				}
			}
			if (onBorder == false) {
				pitBorderIndicesList.remove(currentCellIndex);
			}
		}
		// Volume/elevation-dependent variables and calculations
		retentionVolume = 0;
		cellCountToBeFilled = 0;
		for (int listIdx = 0; listIdx < allPitIndicesList.size(); listIdx++) {
			int currentCellIndex = allPitIndicesList.get(listIdx);
			int r = linearToTwoDIndexing(currentCellIndex, rowSize)[0];
			int c = linearToTwoDIndexing(currentCellIndex, rowSize)[1];
			if (DEM[r][c] < spilloverElevation) {
				retentionVolume = retentionVolume + ((spilloverElevation - DEM[r][c])*cellSize);
				cellCountToBeFilled = cellCountToBeFilled + 1;
			}
		}
		filledVolume = 0;
		pitDrainageRate = 0;
		netAccumulationRate = (rainfallIntensity*areaCellCount) - pitDrainageRate;
		spilloverTime = retentionVolume/((cellSize^2)*netAccumulationRate);

	}
	public List<Integer> findCellsDrainingToPoint(int index, int rowSize, int columnSize, double[][] flowDirection, List<Integer> indicesDrainingToIndex) {
		indicesDrainingToIndex.add(index);
		int r = linearToTwoDIndexing(index, rowSize)[0];
		int c = linearToTwoDIndexing(index, rowSize)[1];
		for (int x = -1; x < 2; x++) {
			for (int y = -1; y < 2; y++){
				if (x == 0 && y == 0) {
					continue;}
				if (r+y > rowSize || r+y < 1 || c+x > columnSize || c+x < 1) {
					continue;}
				double angle = Math.atan2(y,x);
				if (flowDirection[r+y][c+x] == angle % 2*Math.PI) {
					int neighborIndex = twoDToLinearIndexing(r+y, c+x, rowSize);
					indicesDrainingToIndex.addAll(findCellsDrainingToPoint(neighborIndex, rowSize, columnSize, flowDirection, indicesDrainingToIndex));
				}
			}
		}
		return indicesDrainingToIndex;
	}

//	public List<Integer> findPitBorderData(double[][] DEM, int[][] pits, List<Integer> allPitIndicesList, int rowSize, int columnSize) {
//		List<Integer> pitBorderIndicesList = new ArrayList<Integer>();
//		pitBorderIndicesList = allPitIndicesList;
//		double spilloverElevation = Double.NaN;
//		for (int listIdx = 0; listIdx < allPitIndicesList.size(); listIdx++) {
//			int currentCellIndex = allPitIndicesList.get(listIdx);
//			int r = linearToTwoDIndexing(currentCellIndex, rowSize)[0];
//			int c = linearToTwoDIndexing(currentCellIndex, rowSize)[1];
//			boolean onBorder = true;
//			for (int x = -1; x < 2; x++) {
//				for (int y = -1; y < 2; y++){
//					if (x == 0 && y == 0) {
//						continue;}
//					if (r+y > rowSize || r+y < 1 || c+x > columnSize || c+x < 1) {
//						continue;}
//					if (pits[r+y][c+x] != pits[r][c]) {
//						double currentElevation = DEM[r][c];
//						double neighborElevation = DEM[r+y][c+x];
//						onBorder = true;
//						if (Double.isNaN(spilloverElevation) || (currentElevation <= spilloverElevation && neighborElevation <= spilloverElevation)) {
//							minOutsidePerimeterElevation = neighborElevation;
//							minInsidePerimeterElevation = currentElevation;
//							spilloverElevation = Math.max(neighborElevation, currentElevation);
//							pitOutletIndex = currentCellIndex;
//							double angle = Math.atan2(y,x);
//							outletSpilloverFlowDirection = angle % 2*Math.PI;
//							spilloverPitID = pits[r+y][c+x];
//						}
//					}
//				}
//			}
//			if (onBorder == false) {
//				pitBorderIndicesList.remove(currentCellIndex);
//			}
//		}
//	}
//	public int computeCellCountToBeFilled(double cellSize, int rowSize, double[][] DEM, double spilloverElevation, List<Integer> allPitIndicesList) {
//		double retentionVolume = 0;
//		int cellCountToBeFilled = 0;
//		for (int listIdx = 0; listIdx < allPitIndicesList.size(); listIdx++) {
//			int currentCellIndex = allPitIndicesList.get(listIdx);
//			int r = linearToTwoDIndexing(currentCellIndex, rowSize)[0];
//			int c = linearToTwoDIndexing(currentCellIndex, rowSize)[1];
//			if (DEM[r][c] < spilloverElevation) {
//				retentionVolume = retentionVolume + ((spilloverElevation - DEM[r][c])*cellSize);
//				cellCountToBeFilled = cellCountToBeFilled + 1;
//			}
//		}
//		return cellCountToBeFilled;
//	}
	
	public int[] linearToTwoDIndexing(int linearIndex, int numrows) {
		int[] rowcol = new int[2];
		rowcol[0] = linearIndex % numrows; // row index
		rowcol[1] = linearIndex/numrows;   // column index
		return rowcol;
	}
	public int twoDToLinearIndexing(int rowIndex, int columnIndex, int numrows) {
		int Index = (columnIndex*numrows) + rowIndex;

		return Index;
	}
}
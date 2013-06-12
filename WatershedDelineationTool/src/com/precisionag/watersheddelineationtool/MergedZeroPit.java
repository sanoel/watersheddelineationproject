package com.precisionag.watersheddelineationtool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.util.Pair;

public class MergedZeroPit {
	// single pit cell variables
	int pitID;
	double pitBottomElevation;
	Point pitBottomIndex;
	int pitColor;
	// Whole pit depression variables
	ArrayList<Point> allPitPointsList;
	int areaCellCount;
	// Border-dependent variables and calculations
	ArrayList<Point> pitBorderIndicesList;
	double minOutsidePerimeterElevation; // Cells
	double minInsidePerimeterElevation;
	double overflowPointElevation;
	int spilloverPitID;
	Point pitOutletPoint; // in the pit (corresponds to point where #3 occurs)
	Point outletSpilloverFlowDirection; 
	// Volume/elevation-dependent variables and calculations
	double retentionVolume;
	double filledVolume;
	double spilloverTime;
	int cellCountToBeFilled; // at spillover, the area of inundated cells (# of cells)	
	double pitDrainageRate;
	double netAccumulationRate;



	// Constructor method
	@SuppressWarnings("static-access")
	public MergedZeroPit(FilledDataset fillDataset) {
		//Identify The Two Merging Pits
		Pit overflowingPit = fillDataset.fillPits.pitDataList.get(0);
		Pit pitOverflowedInto = fillDataset.fillPits.pitDataList.get(fillDataset.fillPits.getIndexOf(fillDataset.fillPits.pitDataList.get(0).spilloverPitID));
		
		double cellSize = fillDataset.cellSize;
		double rainfallIntensity = fillDataset.rainfallIntensity;
		double[][] DEM = fillDataset.fillDEM;
		FlowDirectionCell[][] flowDirection = fillDataset.fillFlowDirection;
		int[][] pits = fillDataset.fillPits.pitIDMatrix;
		double[][] drainage = fillDataset.fillDrainage;
		int rowSize = DEM[0].length;
		int columnSize = DEM.length;
		
		allPitPointsList = new ArrayList<Point>();
		// single pit cell variables
		pitID = fillDataset.fillPits.getMaximumPitID();
		pitBottomElevation = pitOverflowedInto.pitBottomElevation;
		pitBottomIndex = pitOverflowedInto.pitBottomIndex;
		// New pit takes color of the pit that is being overflowed into
		pitColor = pitOverflowedInto.pitColor;
		// Whole pit depression variables
		allPitPointsList.addAll(pitOverflowedInto.allPitPointsList);
		allPitPointsList.addAll(overflowingPit.allPitPointsList);
		allPitPointsList = findCellsDrainingToPointNew(flowDirection, allPitPointsList);
		areaCellCount = allPitPointsList.size();
		// Border-dependent variables and calculations
		pitBorderIndicesList.addAll(pitOverflowedInto.pitBorderIndicesList);
		pitBorderIndicesList.addAll(overflowingPit.pitBorderIndicesList);
		double spilloverElevation = Double.NaN;
		for (int pitPointsListIdx = 0; pitPointsListIdx < allPitPointsList.size(); pitPointsListIdx++) {
			Point currentPoint = allPitPointsList.get(pitPointsListIdx);
			int r = currentPoint.y;
			int c = currentPoint.x;
			boolean onBorder = false;
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
							pitOutletPoint = currentPoint;
							outletSpilloverFlowDirection = new Point(c+x, r+y);
							spilloverPitID = pits[r+y][c+x];
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
		for (int listIdx = 0; listIdx < allPitPointsList.size(); listIdx++) {
			Point currentPoint = allPitPointsList.get(listIdx);
			int r = currentPoint.y;
			int c = currentPoint.x;
			if (DEM[r][c] < spilloverElevation) {
				retentionVolume = retentionVolume + ((spilloverElevation - DEM[r][c])*cellSize);
				cellCountToBeFilled = cellCountToBeFilled + 1;
			}
		}
		//Sum the drainage taking place in the pit
		for (int listIdx = 0; listIdx < allPitPointsList.size(); listIdx++) {
			Point currentPoint = allPitPointsList.get(listIdx);
			int r = currentPoint.y;
			int c = currentPoint.x;
			pitDrainageRate = pitDrainageRate + drainage[r][c];
		}
		netAccumulationRate = (rainfallIntensity*areaCellCount) - pitDrainageRate;
		spilloverTime = retentionVolume/(Math.pow(cellSize,2)*netAccumulationRate);
	}
	
	public ArrayList<Point> findCellsDrainingToPointNew(FlowDirectionCell[][] flowDirection, ArrayList<Point> indicesDrainingToPit) {
		int i = 0;
		while (i < indicesDrainingToPit.size()) {
			int currentRow = indicesDrainingToPit.get(i).y;
			int currentColumn = indicesDrainingToPit.get(i).x;
			if (flowDirection[currentRow][currentColumn].parentList.isEmpty()){
				i++;
				continue;
			}
			for (int parentIdx = 0; parentIdx < flowDirection[currentRow][currentColumn].parentList.size(); parentIdx++) {
				indicesDrainingToPit.add(flowDirection[currentRow][currentColumn].parentList.get(parentIdx));
			}
			i++;
		}
		return indicesDrainingToPit;
	}
}
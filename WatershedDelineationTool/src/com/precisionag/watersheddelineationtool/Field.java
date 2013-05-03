package com.precisionag.watersheddelineationtool;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class Field {
	// constructor method
	public Field() {
	}

	
	// Flow Direction
	public double[][] computeFlowDirection(double[][] DEM, double[][] drainage,
			double intensity) {
		int width = DEM.length - 2;
		int height = DEM[0].length - 2;
		double[][] flowDirection = new double[width][height];
		for (int r = 0; r < width; r++) {
			for (int c = 0; c < height; c++) {

				// If the drainage rate is greater than the accumulation rate
				// then the cell is a pit.
				if (drainage[r + 1][c + 1] >= intensity) {
					flowDirection[r][c] = -2;
					continue;
				}
				
				Double minimumSlope = Double.NaN;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;}
						double distance = Math.pow((Math.pow(x, 2) + Math.pow(y, 2)),0.5);
						double slope = (DEM[r+1+y][c+1+x] - DEM[r+1][c+1])/distance;
						double angle = Math.atan2(y,x); 
						
						//maintain current minimum slope, minimum slope being the steepest downslope
						if (minimumSlope.isNaN() || slope <= minimumSlope) {
							flowDirection[r][c] = angle % 2*Math.PI;
						}
					}
				}
				if (minimumSlope == 0) {
					flowDirection[r][c] = -4;
				}
				if (minimumSlope >= 0) {
					flowDirection[r][c] = -1;
				}
			}
		}
		return flowDirection;
	}
	
	
	// Flow Accumulation
	public double[][] computeFlowAccumulation(double[][] flowDirection) {
		int width = flowDirection.length - 2;
		int height = flowDirection[0].length - 2;
		double flowAccumulation[][] = new double[width][height];
		return flowAccumulation;
	}
	
	// Pit 
	public double[][] computePits(double[][] flowDirection) {
		int width = flowDirection.length - 2;
		int height = flowDirection[0].length - 2;
		
		int PIT_BOTTOM_ELEVATION = 1;
		int MIN_OUTSIDE_EDGE_ELEVATION = 2;
		int MIN_INSIDE_EDGE_ELEVATION = 3;
		int SPILLOVER_ELEVATION = 4;
		int AREA_CELL_COUNT = 5;
		int CELLS_TO_BE_FILLED_COUNT = 6; // at spillover, the area of inundated cells (# of cells)
		int VOLUME = 7;
		int PIT_OUTLET_INDEX = 8; // in the pit (corresponds to point where #3 occurs)
		int OUTLET_SPILLOVER_FLOW_DIRECTION = 9; 
		int SPILLOVER_TIME = 10;
		int SPILLOVER_PIT_ID = 11;
		int PIT_ID = 12;
		int PIT_BOTTOM_INDEX = 13;
		int FILLED_VOLUME = 14;
		int COLOR = 15;
		int ALL_INDICES = 16;
		int BORDER_INDICES = 17;
		int NET_ACCUMULATION_RATE = 18;
		
		double pits[][] = new double[width][height];
		double pitData[][] = new double[width][18];
		
		int currentPitID = 1;
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				if (flowDirection[r][c] < 0) {
					List<Integer> listOfPitIndices = new ArrayList<Integer>();
					listOfPitIndices.add((c*height) + r);
					pitData[currentPitID][ALL_INDICES] = ;
					
				}
			}
		}
		
		return flowDirection;
	}
}
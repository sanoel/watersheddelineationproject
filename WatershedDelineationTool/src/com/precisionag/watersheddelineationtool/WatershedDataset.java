package com.precisionag.watersheddelineationtool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;

import android.graphics.Point;

public class WatershedDataset {
	FlowDirectionCell[][] flowDirection;
	double[][] DEM;
	double[][] drainage;
	PitRaster pits;
	BigDecimal cellSize;
	BigDecimal rainfallDuration;
	BigDecimal rainfallDepth;
	BigDecimal rainfallIntensity;
	
	// Constructor
	public WatershedDataset(double[][] inputDEM, double[][] inputDrainage, BigDecimal inputCellSize, BigDecimal inputRainfallDuration, BigDecimal inputRainfallDepth) {
		DEM = inputDEM;
		drainage = inputDrainage;
		cellSize = inputCellSize;
		rainfallDuration = inputRainfallDuration;
		rainfallDepth = inputRainfallDepth;
		rainfallIntensity = rainfallDepth.divide(rainfallDuration, MathContext.DECIMAL64);
		
		// Compute Flow Direction
		int numrows = DEM.length;
		int numcols = DEM[0].length;
		flowDirection = new FlowDirectionCell[numrows][numcols];
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				Point childPoint = null;

				// If the cell is along the border then it should remain a null
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
					flowDirection[r][c] = flowDirectionCell;
					continue;
				}

				// If the drainage rate is greater than the accumulation rate
				// then the cell is a pit.
				//					if (drainage[r][c] >= rainfallIntensity) {
				//						FlowDirectionCell flowDirectionCell = new FlowDirectionCell(-2,-2);
				//						flowDirectionCellMatrix[r][c] = flowDirectionCell;					
				//						continue;
				//					}

				double minimumSlope = Double.NaN;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						double distance = Math.pow((Math.pow(x, 2) + Math.pow(y, 2)),0.5);
						double slope = (DEM[r+y][c+x] - DEM[r][c])/distance;
						//							double angle = Math.atan2(y,x); 

						//maintain current minimum slope, minimum slope being the steepest downslope
						if (Double.isNaN(minimumSlope) || slope <= minimumSlope) {
							minimumSlope = slope;
							childPoint = new Point(c+x, r+y);
							FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
							flowDirection[r][c] = flowDirectionCell;
						}
					}
				}				

				// Identification of Pits with undefined flow direction
				if (minimumSlope >= 0) {
					childPoint = new Point(-1, -1);
					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);					
					flowDirection[r][c] = flowDirectionCell;				
				}
			}
		}

		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				Point currentPoint = new Point(c, r);
				ArrayList<Point> parentList = new ArrayList<Point>();				
				// If the drainage rate is greater than the accumulation rate
				// then the cell is a pit.
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					continue;
				}
				// Find all cells pointing to current cell. This comes after assuring that the current cell isn't on the border.
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;}
						if (r+y == numrows-1 || r+y == 0 || c+x == numcols-1 || c+x == 0) {
							continue;
						}
						if (flowDirection[r+y][c+x].childPoint.x == currentPoint.x && flowDirection[r+y][c+x].childPoint.y == currentPoint.y) {
							Point parentPoint = new Point(c+x, r+y); 
							parentList.add(parentPoint);
						}
					}
				}
				flowDirection[r][c].setParentList(parentList);
			}
		}
		
		// Compute Pits
		pits = new PitRaster(DEM, drainage, flowDirection, cellSize, rainfallIntensity);
	}
}
//Construct DEM
//private double[][] readDEMFile(String DEMName) {
//	AssetManager assetManager = getAssets();
//	double[][] DEM = null;
//	try {
//		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(DEMName + ".asc")));
//		String line = bufferedReader.readLine();
//		String[] lineArray = line.split("\\s+");
//		int numcols = Integer.parseInt(lineArray[1]);
//		line = bufferedReader.readLine();
//		lineArray = line.split("\\s+");
//		int numrows = Integer.parseInt(lineArray[1]);
//		line = bufferedReader.readLine();
//		lineArray = line.split("\\s+");
//		double xLLCorner = Double.parseDouble(lineArray[1]);
//		line = bufferedReader.readLine();
//		lineArray = line.split("\\s+");
//		double yLLCorner = Double.parseDouble(lineArray[1]);
//		line = bufferedReader.readLine();
//		lineArray = line.split("\\s+");
//		double cellSize = Double.parseDouble(lineArray[1]);
//		line = bufferedReader.readLine();
//		lineArray = line.split("\\s+");
//		double NaNValue = Double.parseDouble(lineArray[1]);
//		
//		DEM = new double[numrows][numcols]; 
//		int r = 0;
//		while (( line = bufferedReader.readLine()) != null){
//			lineArray = line.split("\\s+");
//			for (int c = 0; c < numcols; c++) {
//			    DEM[r][c] = Double.parseDouble(lineArray[c]);
//			}
//		r++;
//		}
//	} catch (NumberFormatException e) {
//		e.printStackTrace();
//	} catch (IOException e) {
//		e.printStackTrace();
//	} finally {}
//	return DEM;
//}
////Flow Direction
//public FlowDirectionCell[][] computeFlowDirectionNew(double[][] DEM, double[][] drainage, BigDecimal rainfallIntensity2) {
//	int numrows = DEM.length;
//	int numcols = DEM[0].length;
//	FlowDirectionCell[][] flowDirection = new FlowDirectionCell[numrows][numcols];
//	for (int r = 0; r < numrows; r++) {
//		for (int c = 0; c < numcols; c++) {
//			Point childPoint = null;
//
//			// If the cell is along the border then it should remain a null
//			if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
//				FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
//				flowDirection[r][c] = flowDirectionCell;
//				continue;
//			}
//
//			// If the drainage rate is greater than the accumulation rate
//			// then the cell is a pit.
//			//				if (drainage[r][c] >= rainfallIntensity) {
//			//					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(-2,-2);
//			//					flowDirectionCellMatrix[r][c] = flowDirectionCell;					
//			//					continue;
//			//				}
//
//			double minimumSlope = Double.NaN;
//			for (int x = -1; x < 2; x++) {
//				for (int y = -1; y < 2; y++){
//					if (x == 0 && y == 0) {
//						continue;
//					}
//					double distance = Math.pow((Math.pow(x, 2) + Math.pow(y, 2)),0.5);
//					double slope = (DEM[r+y][c+x] - DEM[r][c])/distance;
//					//						double angle = Math.atan2(y,x); 
//
//					//maintain current minimum slope, minimum slope being the steepest downslope
//					if (Double.isNaN(minimumSlope) || slope <= minimumSlope) {
//						minimumSlope = slope;
//						childPoint = new Point(c+x, r+y);
//						FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
//						flowDirection[r][c] = flowDirectionCell;
//					}
//				}
//			}				
//			// Identification of flat spot "pits" with undefined flow direction
//			//				if (minimumSlope == 0) {
//			//					childPoint = new Point(-4, -4);
//			//					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
//			//					flowDirection[r][c] = flowDirectionCell;
//			//				}
//
//			// Identification of Pits with undefined flow direction
//			if (minimumSlope >= 0) {
//				childPoint = new Point(-1, -1);
//				FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);					
//				flowDirection[r][c] = flowDirectionCell;				
//			}
//		}
//	}
//
//	for (int r = 0; r < numrows; r++) {
//		for (int c = 0; c < numcols; c++) {
//			Point currentPoint = new Point(c, r);
//			ArrayList<Point> parentList = new ArrayList<Point>();				
//			// If the drainage rate is greater than the accumulation rate
//			// then the cell is a pit.
//			if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
//				continue;
//			}
//			// Find all cells pointing to current cell. This comes after assuring that the current cell isn't on the border.
//			for (int x = -1; x < 2; x++) {
//				for (int y = -1; y < 2; y++){
//					if (x == 0 && y == 0) {
//						continue;}
//					if (r+y == numrows-1 || r+y == 0 || c+x == numcols-1 || c+x == 0) {
//						continue;
//					}
//					if (flowDirection[r+y][c+x].childPoint.x == currentPoint.x && flowDirection[r+y][c+x].childPoint.y == currentPoint.y) {
//						Point parentPoint = new Point(c+x, r+y); 
//						parentList.add(parentPoint);
//					}
//				}
//			}
//			flowDirection[r][c].setParentList(parentList);
//		}
//	}
//	return flowDirection;
//}
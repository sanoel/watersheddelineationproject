package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;

public class WatershedDataset {
	FlowDirectionCell[][] flowDirection;
	float[][] originalDem;
	float[][] Dem;
	float[][] drainage;
	PitRaster pits;
	float cellSizeX;
	float cellSizeY;
	static int status = 0;
	static String statusMessage = "Reading DEM";
	public static float noDataVal;
	public static int noDataCellsRemoved = 0;
	public static boolean fillAllPits = true;
	public static int delineatedArea = 0;

	WatershedDatasetListener listener;

	public interface WatershedDatasetListener {
		public void watershedDatasetOnProgress(int progress, String status);
		public void watershedDatasetDone();
	}


	// Constructor
	public WatershedDataset(float[][] inputDem, float inputCellSizeX, float inputCellSizeY, float inputNoDataVal, AsyncTask task) {

		if(task instanceof WatershedDatasetListener) {
			listener = (WatershedDatasetListener) task;
		} else {
			throw new ClassCastException("WatershedDataset - Task must implement WatershedDatasetListener");
		}

		noDataVal = inputNoDataVal;
		drainage = null;
		cellSizeX = inputCellSizeX;
		cellSizeY = inputCellSizeY;
		// Load the DEM
		listener.watershedDatasetOnProgress(status, "Reading DEM");
		Dem = inputDem;
		originalDem = inputDem;
		listener.watershedDatasetOnProgress(status, "Removing NoDATA values from the DEM");
		removeDEMNoData();
		listener.watershedDatasetOnProgress(status, "Discovering Flow Routes");
		// Compute Flow Direction
		computeFlowDirection();

		// Compute Pits
		listener.watershedDatasetOnProgress(status, "Identifying Surface Depressions");
		pits = new PitRaster(Dem, drainage, flowDirection, cellSizeX, cellSizeY, listener);
		listener.watershedDatasetOnProgress(status, "Done");
	}
	
	public void setTask(AsyncTask task){
		if(task instanceof WatershedDatasetListener) {
			listener = (WatershedDatasetListener) task;
		} else {
			throw new ClassCastException("WatershedDataset - Task must implement WatershedDatasetListener");
		}
	}

	private float[][] removeDEMNoData() {
		int numrows = Dem.length;
		int numcols = Dem[0].length;
		noDataCellsRemoved = 0;
		double distance;
		double weight;
		float[][] newDEM = Dem;
		boolean noDataCellsRemaining = true;
		while (noDataCellsRemaining == true) {
			noDataCellsRemaining = false;
			for (int r = 0; r < numrows; r++) {
				for (int c = 0; c < numcols; c++) { 
					if (Dem[r][c] == noDataVal) {
						float weightsum = 0;
						float weightedvalsum = 0;
						for (int x = -2; x < 3; x++) {
							for (int y = -2; y < 3; y++) {
								//skip the current cell
								if (x == 0 && y == 0) {
									continue;
								}
								//verify that the cell is in the DEM range
								if (r+y >= numrows || r+y < 0 || c+x >= numcols || c+x < 0) {
									continue;
								}
								//verify that the neighbor cell is not NoDATA, as this will break the IDW computation
								if (Dem[r+y][c+x] != noDataVal) {
									distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)); 
									weight = 1 / distance;
									weightsum += weight;
									weightedvalsum += Dem[r+y][c+x] * weight;
									newDEM[r][c] = weightedvalsum/weightsum;

								}
							}
						}
						if (newDEM[r][c] == noDataVal) {
							noDataCellsRemaining = true;
						} else {
							noDataCellsRemoved++;
						}
					}
				}
				status = (int) (10 * (((r*numcols))/((double)numrows*numcols)));
			}
		}
		status = 10;
		return newDEM;
	}

	public FlowDirectionCell[][] computeFlowDirection() {
		int numrows = Dem.length;
		int numcols = Dem[0].length;

		flowDirection = new FlowDirectionCell[numrows][numcols];
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				Point childPoint = null;
				// If the cell is along the border then it should remain a null
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					flowDirection[r][c] = new FlowDirectionCell(childPoint);
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
						double distance = Math.sqrt((Math.pow(x, 2) + Math.pow(y, 2)));
						double slope = (Dem[r+y][c+x] - Dem[r][c])/distance;
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
			status = (int) (20 + (10 * (((r*numcols))/((double) numrows*numcols))));
			listener.watershedDatasetOnProgress(status, "Discovering Flow Routes");
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
			status = (int) (30 + (10 * (((r*numcols))/((double)numrows*numcols))));
			listener.watershedDatasetOnProgress(status, "Discovering Flow Routes");
		}
		return flowDirection;
	}

	public void resolveFlowDirectionParents() {
		int numrows = Dem.length;
		int numcols = Dem[0].length;
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {			
				// If the drainage rate is greater than the accumulation rate
				// then the cell is a pit.
				if (r >= numrows-1 || r <= 0 || c >= numcols-1 || c <= 0) {
					continue;
				}
				Point currentPoint = new Point(c, r);
				ArrayList<Point> parentList = new ArrayList<Point>();	
				// Find all cells pointing to current cell. This comes after assuring that the current cell isn't on the border.
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;}
						if (r+y >= numrows-1 || r+y <= 0 || c+x >= numcols-1 || c+x <= 0) {
							continue;
						}
						if (flowDirection[r+y][c+x].childPoint.x == currentPoint.x && flowDirection[r+y][c+x].childPoint.y == currentPoint.y) {
							Point parentPoint = new Point(c+x, r+y); 
							parentList.add(parentPoint);
						}
					}
				}
				MainActivity.watershedDataset.flowDirection[r][c].setParentList(parentList);
			}
		}
	}

	// Wrapper function that simulates the rainfall event to iteratively fill pits to connect the surface until the rainfall event ends
	@SuppressWarnings("unchecked")
	public boolean fillPits() {
		Log.w("Rainfall amount", Double.toString(RainfallSimConfig.rainfallDepth));
		statusMessage = "Filling and Merging Depressions";
		int fillcounter = 0;
		Collections.sort(this.pits.pitDataList);
		int numberOfPits = pits.pitDataList.size();
		while ((pits.pitDataList.get(0).spilloverTime < RainfallSimConfig.rainfallDuration) || (fillAllPits)) {
			mergePits();
			if (this.pits.pitDataList.isEmpty()) {
				// No more pits exist, filling is 100% complete for this simulation
				status = 100;
				break;
			}
			Collections.sort(this.pits.pitDataList);
			fillcounter++;
			// update the filling status
			status = (int) (100 * (fillcounter/(double)numberOfPits));
			listener.watershedDatasetOnProgress(status, "Simulating Rainfall");
		}
		resolveFlowDirectionParents();
		// time has expired for the storm event, filling is 100% complete for this simulation
		status = 100;
		listener.watershedDatasetOnProgress(status, "Finished");
		this.pits.updatePitsBitmap();
		return true;
	}

	// Merge two pits
	public boolean mergePits() {
		Pit firstPit = this.pits.pitDataList.get(0);
		int secondPitID = firstPit.pitIdOverflowingInto; //The pit ID that the first pit overflows into

		// Fill the first pit and resolve flow direction. This must be completed before the new pit entry is created or else retention volumes will be incorrectly calculated (the first pit must be filled).

		// Handle pits merging with other pits
		if (secondPitID != -1) {
			int mergedPitID = this.pits.getMaximumPitID() + 1;
			Pit secondPit = this.pits.pitDataList.get(this.pits.getIndexOf(secondPitID));
			//				int secondPitListIndex = this.pits.getIndexOf(secondPitID);

			// re-ID the two merging pits with their new mergedPitID
			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
				this.pits.pitIDMatrix[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = mergedPitID;
			}
			for (int i = 0; i < secondPit.allPointsList.size(); i++) {
				this.pits.pitIDMatrix[secondPit.allPointsList.get(i).y][secondPit.allPointsList.get(i).x] = mergedPitID;
			}

			resolveFilledArea();

			// Update all pits that will overflow into either of the merging pits as now overflowing into the new mergedPitID
			for (int i = 0; i < this.pits.pitDataList.size(); i++){
				if ((this.pits.pitDataList.get(i).pitIdOverflowingInto == firstPit.pitID) || (this.pits.pitDataList.get(i).pitIdOverflowingInto == secondPitID)) {
					this.pits.pitDataList.get(i).pitIdOverflowingInto = mergedPitID;
				}
			}
			//////////////////////////////////////////////
			// Create the new merged pit entry
			// single pit cell variables
			firstPit.pitID = mergedPitID;
			firstPit.pitBottomElevation = secondPit.pitBottomElevation;
			firstPit.pitBottomPoint = secondPit.pitBottomPoint;
			// New pit takes color of the pit that is being overflowed into
			firstPit.color = secondPit.color;
			// Whole pit depression variables
			firstPit.allPointsList.addAll(secondPit.allPointsList);
			firstPit.areaCellCount = firstPit.allPointsList.size();
			// Border-dependent variables and calculations
			firstPit.pitBorderIndicesList.addAll(secondPit.pitBorderIndicesList);
			firstPit.spilloverElevation = Float.NaN;

			// traverse in reverse order.  some of the border indices will be found to be not on the border and removed from the list using the onBorder variable
			for (int i = firstPit.pitBorderIndicesList.size()-1; i > -1; i--) {
				Point currentPoint = firstPit.pitBorderIndicesList.get(i);
				int r = currentPoint.y;
				int c = currentPoint.x;
				boolean onBorder = false;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;}
						if (this.pits.pitIDMatrix[r+y][c+x] != this.pits.pitIDMatrix[r][c]) {
							double currentElevation = this.Dem[r][c];
							double neighborElevation = this.Dem[r+y][c+x];
							onBorder = true;
							if (Float.isNaN(firstPit.spilloverElevation) || (currentElevation <= firstPit.spilloverElevation && neighborElevation <= firstPit.spilloverElevation)) {
								firstPit.minOutsidePerimeterElevation = neighborElevation;
								firstPit.minInsidePerimeterElevation = currentElevation;
								firstPit.spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
								firstPit.pitOutletPoint = currentPoint;
								firstPit.outletSpilloverFlowDirection = new Point(c+x, r+y);
								firstPit.pitIdOverflowingInto = this.pits.pitIDMatrix[r+y][c+x];
							}
						}
					}
				}
				if (onBorder == false) {
					firstPit.pitBorderIndicesList.remove(currentPoint);
				}
			}

			// Volume/elevation-dependent variables and calculations
			firstPit.filledVolume = firstPit.retentionVolume;
			firstPit.retentionVolume = firstPit.filledVolume;
			firstPit.cellCountToBeFilled = 0;
			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
				Point currentPoint = firstPit.allPointsList.get(i);
				int r = currentPoint.y;
				int c = currentPoint.x;
				if (this.Dem[r][c] < firstPit.spilloverElevation) {
					firstPit.retentionVolume += ((firstPit.spilloverElevation - this.Dem[r][c]) * cellSizeX*cellSizeY);
					firstPit.cellCountToBeFilled = firstPit.cellCountToBeFilled + 1;
				}
			}

			//Sum the drainage taking place in the pit
			firstPit.pitDrainageRate = 0;
			//				for (int listIdx = 0; listIdx < firstPit.allPointsList.size(); listIdx++) {
			//					Point currentPoint = firstPit.allPointsList.get(listIdx);
			//					int r = currentPoint.y;
			//					int c = currentPoint.x;
			//					firstPit.pitDrainageRate = firstPit.pitDrainageRate; // + drainage[r][c]
			//				}
			firstPit.netAccumulationRate = (RainfallSimConfig.rainfallIntensity * firstPit.areaCellCount) - firstPit.pitDrainageRate;
			firstPit.spilloverTime = firstPit.retentionVolume/(cellSizeX*cellSizeY * firstPit.netAccumulationRate);

			// Removed the second pit
			this.pits.pitDataList.remove(secondPit);

			// Handle pits merging with pit ID -1 (areas flowing off the map)
		} else if (secondPitID == -1) {
			// re-ID the filled pit as pit ID -1
			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
				this.pits.pitIDMatrix[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = -1;
			}
			resolveFilledArea();

			// Update all pits that will overflow into the filled pit as now overflowing into pit ID -1
			for (int i = 0; i < this.pits.pitDataList.size(); i++){
				if (this.pits.pitDataList.get(i).pitIdOverflowingInto == firstPit.pitID) {
					this.pits.pitDataList.get(i).pitIdOverflowingInto = -1;
				}
			}
			this.pits.pitDataList.remove(0);
		}
		return true;	
	}

	public boolean resolveFilledArea() {
		Pit firstPit = this.pits.pitDataList.get(0);
		List<Point> allPointsToResolve = new ArrayList<Point>();
		List<Point> pointsToResolve = new ArrayList<Point>();
		// Adjust DEM elevations
		for (int i = 0; i < firstPit.allPointsList.size(); i++) {
			if (this.Dem[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] < firstPit.spilloverElevation) {
				this.Dem[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = firstPit.spilloverElevation;
				Point pointToResolve = firstPit.allPointsList.get(i);
//				Point pointToResolve = new Point(firstPit.allPointsList.get(i).x, firstPit.allPointsList.get(i).y);
				allPointsToResolve.add(pointToResolve);
				//add each cell to a single flat flow direction cell object
			}
		}

		// Resolve flow direction to direct flow out of the pit
		this.flowDirection[firstPit.pitOutletPoint.y][firstPit.pitOutletPoint.x] = new FlowDirectionCell(firstPit.outletSpilloverFlowDirection);
		pointsToResolve.add(firstPit.pitOutletPoint);
		while (!pointsToResolve.isEmpty()) {
			Point currentPoint = pointsToResolve.get(0);
			pointsToResolve.remove(0);
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					Point neighborPoint = new Point(currentPoint.x + x, currentPoint.y + y);
					// check if the point is part of the complete list to be resolved, but not already on the "next up" list
					if (allPointsToResolve.contains(neighborPoint) && !pointsToResolve.contains(neighborPoint)) {
						this.flowDirection[neighborPoint.y][neighborPoint.x].childPoint = currentPoint;
						pointsToResolve.add(neighborPoint);
						allPointsToResolve.remove(neighborPoint);
					}
				}			
			}

		}
		return true;
	}




	public Bitmap delineate(Point point) {
		delineatedArea = 1;
		int numrows = Dem.length;
		int numcols = Dem[0].length;
		int[] colorarray = new int[this.pits.pitIDMatrix.length*this.pits.pitIDMatrix[0].length];
		Arrays.fill(colorarray, Color.TRANSPARENT);
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		Bitmap delinBitmap = Bitmap.createBitmap(colorarray, numcols, numrows, config);
		delinBitmap = delinBitmap.copy(config, true);
		List<Point> indicesToCheck = new ArrayList<Point>();
		indicesToCheck.add(point);
		//		for (int x = -4; x < 5; x++) {
		//			for (int y = -4; y < 5; y++) {
		//				indicesToCheck.add(new Point(x+point.x, y + point.y));
		//			}
		//		}
		while (!indicesToCheck.isEmpty()) {
//			System.out.println("delineatefunction while list size" + Integer.toString(indicesToCheck.size()));
			
			int r = indicesToCheck.get(0).y;
			int c = indicesToCheck.get(0).x;
//			System.out.println("r="+Integer.toString(r)+ " c=" + Integer.toString(c));
			delinBitmap.setPixel(numcols - 1 - c, r, Color.RED);
			indicesToCheck.remove(0);
			delineatedArea++;
			if (flowDirection[r][c].parentList.isEmpty()) {
				continue;
			}
			for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
				if (delinBitmap.getPixel(numcols - 1 - flowDirection[r][c].parentList.get(i).x, flowDirection[r][c].parentList.get(i).y) == Color.RED) {
//					Log.w("found repeat", flowDirection[r][c].parentList.get(i).toString());
					continue;
				}
				indicesToCheck.add(flowDirection[r][c].parentList.get(i));
			}
		}
		return delinBitmap;
	}

	public void circulars() {
		int numrows = Dem.length;
		int numcols = Dem[0].length;
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				if (r >= numrows-1 || r <= 0 || c >= numcols-1 || c <= 0) {
					continue;
				}
				Point currentPoint = new Point(r,c);
				if (flowDirection[r][c].childPoint == null) {
//					Log.w("A. null found at", currentPoint.toString());
					continue;
				}
				if (flowDirection[r][c].childPoint.y == -1) {
//					Log.w("B. pit bottom found at", currentPoint.toString());
					continue;
				}
				if (flowDirection[flowDirection[r][c].childPoint.y][flowDirection[r][c].childPoint.x].childPoint == null) {
					if (flowDirection[r][c].childPoint.y >= numrows-1 || flowDirection[r][c].childPoint.y <= 0 || flowDirection[r][c].childPoint.x >= numcols-1 || flowDirection[r][c].childPoint.x <= 0) {
						continue;
					} 
//					Log.w("C. null found at this points child:", flowDirection[r][c].childPoint.toString());
					continue;
				}
				if (flowDirection[flowDirection[r][c].childPoint.y][flowDirection[r][c].childPoint.x].childPoint.x == -1) {
//					Log.w("D. pit bottom found at", flowDirection[r][c].childPoint.toString());
					continue;
				}
				if (currentPoint == flowDirection[flowDirection[r][c].childPoint.y][flowDirection[r][c].childPoint.x].childPoint) {
//					Log.w("E. Point to eachother", " current point=" + currentPoint.toString() + ", current child's child " +  flowDirection[flowDirection[r][c].childPoint.y][flowDirection[r][c].childPoint.x].childPoint.toString());
				}
			}
		}	
	}
}
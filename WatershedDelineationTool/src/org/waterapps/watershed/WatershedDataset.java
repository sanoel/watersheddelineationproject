package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.ibm.util.CoordinateConversion;
import com.tiffdecoder.TiffDecoder;

public class WatershedDataset {
	FlowDirectionCell[][] flowDirection;
	float[][] originalDem;
	float[][] dem;
	float[][] drainage;
	public PitRaster pits;
	float cellSizeX;
	float cellSizeY;
	static int status = 0;
	static String statusMessage = "Reading DEM";
	public static float noDataVal;
//	public static int noDataCellsRemoved = 0;
	public static boolean fillAllPits = false;
	public static int delineatedArea = 0;
	public static double delineatedStorageVolume;
//	public ArrayList<LatLng> delineationCoords = new ArrayList<LatLng>();
	WatershedDatasetListener listener;
	DelineationListener delineationListener;
	

	public interface WatershedDatasetListener {
		public void watershedDatasetOnProgress(int progress, String status, Bitmap bitmaps);
		public void watershedDatasetDone();
	}

	public interface DelineationListener {
		public void delineationOnProgress(Bitmap bitmap);
		public void delineationDone();
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
		listener.watershedDatasetOnProgress(status, "Reading DEM", null);
		originalDem = inputDem;
		dem = new float[originalDem.length][originalDem[0].length];
		for (int r = 0; r < originalDem.length; r++) {
			for (int c = 0; c < originalDem[0].length; c ++) {
				dem[r][c] = originalDem[r][c];
			}
		}

		//////////////////// Manual DEM entry	
		//		int x = 0;
		//		float[] test_data = new float[]{200.0f, 200.0f, 200.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 170.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 150.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 170.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 130.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 90.0f,  200.0f, 200.0f,
		//										200.0f, 200.0f, 140.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 130.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 90.0f,  200.0f, 200.0f,
		//										200.0f, 200.0f, 150.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 100.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 60.0f,  200.0f, 200.0f,
		//										200.0f, 200.0f, 130.0f, 200.0f, 200.0f,
		//										200.0f, 200.0f, 70.0f,  200.0f, 200.0f};
		//		for (int r = 0; r < 14; r++) {
		//			for (int c = 0; c < 5; c++) { 
		//				Dem[r][c] = test_data[x];
		//				x++;
		//			}
		//		}
		///////////////////////


		listener.watershedDatasetOnProgress(status, "Removing NoDATA values from the DEM", null);
		removeDemNoData();
		listener.watershedDatasetOnProgress(status, "Discovering Flow Routes", null);
		// Compute Flow Direction
		int pitCellCount = computeFlowDirection();		
		// Compute Pits
		listener.watershedDatasetOnProgress(status, "Identifying Surface Depressions", null);
		pits = new PitRaster(dem, drainage, flowDirection, cellSizeX, cellSizeY, listener);
		pits.constructPitRaster(pitCellCount);
		listener.watershedDatasetOnProgress(status, "Done", null);
	}

	public void recalculatePitsForNewRainfall() {
		MainActivity.simulateButton.setEnabled(false);
		for (int i=0; i < this.pits.pitDataList.size(); i++) {
			if (this.pits.pitDataList.get(i).pitId < 0) {
				continue;
			}
			this.pits.pitDataList.get(i).netAccumulationRate = (RainfallSimConfig.rainfallIntensity * this.pits.pitDataList.get(i).allPointsList.size() * cellSizeX * cellSizeY);
			this.pits.pitDataList.get(i).spilloverTime = this.pits.pitDataList.get(i).retentionVolume / this.pits.pitDataList.get(i).netAccumulationRate;
		}
		MainActivity.simulateButton.setEnabled(true);
	}

	public void setTask(AsyncTask task){
		if(task instanceof WatershedDatasetListener) {
			listener = (WatershedDatasetListener) task;
		} else {
			throw new ClassCastException("WatershedDataset - Task must implement WatershedDatasetListener");
		}
	}

	private float[][] removeDemNoData() {
		int numrows = dem.length;
		int numcols = dem[0].length;
		double distance;
		double weight;
		float[][] newDEM = dem;
		boolean noDataCellsRemaining = true;
		while (noDataCellsRemaining == true) {
			noDataCellsRemaining = false;
			for (int r = 0; r < numrows; r++) {
				for (int c = 0; c < numcols; c++) { 
					if (dem[r][c] == noDataVal) {
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
								if (dem[r+y][c+x] != noDataVal) {
									distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)); 
									weight = 1 / distance;
									weightsum += weight;
									weightedvalsum += dem[r+y][c+x] * weight;
									newDEM[r][c] = weightedvalsum/weightsum;

								}
							}
						}
						if (newDEM[r][c] == noDataVal) {
							noDataCellsRemaining = true;
						}
					}
				}
				status = (int) (10 * (((r*numcols))/((double)numrows*numcols)));
			}
		}
		status = 10;
		return newDEM;
	}

	public int computeFlowDirection() {
		int numrows = dem.length;
		int numcols = dem[0].length;
		int pitCellCount = 0;

		flowDirection = new FlowDirectionCell[numrows][numcols];
		for (int c = 0; c < numcols; c++) {
			for (int r = 0; r < numrows; r++) {
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
						double slope = (dem[r+y][c+x] - dem[r][c])/distance;
						//maintain current minimum slope, minimum slope being the steepest downslope
						if (Double.isNaN(minimumSlope) || slope <= minimumSlope) {
							minimumSlope = slope;
							childPoint = new Point(c+x, r+y);
							flowDirection[r][c] = new FlowDirectionCell(childPoint);
						}
					}
				}				

				// Identification of pit cells (no downslope available) as (-1, -1) flow direction childpoint 
				if (minimumSlope >= 0) {
					pitCellCount++;
					childPoint = new Point(-1, -1);
					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);					
					flowDirection[r][c] = flowDirectionCell;				
				}				
			}
			status = (int) (20 + (10 * (((c*numrows))/((double) numrows*numcols))));
			listener.watershedDatasetOnProgress(status, "Discovering Flow Routes", null);
		}

		// Now go back through and also build a list of parents so the tree structure can be traversed either way.
		// Edge pixels may have parents, but lack the neighbors to determine a valid flow direction (child). 
		for (int c = 0; c < numcols; c++) {
			for (int r = 0; r < numrows; r++) {
				Point currentPoint = new Point(c, r);
				ArrayList<Point> parentList = new ArrayList<Point>();				
				// Find all cells pointing to current cell.
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						if (r+y > numrows-1 || r+y < 0 || c+x > numcols-1 || c+x < 0) {
							continue;
						}
						if (flowDirection[r+y][c+x].childPoint != null) {
							if (flowDirection[r+y][c+x].childPoint.x == currentPoint.x && flowDirection[r+y][c+x].childPoint.y == currentPoint.y) { //apparently, this is not the same thing as "flowDirection[r+y][c+x].childPoint == currentPoint"; perhaps checking == for two points doesn't work
								parentList.add(new Point(c+x, r+y));
							}
						}
					}
				}
				flowDirection[r][c].setParentList(parentList);
			}
			status = (int) (30 + (10 * (((c*numrows))/((double)numrows*numcols))));
			listener.watershedDatasetOnProgress(status, "Discovering Flow Routes", null);
		}
		return pitCellCount;
	}

	// 
	public void resolveFlowDirectionParents() {
		int numrows = dem.length;
		int numcols = dem[0].length;
		for (int c = 0; c < numcols; c++) {	
			for (int r = 0; r < numrows; r++) {
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
				this.flowDirection[r][c].setParentList(parentList);
			}
		}
	}

	// Wrapper function that simulates the rainfall event to iteratively fill pits to connect the surface until the rainfall event ends
	@SuppressWarnings("unchecked")
	public boolean fillPits() {
		statusMessage = "Filling and Merging Depressions";
		int fill_counter = 0;
		Collections.sort(this.pits.pitDataList);
		int numberOfPits = pits.pitDataList.size();
		while ((this.pits.pitDataList.get(0).spilloverTime < RainfallSimConfig.rainfallDuration) || (fillAllPits)) {
			mergePits();
			if (this.pits.pitDataList.isEmpty()) {
				// No more pits exist, filling is 100% complete for this simulation
				status = 100;
				break;
			}
			Collections.sort(this.pits.pitDataList);

			fill_counter++;
			status = (int) (100 * (fill_counter/(double)numberOfPits));

			// update the filling status
			if (fill_counter/10 != 0) {
				//                listener.watershedDatasetOnProgress(status, "Simulating Rainfall", null);
				listener.watershedDatasetOnProgress(status, "Simulating Rainfall", this.pits.pitsBitmap);
			} else {
				listener.watershedDatasetOnProgress(status, "Simulating Rainfall", null);
			}
		}
		resolveFlowDirectionParents();
		// time has expired for the storm event, filling is 100% complete for this simulation
		status = 100;
		listener.watershedDatasetOnProgress(status, "Finished", this.pits.pitsBitmap);
		drawPuddles();
		return true;
	}

	// Merge two pits
	public boolean mergePits() {
		Pit firstPit = this.pits.pitDataList.get(0);
		int secondPitId = firstPit.pitIdOverflowingInto; //The pit ID that the first pit overflows into

		// Handle pits merging with other pits
		if (secondPitId > -1) {
			int mergedPitID = this.pits.maxPitId++;
			Pit secondPit = this.pits.pitDataList.get(this.pits.getIndexOf(secondPitId));

			// re-ID the two merging pits with their new mergedPitID
			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
				this.pits.pitIdMatrix[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = mergedPitID;
				this.pits.pitsBitmap.setPixel(this.dem[0].length - 1 - firstPit.allPointsList.get(i).x, firstPit.allPointsList.get(i).y, secondPit.color);
			}
			for (int i = 0; i < secondPit.allPointsList.size(); i++) {
				this.pits.pitIdMatrix[secondPit.allPointsList.get(i).y][secondPit.allPointsList.get(i).x] = mergedPitID;
			}

			// Fill the first pit and resolve flow direction. This must be completed before the new pit entry is created or else retention volumes will be incorrectly calculated (the first pit must be filled).
			resolveFilledArea();

			// Update all pits that will overflow into either of the merging pits as now overflowing into the new mergedPitID
			for (int i = 0; i < this.pits.pitDataList.size(); i++){
				if ((this.pits.pitDataList.get(i).pitIdOverflowingInto == firstPit.pitId) || (this.pits.pitDataList.get(i).pitIdOverflowingInto == secondPitId)) {
					this.pits.pitDataList.get(i).pitIdOverflowingInto = mergedPitID;
				}
			}
			// Rather than create the new merged pit entry, overwrite the second pit with the new merged pit data
			firstPit.pitId = mergedPitID;
			firstPit.color = secondPit.color;
			firstPit.allPointsList.addAll(0, secondPit.allPointsList); //put the second pit's indices at the beginning of the list (so that the pit bottom is always the 0th item)
			firstPit.pitBorderIndicesList.addAll(0, secondPit.pitBorderIndicesList);
			firstPit.spilloverElevation = Float.NaN;

			// traverse in reverse order.  some of the border indices will be found to be not on the border and removed from the list (necessitating the onBorder variable)
			for (int i = firstPit.pitBorderIndicesList.size()-1; i > -1; i--) {
				Point currentPoint = firstPit.pitBorderIndicesList.get(i);
				int r = currentPoint.y;
				int c = currentPoint.x;
				boolean onBorder = false;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}

						if (this.pits.pitIdMatrix[r+y][c+x] != this.pits.pitIdMatrix[r][c]) {
							double currentElevation = this.dem[r][c];
							double neighborElevation = this.dem[r+y][c+x];
							onBorder = true;
							if (Float.isNaN(firstPit.spilloverElevation) || (currentElevation <= firstPit.spilloverElevation && neighborElevation <= firstPit.spilloverElevation)) {
								firstPit.spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
								firstPit.pitOutletPoint = currentPoint;
								firstPit.outletSpilloverFlowDirection = new Point(c+x, r+y);
								firstPit.pitIdOverflowingInto = this.pits.pitIdMatrix[r+y][c+x];
							}
						}
					}
				}
				if (onBorder == false) {
					firstPit.pitBorderIndicesList.remove(currentPoint);
				}
			}

			// Volume/elevation-dependent variables and calculations
			firstPit.filledVolume = secondPit.filledVolume + firstPit.retentionVolume;
			firstPit.retentionVolume = firstPit.filledVolume;
			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
				int r = firstPit.allPointsList.get(i).y;
				int c = firstPit.allPointsList.get(i).x;
				if (this.dem[r][c] < firstPit.spilloverElevation) {
					firstPit.retentionVolume += ((firstPit.spilloverElevation - this.dem[r][c]) * cellSizeX * cellSizeY);
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
			firstPit.netAccumulationRate = (RainfallSimConfig.rainfallIntensity * firstPit.allPointsList.size() * cellSizeX * cellSizeY) - firstPit.pitDrainageRate;
			firstPit.spilloverTime = firstPit.retentionVolume/firstPit.netAccumulationRate;

			// Removed the second pit
			this.pits.pitDataList.remove(secondPit);

			// Handle pits that begin to run off the DEM
		} else if (secondPitId <= -1) {
			int mergedPitId = this.pits.minPitId--;
			Pit secondPit = this.pits.pitDataList.get(this.pits.getIndexOf(secondPitId));

			// re-ID the two merging pits with their new mergedPitID
			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
				this.pits.pitIdMatrix[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = mergedPitId;
				this.pits.pitsBitmap.setPixel(this.dem[0].length - 1 - firstPit.allPointsList.get(i).x, firstPit.allPointsList.get(i).y, secondPit.color);
			}
			for (int i = 0; i < secondPit.allPointsList.size(); i++) {
				this.pits.pitIdMatrix[secondPit.allPointsList.get(i).y][secondPit.allPointsList.get(i).x] = mergedPitId;
			}

			// Fill the first pit and resolve flow direction. This must be completed before the new pit entry is created or else retention volumes will be incorrectly calculated (the first pit must be filled).
			resolveFilledArea();

			// Update all pits that will overflow into either of the merging pits as now overflowing into the new mergedPitID
			for (int i = 0; i < this.pits.pitDataList.size(); i++){
				if ((this.pits.pitDataList.get(i).pitIdOverflowingInto == firstPit.pitId) || (this.pits.pitDataList.get(i).pitIdOverflowingInto == secondPitId)) {
					this.pits.pitDataList.get(i).pitIdOverflowingInto = mergedPitId;
				}
			}

			// Rather than create the new merged pit entry, overwrite the first pit with the new merged pit data
			firstPit.pitId = mergedPitId;
			firstPit.color = secondPit.color;
			firstPit.allPointsList.addAll(0, secondPit.allPointsList);
			firstPit.pitBorderIndicesList.addAll(0, secondPit.pitBorderIndicesList);
			firstPit.spilloverElevation = Float.NaN;

			// traverse in reverse order.  some of the border indices will be found to be not on the border and removed from the list (hence, the onBorder variable)
			for (int i = firstPit.pitBorderIndicesList.size()-1; i > -1; i--) {
				Point currentPoint = firstPit.pitBorderIndicesList.get(i);
				int r = currentPoint.y;
				int c = currentPoint.x;
				boolean onBorder = false;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						if (currentPoint.y+y >= this.pits.pitIdMatrix.length-1 || currentPoint.y+y <= 0 || currentPoint.x+x >= this.pits.pitIdMatrix[0].length-1 || currentPoint.x+x <= 0) {
							continue;
						}
						if (this.pits.pitIdMatrix[r+y][c+x] != this.pits.pitIdMatrix[r][c] || (r == this.pits.pitIdMatrix.length-1 || r == 0 || c == this.pits.pitIdMatrix[0].length-1 || c == 0)) {
							double currentElevation = this.dem[r][c];
							double neighborElevation = this.dem[r+y][c+x];
							onBorder = true;
							if (Float.isNaN(firstPit.spilloverElevation) || (currentElevation <= firstPit.spilloverElevation && neighborElevation <= firstPit.spilloverElevation)) {
								firstPit.spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
								firstPit.pitOutletPoint = currentPoint;
								firstPit.outletSpilloverFlowDirection = new Point(c+x, r+y);
								firstPit.pitIdOverflowingInto = this.pits.pitIdMatrix[r+y][c+x];
							}
						}
					}
				}
				if (onBorder == false) {
					firstPit.pitBorderIndicesList.remove(currentPoint);
				}
			}

			firstPit.filledVolume = secondPit.filledVolume + firstPit.retentionVolume;
			firstPit.retentionVolume = firstPit.filledVolume;
			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
				int r = firstPit.allPointsList.get(i).y;
				int c = firstPit.allPointsList.get(i).x;
				if (this.dem[r][c] < firstPit.spilloverElevation) {
					firstPit.retentionVolume += ((firstPit.spilloverElevation - this.dem[r][c]) * cellSizeX * cellSizeY);
				}
			}

			//Sum the drainage taking place in the pit
			firstPit.pitDrainageRate = 0.0;
			//				for (int listIdx = 0; listIdx < firstPit.allPointsList.size(); listIdx++) {
			//					Point currentPoint = firstPit.allPointsList.get(listIdx);
			//					int r = currentPoint.y;
			//					int c = currentPoint.x;
			//					firstPit.pitDrainageRate = firstPit.pitDrainageRate; // + drainage[r][c]
			//				}
			firstPit.netAccumulationRate = (RainfallSimConfig.rainfallIntensity * firstPit.allPointsList.size() * cellSizeX * cellSizeY) - firstPit.pitDrainageRate;
			//			firstPit.spilloverTime = firstPit.retentionVolume/firstPit.netAccumulationRate;
			firstPit.spilloverTime = Double.POSITIVE_INFINITY;

			// Removed the second pit
			this.pits.pitDataList.remove(secondPit);

			//			////////////////////////////////////////
			//			int color = this.pits.pitsBitmap.getPixel(this.dem[0].length - 1 - firstPit.outletSpilloverFlowDirection.x, firstPit.outletSpilloverFlowDirection.y);
			//			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
			//				this.pits.pitIdMatrix[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = secondPitId;
			//				this.pits.pitsBitmap.setPixel(this.dem[0].length - 1 - firstPit.allPointsList.get(i).x, firstPit.allPointsList.get(i).y, color);				
			//			}
			//			resolveFilledArea();
			//
			//			// Update all pits that will overflow into the filled pit as now overflowing into the second pit's ID
			//			for (int i = 0; i < this.pits.pitDataList.size(); i++){
			//				if (this.pits.pitDataList.get(i).pitIdOverflowingInto == firstPit.pitId) {
			//					this.pits.pitDataList.get(i).pitIdOverflowingInto = secondPitId;
			//				}
			//			}
			//			this.pits.pitDataList.remove(0);
		}
		return true;	
	}

	public boolean resolveFilledArea() {
		Pit firstPit = this.pits.pitDataList.get(0);
		List<Point> allPointsToResolve = new ArrayList<Point>();
		List<Point> pointsToResolve = new ArrayList<Point>();
		List<Point> pointsResolved = new ArrayList<Point>();
		// Adjust DEM elevations
		for (int i = 0; i < firstPit.allPointsList.size(); i++) {
			if (this.dem[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] < firstPit.spilloverElevation) {
				this.dem[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = firstPit.spilloverElevation;
				Point pointToResolve = firstPit.allPointsList.get(i);
				allPointsToResolve.add(pointToResolve);
			}
		}
		// Resolve flow direction to direct flow out of the pit
		this.flowDirection[firstPit.pitOutletPoint.y][firstPit.pitOutletPoint.x] = new FlowDirectionCell(firstPit.outletSpilloverFlowDirection);
		pointsToResolve.add(firstPit.pitOutletPoint);
		while (!pointsToResolve.isEmpty()) {
			Point currentPoint = pointsToResolve.get(0);
			pointsResolved.add(currentPoint);
			pointsToResolve.remove(0);
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					Point neighborPoint = new Point(currentPoint.x + x, currentPoint.y + y);
					// check if the point is part of the complete list to be resolved, but not already on the "next up" list
					if (allPointsToResolve.contains(neighborPoint) && !pointsToResolve.contains(neighborPoint) && !pointsResolved.contains(neighborPoint)) {
						this.flowDirection[neighborPoint.y][neighborPoint.x].childPoint = currentPoint;
						pointsToResolve.add(neighborPoint);
						allPointsToResolve.remove(neighborPoint);
					}
				}			
			}

		}
		return true;
	}

	public Bitmap delineate(Point point, AsyncTask task) {
		if(task instanceof DelineationListener) {
			delineationListener = (DelineationListener) task;
		} else {
			throw new ClassCastException("WatershedDataset - Task must implement DelineationListener");
		}
		int numrows = this.dem.length;
		int numcols = this.dem[0].length;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		
		Bitmap icon = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.watershedelineation, options);
		Bitmap delinBitmap = Bitmap.createScaledBitmap(icon, numcols, numrows, false);
		for (int r = 1; r < numrows-1; r++) {
			for (int c = 1; c < numcols-1; c++) {
				delinBitmap.setPixel(numcols - 1 - c, r, Color.TRANSPARENT);
			}
		}
		delineatedArea = 0; //number of cells in the delineation
		delineatedStorageVolume = 0.0;

		// discover adjacent points that may be part of a puddle
		List<Point> indicesToCheck = new ArrayList<Point>();
		List<Point> indicesToCheckPuddle = new ArrayList<Point>();
		float puddleElevation = this.dem[point.y][point.x]; 
		indicesToCheck.add(point);
		indicesToCheckPuddle.add(point);


		while (!indicesToCheckPuddle.isEmpty()) {
			int r = indicesToCheckPuddle.get(0).y;
			int c = indicesToCheckPuddle.get(0).x;
			indicesToCheckPuddle.remove(0);
			if (delinBitmap.getPixel(numcols - 1 - c, r) == Color.RED){
				continue;
			}
			delinBitmap.setPixel(numcols - 1 - c, r, Color.RED);
			delineatedArea++;
			delineatedStorageVolume += dem[r][c] - originalDem[r][c];
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					if (r+y >= numrows-1 || r+y <= 0 || c+x >= numcols-1 || c+x <= 0) {
						continue;
					}
					if (dem[r+y][c+x] == puddleElevation && delinBitmap.getPixel(numcols - 1 - (c+x), (r+y)) != Color.RED) {
						indicesToCheckPuddle.add(new Point(c+x, r+y));
						indicesToCheck.add(new Point(c+x, r+y));
					}
				}
			}
		}

		// Add a buffer around the chosen pixel to provide a more likely meaningful delineation
		for (int x = -3; x < 4; x++) {
			for (int y = -3; y < 4; y++) {
				if (point.y+y >= numrows-1 || point.y+y <= 0 || point.x+x >= numcols-1 || point.x+x <= 0) {
					continue;
				}
				if (delinBitmap.getPixel(numcols - 1 - (point.x+x), (point.y+y)) != Color.RED) {
					indicesToCheck.add(new Point(x+point.x, y +point.y));
					delinBitmap.setPixel(numcols - 1 - (point.x+x), (point.y+y), Color.RED);
					delineatedArea++;
					delineatedStorageVolume += dem[point.y+y][point.x+x] - originalDem[point.y+y][point.x+x];
				}
			}
		}

		// Now find all cells draining to either the puddle or the buffered delineation point
		while (!indicesToCheck.isEmpty()) {                        
			int r = indicesToCheck.get(0).y;
			int c = indicesToCheck.get(0).x;
			indicesToCheck.remove(0);

			if (flowDirection[r][c].parentList.isEmpty()) {
				continue;
			}

			for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
				if (delinBitmap.getPixel(numcols - 1 - flowDirection[r][c].parentList.get(i).x, flowDirection[r][c].parentList.get(i).y) != Color.RED) {
					indicesToCheck.add(flowDirection[r][c].parentList.get(i));
					delinBitmap.setPixel(numcols - 1 - flowDirection[r][c].parentList.get(i).x, flowDirection[r][c].parentList.get(i).y, Color.RED);
					delineatedArea++;
					delineatedStorageVolume += dem[r][c] - originalDem[r][c];
				}
			}
		}
		return delinBitmap;
	}


	public Bitmap drawPuddles() {
		int numrows = dem.length;
		int numcols = dem[0].length;
		int[] colorarray = new int[this.pits.pitIdMatrix.length*this.pits.pitIdMatrix[0].length];
		Arrays.fill(colorarray, Color.TRANSPARENT);
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		Bitmap puddleBitmap = Bitmap.createBitmap(colorarray, numcols, numrows, config);
		puddleBitmap = puddleBitmap.copy(config, true);

		for (int r = 1; r < numrows-1; r++) {
			for (int c = 1; c < numcols-1; c++) {
				if (r >= numrows-1 || r <= 0 || c >= numcols-1 || c <= 0) {
					continue;
				}
				if (originalDem[r][c] < dem[r][c]) {
					puddleBitmap.setPixel(numcols - 1 - c, r, Color.BLUE);
				}
			}
		}
		return puddleBitmap;
	}

//	public void drawFlowPaths() {
//		int numrows = dem.length;
//		int numcols = dem[0].length;
//
//		for (int r = 1; r < 50; r++) {
//			for (int c = 1; c < numcols-1; c++) {
//				if (r >= numrows-1 || r <= 0 || c >= numcols-1 || c <= 0) {
//					continue;
//				}
//				LatLng parentLatLng = bitmapRowColToLatLng(r, c);
//				LatLng childLatLng = bitmapRowColToLatLng(this.flowDirection[r][c].childPoint.y, this.flowDirection[r][c].childPoint.x);
//				Polyline flowPath = MainActivity.map.addPolyline(new PolylineOptions()
//				.add(parentLatLng, childLatLng)
//				.color(Color.RED));
//			}
//		}
//	}

//	public LatLng bitmapRowColToLatLng(double r, double c) {
//		double xULCorner = TiffDecoder.nativeTiffGetCornerLongitude();
//		double yULCorner = TiffDecoder.nativeTiffGetCornerLatitude();
//
//		CoordinateConversion conversion = new CoordinateConversion();
//		String UTM = TiffDecoder.nativeTiffGetParams();
//		String UTMZone = UTM.substring(18, 20).concat(" ").concat(UTM.substring(20, 21)).concat(" ");
//		double x = xULCorner + (dem[0].length*cellSizeX) - c*cellSizeX;
//		double y = yULCorner - (r*cellSizeY);
//		double latLng[] = conversion.utm2LatLon(UTMZone + Integer.toString((int)x) + " " + Integer.toString((int)y));
//		LatLng latLong = new LatLng(latLng[0], latLng[1]);
//		return latLong;
//	}
}
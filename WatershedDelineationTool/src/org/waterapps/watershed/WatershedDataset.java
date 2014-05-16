
package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.Driver;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.osr.SpatialReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.openatk.openatklib.atkmap.models.ATKPolygon;

public class WatershedDataset {
	FlowDirectionCell[][] flowDirection;
	float[][] originalDem;
	float[][] dem;
	float[][] drainage;
	public static PitRaster pits;
	static float cellSize;
	static int status = 0;
	static String statusMessage = "Reading DEM";
	public static float noDataVal;
	public static boolean fillAllPits = false;
	public static int delineatedArea = 0;
	WatershedDatasetListener listener;
	DelineationListener delineationListener;
	static Layer layer;
	static org.gdal.ogr.DataSource dst;

	public interface WatershedDatasetListener {
		public void simulationOnProgress(int progress, String status);
		public void simulationDone();
	}

	public interface DelineationListener {
		public void delineationOnProgress(Bitmap bitmap);
		public void delineationDone();
	}

	// Constructor
	public WatershedDataset(float[][] inputDem, float inputCellSize, float inputNoDataVal, AsyncTask task) {

		if(task instanceof WatershedDatasetListener) {
			listener = (WatershedDatasetListener) task;
		} else {
			throw new ClassCastException("WatershedDataset - Task must implement WatershedDatasetListener");
		}

		noDataVal = inputNoDataVal;
		drainage = null;
		cellSize = inputCellSize;

		// Load the DEM
		listener.simulationOnProgress(status, "Reading DEM");
		originalDem = inputDem;
		dem = new float[originalDem.length][originalDem[0].length];
		for (int r = 0; r < originalDem.length; r++) {
			for (int c = 0; c < originalDem[0].length; c ++) {
				dem[r][c] = originalDem[r][c];
			}
		}
		
		listener.simulationOnProgress(status, "Discovering Flow Routes");
		// Compute Flow Direction
		int pitCellCount = computeFlowDirection();		
		// Compute Pits
		listener.simulationOnProgress(status, "Identifying Surface Depressions");
		pits = new PitRaster(dem, drainage, flowDirection, cellSize, listener);
		pits.constructPitRaster(pitCellCount);
		listener.simulationOnProgress(status, "Done");
	}

	public void recalculatePitsForNewRainfall() {
		MainActivity.simulateButton.setEnabled(false);
		for (int i=0; i < WatershedDataset.pits.pitDataList.size(); i++) {
			if (WatershedDataset.pits.pitDataList.get(i).pitId < 0) {
				continue;
			}
//			float netAccumulationRate = (RainfallSimConfig.rainfallIntensity * WatershedDataset.pits.pitDataList.get(i).allPointsList.size() * cellSizeX * cellSizeY);
//			WatershedDataset.pits.pitDataList.get(i).spilloverTime = WatershedDataset.pits.pitDataList.get(i).retentionVolume / netAccumulationRate;
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


	public int computeFlowDirection() {
		int pitCellCount = 0;

		flowDirection = new FlowDirectionCell[this.dem.length][this.dem[0].length];
		for (int c = 0; c < this.dem[0].length; c++) {
			for (int r = 0; r < this.dem.length; r++) {
				Point childPoint = null;
				// If the cell is along the border then it should remain a null
				if (r == this.dem.length-1 || r == 0 || c == this.dem[0].length-1 || c == 0) {
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
				float minimumSlope = Float.NaN;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						float distance = (float) Math.sqrt((Math.pow(x, 2) + Math.pow(y, 2)));
						float slope = (dem[r+y][c+x] - dem[r][c])/distance;
						//maintain current minimum slope, minimum slope being the steepest downslope
						if (Float.isNaN(minimumSlope) || slope < minimumSlope) {
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
			status = (int) (20 + (10 * (((c*this.dem.length))/((float) this.dem.length*this.dem[0].length))));
			listener.simulationOnProgress(status, "Discovering Flow Routes");
		}

		// Now go back through and also build a list of parents so the tree structure can be traversed either way.
		// Edge pixels may have parents, but lack the neighbors to determine a valid flow direction (child). 
		for (int c = 0; c < this.dem[0].length; c++) {
			for (int r = 0; r < this.dem.length; r++) {
				Point currentPoint = new Point(c, r);
				ArrayList<Point> parentList = new ArrayList<Point>(8);				
				// Find all cells pointing to current cell.
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						if (r+y > this.dem.length-1 || r+y < 0 || c+x > this.dem[0].length-1 || c+x < 0) {
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
			status = (int) (30 + (10 * (((c*this.dem.length))/((float)this.dem.length*this.dem[0].length))));
			listener.simulationOnProgress(status, "Discovering Flow Routes");
		}
		return pitCellCount;
	}
	
	public void findFlowDirectionParents(List<Point> cellsToFindParents) {
		for (int i = 0; i < cellsToFindParents.size(); i++) {
			int r = cellsToFindParents.get(i).y;
			int c = cellsToFindParents.get(i).x;
			ArrayList<Point> parentList = new ArrayList<Point>(8);
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					if (r+y > this.dem.length-1 || r+y < 0 || c+x > this.dem[0].length-1 || c+x < 0) {
						continue;
					}
					if (flowDirection[r+y][c+x].childPoint != null) {
						if (flowDirection[r+y][c+x].childPoint.y == r && flowDirection[r+y][c+x].childPoint.x == c) { //apparently, this is not the same thing as "flowDirection[r+y][c+x].childPoint == currentPoint"; perhaps checking == for two points doesn't work
							parentList.add(new Point(c+x, r+y));
						}
					}
				}
			}
			flowDirection[r][c].setParentList(parentList);
		}
	}

	public void resolveFlowDirectionParents() {
		for (int c = 0; c < this.dem[0].length; c++) {	
			for (int r = 0; r < this.dem.length; r++) {
				if (r > this.dem.length-1 || r < 0 || c > this.dem[0].length-1 || c < 0) {
					continue;
				}
				Point currentPoint = new Point(c, r);
				ArrayList<Point> parentList = new ArrayList<Point>();

				// Find all cells pointing to current cell. This comes after assuring that the current cell isn't on the border.
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;}
						if (r+y >= this.dem.length-1 || r+y <= 0 || c+x >= this.dem[0].length-1 || c+x <= 0) {
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

	// Wrapper function that simulates the rainfall event to iteratively fill depressions until the rainfall event ends or no more remain
	@SuppressWarnings("unchecked")
	public boolean fillPits() {
		statusMessage = "Filling and Merging Depressions";
		int fill_counter = 0;
		Collections.sort(WatershedDataset.pits.pitDataList);
		int numberOfPits = pits.pitDataList.size();
		long pre = System.currentTimeMillis();
		if (fillAllPits) {
			// Once a pit is connected to the edge of the map, it becomes negative.  All negative pits (and only negative pits) should have an 
			// infinite spillover time, placing them at the end of the list.  If the first pit in the list has a negative ID,
			// then all remaining pits are negative and filling is complete.  
			while (WatershedDataset.pits.pitDataList.get(0).pitId > 0) {
				altMergePits();
				if (WatershedDataset.pits.pitDataList.isEmpty()) {
					// No more pits exist, filling is 100% complete for this simulation
					status = 100;
					break;
				}
				Collections.sort(WatershedDataset.pits.pitDataList);

				fill_counter++;
				status = (int) (100 * (fill_counter/(float)numberOfPits));
				listener.simulationOnProgress(status, "Simulating Rainfall");
			}
		} else {
			// Handle rainfall/duration-based filling.
			while (WatershedDataset.pits.pitDataList.get(0).spilloverTime < RainfallSimConfig.rainfallDuration) {
				altMergePits();
				if (WatershedDataset.pits.pitDataList.isEmpty()) {
					// No more pits exist, filling is 100% complete for this simulation
					status = 100;
					break;
				}
//				System.out.println(" ");
				fill_counter++;
				status = (int) (100 * (fill_counter/(float)numberOfPits));
				listener.simulationOnProgress(status, "Simulating Rainfall");
			}
		}
//		resolveFlowDirectionParents();
		// time has expired for the storm event, filling is 100% complete for this simulation
		status = 100;
		listener.simulationOnProgress(status, "Finished");
		drawPuddles();
		long post = System.currentTimeMillis();
		System.out.println(Long.toString(post-pre) + ",");
		return true;
	}
	
//	public void checkFlow(Point pitBottom) {
//		List<Point> indicesToCheck = new ArrayList<Point>();
//		indicesToCheck.add(pitBottom);
//		int checkArea = 1;
//		while (!indicesToCheck.isEmpty()) {                        
//			int r = indicesToCheck.get(0).y;
//			int c = indicesToCheck.get(0).x;
//			indicesToCheck.remove(0);
//
//			if (flowDirection[r][c].parentList.isEmpty()) {
//				continue;
//			}
//
//			for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
//				indicesToCheck.add(flowDirection[r][c].parentList.get(i));
//				checkArea++;
//			}
//		}
//	}
	
	public boolean altMergePits() {
		Pit firstPit = WatershedDataset.pits.pitDataList.get(0);
		int secondPitId = WatershedDataset.pits.pitIdMatrix[firstPit.outletSpilloverFlowDirection.y][firstPit.outletSpilloverFlowDirection.x];
		int secondPitListIndex = WatershedDataset.pits.getIndexOf(secondPitId);
		Pit secondPit = WatershedDataset.pits.pitDataList.get(secondPitListIndex);
		
		// Handle pits merging with other pits
		if (secondPitId > 0) {
			int mergedPitId = WatershedDataset.pits.maxPitId;
			WatershedDataset.pits.maxPitId++;
			
			secondPit.pitBorderIndicesList.addAll(firstPit.pitBorderIndicesList);
			secondPit.spilloverElevation = Float.NaN;
			// traverse in reverse order.  some of the border indices will be found to be not on the border and removed from the list (necessitating the onBorder variable)
			for (int i = secondPit.pitBorderIndicesList.size()-1; i > -1; i--) {
				Point currentPoint = secondPit.pitBorderIndicesList.get(i);
				int r = currentPoint.y;
				int c = currentPoint.x;
				boolean onBorder = false;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}

						if ((WatershedDataset.pits.pitIdMatrix[r+y][c+x] != firstPit.pitId) && (WatershedDataset.pits.pitIdMatrix[r+y][c+x] != secondPitId)) {
							float currentElevation = this.dem[r][c];
							float neighborElevation = this.dem[r+y][c+x];
							onBorder = true;
							if (Float.isNaN(secondPit.spilloverElevation) || (currentElevation <= secondPit.spilloverElevation && neighborElevation <= secondPit.spilloverElevation)) {
								secondPit.spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
								secondPit.pitOutletPoint = currentPoint;
								secondPit.outletSpilloverFlowDirection = new Point(c+x, r+y);
							}
						}
					}
				}
				if (onBorder == false) {
					secondPit.pitBorderIndicesList.remove(currentPoint);
				}
			}
			secondPit.pitId = mergedPitId;			
			secondPit.filledVolume = secondPit.filledVolume + firstPit.retentionVolume;
			secondPit.retentionVolume = secondPit.filledVolume;
//			List<Point> raisedPoints = new ArrayList<Point>(firstPit.area);
			int raisedPointsCount = 0;
			List<Point> indicesToCheck = new ArrayList<Point>(firstPit.area);
			indicesToCheck.add(firstPit.pitPoint);
			for (int j = 0; j < firstPit.area; j++) {
				int r = indicesToCheck.get(j).y;
				int c = indicesToCheck.get(j).x;
				if (this.dem[r][c] <= firstPit.spilloverElevation) {
//					raisedPoints.add(indicesToCheck.get(j));
					raisedPointsCount++;
					this.dem[r][c] = firstPit.spilloverElevation;
				} else {
					WatershedDataset.pits.pitIdMatrix[r][c] = mergedPitId;
				}
				if (this.dem[r][c] < secondPit.spilloverElevation) {
					secondPit.retentionVolume += ((secondPit.spilloverElevation - this.dem[r][c]) * cellSize * cellSize);
				}
				if (flowDirection[r][c].parentList.isEmpty()) {
					continue;
				}
				for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
					indicesToCheck.add(flowDirection[r][c].parentList.get(i));
				}
			}

			indicesToCheck = new ArrayList<Point>(secondPit.area);
			indicesToCheck.add(secondPit.pitPoint);
			// re-ID second pit
			for (int j = 0; j < secondPit.area; j++) {
				int r = indicesToCheck.get(j).y;
				int c = indicesToCheck.get(j).x;
				WatershedDataset.pits.pitIdMatrix[r][c] = mergedPitId;
				if (this.dem[r][c] < secondPit.spilloverElevation) {
					secondPit.retentionVolume += ((secondPit.spilloverElevation - this.dem[r][c]) * cellSize * cellSize);
				}
				if (flowDirection[r][c].parentList.isEmpty()) {
					continue;
				}
				for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
					indicesToCheck.add(flowDirection[r][c].parentList.get(i));
				}
			}
			indicesToCheck = null;
			
			// Resolve flow direction to direct flow out of the pit
//			List<Point> toCheckForNeighbors = new ArrayList<Point>(raisedPoints.size());
			List<Point> toCheckForNeighbors = new ArrayList<Point>(raisedPointsCount);
			this.flowDirection[firstPit.pitOutletPoint.y][firstPit.pitOutletPoint.x].childPoint = firstPit.outletSpilloverFlowDirection;
			this.flowDirection[firstPit.outletSpilloverFlowDirection.y][firstPit.outletSpilloverFlowDirection.x].parentList.add(firstPit.pitOutletPoint);
			WatershedDataset.pits.pitIdMatrix[firstPit.pitOutletPoint.y][firstPit.pitOutletPoint.x] = mergedPitId;
			toCheckForNeighbors.add(firstPit.pitOutletPoint);
//			for (int i = 0; i < raisedPoints.size(); i++) {
			for (int i = 0; i < raisedPointsCount; i++) {
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						Point neighborPoint = new Point(toCheckForNeighbors.get(i).x + x, toCheckForNeighbors.get(i).y + y);
						// check if the point is part of the complete list to be resolved, but not already on the "next up" list
//						if (raisedPoints.contains(neighborPoint) && !toCheckForNeighbors.contains(neighborPoint)) {
						if ((WatershedDataset.pits.pitIdMatrix[neighborPoint.y][neighborPoint.x] == firstPit.pitId) && (WatershedDataset.pits.pitIdMatrix[neighborPoint.y][neighborPoint.x] != mergedPitId)) {
							this.flowDirection[neighborPoint.y][neighborPoint.x].childPoint = toCheckForNeighbors.get(i);
							WatershedDataset.pits.pitIdMatrix[neighborPoint.y][neighborPoint.x] = mergedPitId;
							toCheckForNeighbors.add(neighborPoint);
						}
					}
				}
			}
			findFlowDirectionParents(toCheckForNeighbors);
			toCheckForNeighbors = null;
//			raisedPoints = null;
	
			//Sum the drainage taking place in the pit
			secondPit.area = firstPit.area + secondPit.area;
			secondPit.pitDrainageRate = 0;
			float netAccumulationRate = (RainfallSimConfig.rainfallIntensity * secondPit.area * cellSize * cellSize) - secondPit.pitDrainageRate;
			secondPit.spilloverTime = secondPit.retentionVolume/netAccumulationRate;
			
		// Handle pits that begin to run off the DEM
		} else if (secondPitId < 0) {
			int mergedPitId = WatershedDataset.pits.minPitId;
			WatershedDataset.pits.minPitId--;

			secondPit.pitBorderIndicesList.addAll(firstPit.pitBorderIndicesList);
			// traverse in reverse order.  some of the border indices will be found to be not on the border and removed from the list (hence, the onBorder variable)
			for (int i = secondPit.pitBorderIndicesList.size()-1; i > -1; i--) {
				Point currentPoint = secondPit.pitBorderIndicesList.get(i);
				int r = currentPoint.y;
				int c = currentPoint.x;
				boolean onBorder = false;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						if (r+y > WatershedDataset.pits.pitIdMatrix.length-1 || r+y < 0 || c+x > WatershedDataset.pits.pitIdMatrix[0].length-1 || c+x < 0) {
							continue;
						}
						if ((WatershedDataset.pits.pitIdMatrix[r+y][c+x] != secondPit.pitId && WatershedDataset.pits.pitIdMatrix[r+y][c+x] != firstPit.pitId) || (r == WatershedDataset.pits.pitIdMatrix.length-1 || r == 0 || c == WatershedDataset.pits.pitIdMatrix[0].length-1 || c == 0)) {
							onBorder = true;
						}
					}
				}
				if (onBorder == false) {
					secondPit.pitBorderIndicesList.remove(currentPoint);
				}				
			}
			secondPit.pitId = mergedPitId;
			
			// Fill the first pit and resolve flow direction. This must be completed before the new pit entry is created or else retention volumes will be incorrectly calculated (the first pit must be filled).
//			List<Point> raisedPoints = new ArrayList<Point>(firstPit.area);
			int raisedPointsCount = 0;
			List<Point> indicesToCheck = new ArrayList<Point>(firstPit.area);
			indicesToCheck.add(firstPit.pitPoint);
			for (int j = 0; j < firstPit.area; j++) {
				int r = indicesToCheck.get(j).y;
				int c = indicesToCheck.get(j).x;
//				WatershedDataset.pits.pitIdMatrix[r][c] = mergedPitId;
				if (this.dem[r][c] <= firstPit.spilloverElevation) {
//					raisedPoints.add(indicesToCheck.get(j));
					raisedPointsCount++;
					this.dem[r][c] = firstPit.spilloverElevation;
				} else {
					WatershedDataset.pits.pitIdMatrix[r][c] = mergedPitId;
				}

				if (flowDirection[r][c].parentList.isEmpty()) {
					continue;
				}
				for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
					indicesToCheck.add(flowDirection[r][c].parentList.get(i));
				}
			}			
			
			indicesToCheck = new ArrayList<Point>(secondPit.area);
			indicesToCheck.add(secondPit.pitPoint);
			// re-ID second pit
			for (int j = 0; j < secondPit.area; j++) {
				int r = indicesToCheck.get(j).y;
				int c = indicesToCheck.get(j).x;
				WatershedDataset.pits.pitIdMatrix[r][c] = mergedPitId;

				if (flowDirection[r][c].parentList.isEmpty()) {
					continue;
				}
				for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
					indicesToCheck.add(flowDirection[r][c].parentList.get(i));
				}
			}
			indicesToCheck = null;

			// Resolve flow direction to direct flow out of the pit
//			List<Point> toCheckForNeighbors = new ArrayList<Point>(raisedPoints.size());
			List<Point> toCheckForNeighbors = new ArrayList<Point>(raisedPointsCount);
			this.flowDirection[firstPit.pitOutletPoint.y][firstPit.pitOutletPoint.x].childPoint = firstPit.outletSpilloverFlowDirection;
			this.flowDirection[firstPit.outletSpilloverFlowDirection.y][firstPit.outletSpilloverFlowDirection.x].parentList.add(firstPit.pitOutletPoint);
			WatershedDataset.pits.pitIdMatrix[firstPit.pitOutletPoint.y][firstPit.pitOutletPoint.x] = mergedPitId;
			toCheckForNeighbors.add(firstPit.pitOutletPoint);
//			for (int i = 0; i < raisedPoints.size(); i++) {
			for (int i = 0; i < raisedPointsCount; i++) {
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						Point neighborPoint = new Point(toCheckForNeighbors.get(i).x + x, toCheckForNeighbors.get(i).y + y);
						// check if the point is part of the complete list to be resolved, but not already on the "next up" list
//						if (raisedPoints.contains(neighborPoint) && !toCheckForNeighbors.contains(neighborPoint)) {
						if ((WatershedDataset.pits.pitIdMatrix[neighborPoint.y][neighborPoint.x] == firstPit.pitId) && (WatershedDataset.pits.pitIdMatrix[neighborPoint.y][neighborPoint.x] != mergedPitId)) {
							this.flowDirection[neighborPoint.y][neighborPoint.x].childPoint = toCheckForNeighbors.get(i);
							WatershedDataset.pits.pitIdMatrix[neighborPoint.y][neighborPoint.x] = mergedPitId;
							toCheckForNeighbors.add(neighborPoint);
						}
					}
				}
			}
			findFlowDirectionParents(toCheckForNeighbors);
			toCheckForNeighbors = null;
//			raisedPoints = null;
			
			secondPit.area = firstPit.area + secondPit.area;
			secondPit.filledVolume = secondPit.filledVolume + firstPit.retentionVolume;
			secondPit.retentionVolume = secondPit.filledVolume;
			secondPit.pitDrainageRate = 0.0f;
			secondPit.spilloverTime = Float.POSITIVE_INFINITY;
		}
		//Remove first pit	
		WatershedDataset.pits.pitDataList.remove(firstPit);
		if (secondPit.spilloverTime == Float.POSITIVE_INFINITY) {
			WatershedDataset.pits.pitDataList.add(secondPit); // add to end of list; shouldn't change order of list
			WatershedDataset.pits.pitDataList.remove(secondPit);
		}else {
			WatershedDataset.pits.pitDataList.remove(secondPit);
			for (int i = 0; i < WatershedDataset.pits.pitDataList.size(); i++) {
				if (WatershedDataset.pits.pitDataList.get(i).spilloverTime > secondPit.spilloverTime) {
					WatershedDataset.pits.pitDataList.add(i, secondPit);
					break;
				}
			}
		}
		return true;	
	}
	
	// Merge two pits
//	public boolean mergePits() {
//		Pit firstPit = WatershedDataset.pits.pitDataList.get(0);
//		int secondPitId = WatershedDataset.pits.pitIdMatrix[firstPit.outletSpilloverFlowDirection.y][firstPit.outletSpilloverFlowDirection.x];
////		long pre;
////		long post;
//
//		// Handle pits merging with other pits
//		if (secondPitId > -1) {
////			pre = System.currentTimeMillis();
//			int mergedPitID = WatershedDataset.pits.maxPitId++;
//			Pit secondPit = WatershedDataset.pits.pitDataList.get(WatershedDataset.pits.getIndexOf(secondPitId));
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
////			pre = System.currentTimeMillis();
//			// re-ID the two merging pits with their new mergedPitID
//			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
//				WatershedDataset.pits.pitIdMatrix[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = mergedPitID;
//			}
//			for (int i = 0; i < secondPit.allPointsList.size(); i++) {
//				WatershedDataset.pits.pitIdMatrix[secondPit.allPointsList.get(i).y][secondPit.allPointsList.get(i).x] = mergedPitID;
//			}
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
//			// Fill the first pit and resolve flow direction. This must be completed before the new pit entry is created or else retention volumes will be incorrectly calculated (the first pit must be filled).
////			pre = System.currentTimeMillis();
//			resolveFilledArea();
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
////			pre = System.currentTimeMillis();
//			// Rather than create the new merged pit entry, overwrite the second pit with the new merged pit data
//			firstPit.pitId = mergedPitID;
//			firstPit.allPointsList.addAll(0, secondPit.allPointsList); //put the second pit's indices at the beginning of the list (so that the pit bottom is always the 0th item)
//			firstPit.pitBorderIndicesList.addAll(0, secondPit.pitBorderIndicesList);
//			firstPit.spilloverElevation = Float.NaN;
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
////			pre = System.currentTimeMillis();
//			// traverse in reverse order.  some of the border indices will be found to be not on the border and removed from the list (necessitating the onBorder variable)
//			for (int i = firstPit.pitBorderIndicesList.size()-1; i > -1; i--) {
//				Point currentPoint = firstPit.pitBorderIndicesList.get(i);
//				int r = currentPoint.y;
//				int c = currentPoint.x;
//				boolean onBorder = false;
//				for (int x = -1; x < 2; x++) {
//					for (int y = -1; y < 2; y++){
//						if (x == 0 && y == 0) {
//							continue;
//						}
//
//						if (WatershedDataset.pits.pitIdMatrix[r+y][c+x] != WatershedDataset.pits.pitIdMatrix[r][c]) {
//							float currentElevation = this.dem[r][c];
//							float neighborElevation = this.dem[r+y][c+x];
//							onBorder = true;
//							if (Float.isNaN(firstPit.spilloverElevation) || (currentElevation <= firstPit.spilloverElevation && neighborElevation <= firstPit.spilloverElevation)) {
//								firstPit.spilloverElevation = (float) Math.max(neighborElevation, currentElevation);
//								firstPit.pitOutletPoint = currentPoint;
//								firstPit.outletSpilloverFlowDirection = new Point(c+x, r+y);
////								firstPit.pitIdOverflowingInto = this.pits.pitIdMatrix[r+y][c+x];
//							}
//						}
//					}
//				}
//				if (onBorder == false) {
//					firstPit.pitBorderIndicesList.remove(currentPoint);
//				}
//			}
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
////			pre = System.currentTimeMillis(); 
//			// Volume/elevation-dependent variables and calculations
//			firstPit.filledVolume = secondPit.filledVolume + firstPit.retentionVolume;
//			firstPit.retentionVolume = firstPit.filledVolume;
//			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
//				int r = firstPit.allPointsList.get(i).y;
//				int c = firstPit.allPointsList.get(i).x;
//				if (this.dem[r][c] < firstPit.spilloverElevation) {
//					firstPit.retentionVolume += ((firstPit.spilloverElevation - this.dem[r][c]) * cellSize * cellSize);
//				}
//			}
//
//			//Sum the drainage taking place in the pit
//			firstPit.pitDrainageRate = 0;
//			//				for (int listIdx = 0; listIdx < firstPit.allPointsList.size(); listIdx++) {
//			//					Point currentPoint = firstPit.allPointsList.get(listIdx);
//			//					int r = currentPoint.y;
//			//					int c = currentPoint.x;
//			//					firstPit.pitDrainageRate = firstPit.pitDrainageRate; // + drainage[r][c]
//			//				}
//			float netAccumulationRate = (RainfallSimConfig.rainfallIntensity * firstPit.allPointsList.size() * cellSize * cellSize) - firstPit.pitDrainageRate;
//			firstPit.spilloverTime = firstPit.retentionVolume/netAccumulationRate;
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//			
////			pre = System.currentTimeMillis();
//			// Removed the second pit
//			WatershedDataset.pits.pitDataList.remove(secondPit);
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//			// Handle pits that begin to run off the DEM
//		} else if (secondPitId <= -1) {
////			pre = System.currentTimeMillis();
//			int mergedPitId = WatershedDataset.pits.minPitId--;
//			Pit secondPit = WatershedDataset.pits.pitDataList.get(WatershedDataset.pits.getIndexOf(secondPitId));
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//			
////			pre = System.currentTimeMillis();
//			// re-ID the two merging pits with their new mergedPitID, and color the first pit with the color of the second pit
//			for (int i = 0; i < firstPit.allPointsList.size(); i++) {
//				WatershedDataset.pits.pitIdMatrix[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = mergedPitId;
////				this.pits.pitsBitmap.setPixel(this.dem[0].length - 1 - firstPit.allPointsList.get(i).x, firstPit.allPointsList.get(i).y, secondPit.color);
//			}
//			for (int i = 0; i < secondPit.allPointsList.size(); i++) {
//				WatershedDataset.pits.pitIdMatrix[secondPit.allPointsList.get(i).y][secondPit.allPointsList.get(i).x] = mergedPitId;
//			}
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
//			// Fill the first pit and resolve flow direction. This must be completed before the new pit entry is created or else retention volumes will be incorrectly calculated (the first pit must be filled).
////			pre = System.currentTimeMillis();
//			resolveFilledArea();
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//			
////			pre = System.currentTimeMillis();
//			// Rather than create the new merged pit entry, overwrite the first pit with the new merged pit data
////			firstPit.color = secondPit.color;
//			firstPit.allPointsList.addAll(0, secondPit.allPointsList);
//			firstPit.pitId = mergedPitId;
//			firstPit.pitBorderIndicesList.addAll(0, secondPit.pitBorderIndicesList);
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
////			pre = System.currentTimeMillis();
//			firstPit.pitOutletPoint = secondPit.allPointsList.get(0);
//			firstPit.spilloverElevation = dem[secondPit.allPointsList.get(0).y][secondPit.allPointsList.get(0).x];
//			// traverse in reverse order.  some of the border indices will be found to be not on the border and removed from the list (hence, the onBorder variable)
//			for (int i = firstPit.pitBorderIndicesList.size()-1; i > -1; i--) {
//				Point currentPoint = firstPit.pitBorderIndicesList.get(i);
//				int r = currentPoint.y;
//				int c = currentPoint.x;
//				boolean onBorder = false;
//				for (int x = -1; x < 2; x++) {
//					for (int y = -1; y < 2; y++){
//						if (x == 0 && y == 0) {
//							continue;
//						}
//						if (r+y > WatershedDataset.pits.pitIdMatrix.length-1 || r+y < 0 || c+x > WatershedDataset.pits.pitIdMatrix[0].length-1 || c+x < 0) {
//							continue;
//						}
//						if (WatershedDataset.pits.pitIdMatrix[r+y][c+x] != WatershedDataset.pits.pitIdMatrix[r][c] || (r == WatershedDataset.pits.pitIdMatrix.length-1 || r == 0 || c == WatershedDataset.pits.pitIdMatrix[0].length-1 || c == 0)) {
//							onBorder = true;
//						}
//					}
//				}
//				if (onBorder == false) {
//					firstPit.pitBorderIndicesList.remove(currentPoint);
//				}
//			}
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
////			pre = System.currentTimeMillis();
//			firstPit.filledVolume = secondPit.filledVolume + firstPit.retentionVolume;
//			firstPit.retentionVolume = firstPit.filledVolume;
//
//			//Sum the drainage taking place in the pit
//			firstPit.pitDrainageRate = 0.0f;
//			//				for (int listIdx = 0; listIdx < firstPit.allPointsList.size(); listIdx++) {
//			//					Point currentPoint = firstPit.allPointsList.get(listIdx);
//			//					int r = currentPoint.y;
//			//					int c = currentPoint.x;
//			//					firstPit.pitDrainageRate = firstPit.pitDrainageRate; // + drainage[r][c]
//			//				}
//			firstPit.spilloverTime = Float.POSITIVE_INFINITY;
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//
////			pre = System.currentTimeMillis();
//			// Removed the second pit
//			WatershedDataset.pits.pitDataList.remove(secondPit);
////			post = System.currentTimeMillis();
////			System.out.print(Long.toString(post-pre) + ",");
//		}
////		pre = System.currentTimeMillis();
//		if (firstPit.spilloverTime == Float.POSITIVE_INFINITY) {
//			WatershedDataset.pits.pitDataList.add(firstPit);
//			WatershedDataset.pits.pitDataList.remove(0);
//		}else {
//			for (int i = 0; i < WatershedDataset.pits.pitDataList.size(); i++) {
//				if (WatershedDataset.pits.pitDataList.get(i).spilloverTime > firstPit.spilloverTime) {
//					WatershedDataset.pits.pitDataList.add(i, firstPit);
//					WatershedDataset.pits.pitDataList.remove(0);
//					break;
//				}
//			}
//		}
////		post = System.currentTimeMillis();
////		System.out.print(Long.toString(post-pre) + ",");
//		return true;	
//	}

//	public boolean resolveFilledArea() {
//		Pit firstPit = WatershedDataset.pits.pitDataList.get(0);
//		List<Point> allPointsToResolve = new ArrayList<Point>();
//		List<Point> pointsToResolve = new ArrayList<Point>();
//		List<Point> pointsResolved = new ArrayList<Point>();
//		
//		// Adjust DEM elevations
//		for (int i = 0; i < firstPit.allPointsList.size(); i++) {
//			if (this.dem[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] < firstPit.spilloverElevation) {
//				this.dem[firstPit.allPointsList.get(i).y][firstPit.allPointsList.get(i).x] = firstPit.spilloverElevation;
//				allPointsToResolve.add(firstPit.allPointsList.get(i));
//			}
//		}
//		// Resolve flow direction to direct flow out of the pit
//		this.flowDirection[firstPit.pitOutletPoint.y][firstPit.pitOutletPoint.x].childPoint = firstPit.outletSpilloverFlowDirection;
//		pointsToResolve.add(firstPit.pitOutletPoint);
//		while (!pointsToResolve.isEmpty()) {
//			Point currentPoint = pointsToResolve.get(0);
//			pointsResolved.add(currentPoint);
//			pointsToResolve.remove(0);
//			for (int x = -1; x < 2; x++) {
//				for (int y = -1; y < 2; y++){
//					if (x == 0 && y == 0) {
//						continue;
//					}
//					Point neighborPoint = new Point(currentPoint.x + x, currentPoint.y + y);
//					// check if the point is part of the complete list to be resolved, but not already on the "next up" list
//					if (allPointsToResolve.contains(neighborPoint) && !pointsToResolve.contains(neighborPoint) && !pointsResolved.contains(neighborPoint)) {
//						this.flowDirection[neighborPoint.y][neighborPoint.x].childPoint = currentPoint;
//						pointsToResolve.add(neighborPoint);
//						allPointsToResolve.remove(neighborPoint);
//					}
//				}			
//			}
//		}
		
//		resolvedPointsToCheckForNeighbors.set(0, firstPit.pitOutletPoint);
//		logicMatrix[firstPit.pitOutletPoint.y][firstPit.pitOutletPoint.x] = true;
//		int indicesToResolveCounter = 1;
//		for (int i = 0; i < resolvedPointsToCheckForNeighbors.size(); i++) {
//			int r = resolvedPointsToCheckForNeighbors.get(0).y;
//			int c = resolvedPointsToCheckForNeighbors.get(0).x;
//			for (int x = -1; x < 2; x++) {
//				for (int y = -1; y < 2; y++){
//					if (x == 0 && y == 0) {
//						continue;
//					}
//					if (r+y >= this.dem.length-1 || r+y <= 0 || c+x >= this.dem[0].length-1 || c+x <= 0) {
//						continue;
//					}
//					if (logicMatrix[r+y][c+x] == null) {
//						continue;
//					}
//					if (!logicMatrix[r+y][c+x]) {
//						resolvedPointsToCheckForNeighbors.set(indicesToResolveCounter, new Point(c+x, r+y));
//						flowDirection[r+y][c+x].childPoint = resolvedPointsToCheckForNeighbors.get(0);
//						logicMatrix[r+y][c+x] = true; // The point is now on the list and resolved.  Do not add it anymore if it is a neighbor of a future cell.
//						indicesToResolveCounter++;
//					}
//				}
//			}
//		}
//		return true;
//	}

	public Bitmap delineate(Point point, AsyncTask task) {
		if(task instanceof DelineationListener) {
			delineationListener = (DelineationListener) task;
		} else {
			throw new ClassCastException("WatershedDataset - Task must implement DelineationListener");
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		
		Bitmap icon = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.watershedelineation, options);
		Bitmap delinBitmap = Bitmap.createScaledBitmap(icon, this.dem[0].length, this.dem.length, false);
		//skip outside cells
		for (int r = 1; r < this.dem.length-1; r++) {
			for (int c = 1; c < this.dem[0].length-1; c++) {
				delinBitmap.setPixel(this.dem[0].length - 1 - c, r, Color.TRANSPARENT);
			}
		}
		delineatedArea = 0; //number of cells in the delineation
//		delineatedStorageVolume = 0.0;

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
			if (delinBitmap.getPixel(this.dem[0].length - 1 - c, r) == Color.RED){
				continue;
			}
			delinBitmap.setPixel(this.dem[0].length - 1 - c, r, Color.RED);
			delineatedArea++;
//			delineatedStorageVolume += dem[r][c] - originalDem[r][c];
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					if (r+y >= this.dem.length-1 || r+y <= 0 || c+x >= this.dem[0].length-1 || c+x <= 0) {
						continue;
					}
					if (dem[r+y][c+x] == puddleElevation && delinBitmap.getPixel(this.dem[0].length - 1 - (c+x), (r+y)) != Color.RED) {
						indicesToCheckPuddle.add(new Point(c+x, r+y));
						indicesToCheck.add(new Point(c+x, r+y));
					}
				}
			}
		}

		// Add a buffer around the chosen pixel to provide a more likely meaningful delineation
		for (int x = -3; x < 4; x++) {
			for (int y = -3; y < 4; y++) {
				if (point.y+y > this.dem.length-1 || point.y+y < 0 || point.x+x > this.dem[0].length-1 || point.x+x < 0) {
					continue;
				}
				if (delinBitmap.getPixel(this.dem[0].length - 1 - (point.x+x), (point.y+y)) != Color.RED) {
					indicesToCheck.add(new Point(x+point.x, y +point.y));
					delinBitmap.setPixel(this.dem[0].length - 1 - (point.x+x), (point.y+y), Color.RED);
					delineatedArea++;
//					delineatedStorageVolume += dem[point.y+y][point.x+x] - originalDem[point.y+y][point.x+x];
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
				if (delinBitmap.getPixel(this.dem[0].length - 1 - flowDirection[r][c].parentList.get(i).x, flowDirection[r][c].parentList.get(i).y) != Color.RED) {
					indicesToCheck.add(flowDirection[r][c].parentList.get(i));
					delinBitmap.setPixel(this.dem[0].length - 1 - flowDirection[r][c].parentList.get(i).x, flowDirection[r][c].parentList.get(i).y, Color.RED);
					delineatedArea++;
//					delineatedStorageVolume += dem[r][c] - originalDem[r][c];
				}
			}
		}
		return delinBitmap;
	}
	
	public Bitmap altDrawPits() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		Bitmap icon = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.watershedelineation, options);
		Bitmap pitsBitmap = Bitmap.createScaledBitmap(icon, dem[0].length, dem.length, false);
		Random random = new Random();
		for (int i = 0; i < WatershedDataset.pits.pitDataList.size(); i++) {
			int red = random.nextInt(255);
			int green = random.nextInt(255);
			int blue = random.nextInt(255);
			int pitColor = Color.rgb(red,green,blue);
			List<Point> indicesToCheck = new ArrayList<Point>(WatershedDataset.pits.pitDataList.get(i).area);
//			List<Point> indicesToCheck = new ArrayList<Point>(WatershedDataset.pits.pitDataList.get(i).allPointsList.size());
			indicesToCheck.add(WatershedDataset.pits.pitDataList.get(i).pitPoint);
			
			while (!indicesToCheck.isEmpty()) {
				int r = indicesToCheck.get(0).y;
				int c = indicesToCheck.get(0).x;
				pitsBitmap.setPixel(this.dem[0].length - 1 - c, r, pitColor);
				indicesToCheck.remove(0);
				if (flowDirection[r][c].parentList.isEmpty()) {
					continue;
				}
				
				for (int j = 0; j < flowDirection[r][c].parentList.size(); j++) {
					
					indicesToCheck.add(flowDirection[r][c].parentList.get(j));
				}
			}
		}
		return pitsBitmap;		
	}
	
//	public Bitmap drawPits() {
//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inPurgeable = true;
//		options.inInputShareable = true;
//		Bitmap icon = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.watershedelineation, options);
//		Bitmap pitsBitmap = Bitmap.createScaledBitmap(icon, dem[0].length, dem.length, false);
//		Random random = new Random();
//		for (int i = 0; i < WatershedDataset.pits.pitDataList.size(); i++) {
//			int red = random.nextInt(255);
//			int green = random.nextInt(255);
//			int blue = random.nextInt(255);
//			int pitColor = Color.rgb(red,green,blue);
//			for (int j = 0; j < WatershedDataset.pits.pitDataList.get(i).allPointsList.size(); j++) {
//				pitsBitmap.setPixel(this.dem[0].length - 1 - WatershedDataset.pits.pitDataList.get(i).allPointsList.get(j).x, WatershedDataset.pits.pitDataList.get(i).allPointsList.get(j).y, pitColor);
//			}
//		}
//		return pitsBitmap;		
//	}

	public Bitmap drawPuddles() {
		int[] colorarray = new int[WatershedDataset.pits.pitIdMatrix.length*WatershedDataset.pits.pitIdMatrix[0].length];
		Arrays.fill(colorarray, Color.TRANSPARENT);
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		Bitmap puddleBitmap = Bitmap.createBitmap(colorarray, this.dem[0].length, this.dem.length, config);
		puddleBitmap = puddleBitmap.copy(config, true);

		for (int r = 1; r < this.dem.length-1; r++) {
			for (int c = 1; c < this.dem[0].length-1; c++) {
				if (r >= this.dem.length-1 || r <= 0 || c >= this.dem[0].length-1 || c <= 0) {
					continue;
				}
				if (originalDem[r][c] < dem[r][c]) {
					puddleBitmap.setPixel(this.dem[0].length - 1 - c, r, Color.BLUE);
				}
			}
		}
		return puddleBitmap;
	}
	
	public static void polygonize(String rasterFilePath, int[][] rasterData, String fileOutPath) {
		gdal.AllRegister();
		ogr.RegisterAll();
		Dataset demRaster = gdal.Open(rasterFilePath);
		
		
		// Transform from 2D array to 1D array
		int[] array = new int[rasterData.length * rasterData[0].length];
		int i = 0;
		for (int r = 0; r < rasterData.length; r++) {
			for (int c = 0; c < rasterData[0].length; c++) {
				array[i] = rasterData[r][rasterData[0].length - c - 1]; 
				i++;
			}
		}
		
		//mask the outer rows
		int[] mask = new int[rasterData.length * rasterData[0].length];
		i = 0;
		for (int r = 0; r < rasterData.length; r++) {
			for (int c = 0; c < rasterData[0].length; c++) {
				if ((r == 0) || (r == rasterData.length - 1) || (c == 0) || (c == rasterData[0].length - 1)) { 
					mask[i] = 0;
					i++;
				} else {
					mask[i] = 1;
				}
			}
		}
		
		//Create a new file that is a copy of the DEM geotiff so that the georeferencing data is identical and write the new band data to this file
		org.gdal.gdal.Driver rdriver = gdal.GetDriverByName("GTiff");
		Dataset catchmentRaster = rdriver.CreateCopy(fileOutPath, demRaster);
		catchmentRaster.WriteRaster(0, 0, catchmentRaster.getRasterXSize(), catchmentRaster.getRasterYSize(), catchmentRaster.getRasterXSize(), catchmentRaster.getRasterYSize(), gdalconstConstants.GDT_Int32, array, new int[]{1}, 0, 0, 0);
		catchmentRaster.AddBand(gdalconst.GDT_UInt16);
		catchmentRaster.WriteRaster(0, 0, catchmentRaster.getRasterXSize(), catchmentRaster.getRasterYSize(), catchmentRaster.getRasterXSize(), catchmentRaster.getRasterYSize(), gdalconstConstants.GDT_Int32, mask, new int[]{2}, 0, 0, 0);
		//TODO Test if this does anything useful
		catchmentRaster.FlushCache();
		
		// When creating this new datasource, the file must not already exist.
		Driver shpDriver = ogr.GetDriverByName("ESRI Shapefile");
		org.gdal.ogr.DataSource catchmentVector = shpDriver.CreateDataSource(fileOutPath+".shp");
		SpatialReference srs = new SpatialReference(catchmentRaster.GetProjection());
		Layer catchmentLayer = catchmentVector.CreateLayer("NewLayer", srs);
		gdal.Polygonize(catchmentRaster.GetRasterBand(1), catchmentRaster.GetRasterBand(2), catchmentLayer, 0);
		
		SpatialReference wgs = new SpatialReference("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]");
		FeatureDefn outFeatureDef = catchmentLayer.GetLayerDefn();
		// for each polygon
		float epsilon = (float) Math.sqrt(2*Math.pow(cellSize, 2))/2;
		for (int i1 = catchmentLayer.GetFeatureCount()-1; i1 > -1; i1--) {
			Feature inFeature = catchmentLayer.GetNextFeature();
//			Feature outFeature = new Feature(outFeatureDef);
			Geometry geometry = inFeature.GetGeometryRef();
//			float[][] points = new float[geometry.GetGeometryRef(0).GetPointCount()][2];
//			for (int j = 0; j < geometry.GetGeometryRef(0).GetPointCount(); j++) {
//				points[j][0] = (float) geometry.GetGeometryRef(0).GetPoint(j)[1];
//				points[j][0] = (float) geometry.GetGeometryRef(0).GetPoint(j)[0];
//			}
//			points = ramerDouglasPeucker(points, epsilon);
//			geometry.delete();
//			geometry.s
			Geometry geom = geometry.Simplify(epsilon);
			Log.w("geometry null", Boolean.toString(geom == null));
			inFeature.SetGeometryDirectly(geom);
//			Log.w("geometry null", Boolean.toString(geom == null));
//			int success = catchmentLayer.CreateFeature(inFeature);
//			Log.w("success?", Boolean.toString(success == 0));
//			catchmentLayer.DeleteFeature(i1);
		}
//		catchmentLayer.
		for (int i1 = 0; i1 < catchmentLayer.GetFeatureCount(); i1++) {
			Feature inFeature = catchmentLayer.GetNextFeature();
			Geometry geometry = inFeature.GetGeometryRef();
			Log.w("error check", gdal.GetLastErrorMsg());
			geometry.TransformTo(wgs);
			//		points = ramerDouglasPeucker(points, epsilon);
			List<LatLng> list = new ArrayList<LatLng>();
			for (int j = 0; j < geometry.GetGeometryRef(0).GetPointCount(); j++) {
				list.add(new LatLng(geometry.GetGeometryRef(0).GetPoint(j)[1], geometry.GetGeometryRef(0).GetPoint(j)[0]));
			}
			ATKPolygon poly = new ATKPolygon("test", list);
			MainActivity.map.addPolygon(poly);
			poly.viewOptions.setFillColor(Color.TRANSPARENT);
			poly.viewOptions.setStrokeColor(Color.RED);
			//		outFeature.SetGeometry(geometry);
			//		catchmentLayer.CreateFeature(outFeature);
	}
        
		catchmentVector.SyncToDisk();
		//TODO Test if this is what actually makes the file
		catchmentVector.SyncToDisk();
		
		
		
		demRaster.delete();
		catchmentRaster.delete();
		catchmentVector.delete();
		catchmentLayer.delete();
		
		demRaster = null;
		catchmentRaster = null;
		catchmentVector = null;
		catchmentLayer = null;
	}
	
//	public static void PolygonizeA() {
//		// Create GDAL Dataset, create a Band, and write the data to that Band
//		gdal.AllRegister();
//		ogr.RegisterAll();
//		String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/dem/";
//		String filepath = directory + "Feldun.tif";
//		Dataset dataset = gdal.Open(filepath);
//		org.gdal.gdal.Driver rdriver = gdal.GetDriverByName("GTiff");
//		
//		// Transform from 2D array to 1D array
//		int[] array = new int[pits.pitIdMatrix.length * pits.pitIdMatrix[0].length];
//		int i = 0;
//		for (int r = 0; r < pits.pitIdMatrix.length; r++) {
//			for (int c = 0; c < pits.pitIdMatrix[0].length; c++) {
//				array[i] = pits.pitIdMatrix[r][pits.pitIdMatrix[0].length - c - 1]; 
//				i++;
//			}
//		}
//		
//		//mask the outer rows
//		i = 0;
//		for (int r = 0; r < pits.pitIdMatrix.length; r++) {
//			for (int c = 0; c < pits.pitIdMatrix[0].length; c++) {
//				if ((r == 0) || (r == pits.pitIdMatrix.length - 1) || (c == 0) || (c == pits.pitIdMatrix[0].length - 1)) { 
//					array[i] = 0;
//					i++;
//				} else {
//					array[i] = 1;
//				}
//			}
//		}
//		
//		//write dataset containing desired raster data
//		String file = directory+"FCatchments.tif";
//		Dataset dswrite = rdriver.CreateCopy(file, dataset);
//		dswrite.WriteRaster(0, 0, dswrite.getRasterXSize(), dswrite.getRasterYSize(), dswrite.getRasterXSize(), dswrite.getRasterYSize(), gdalconstConstants.GDT_Int32, array, new int[]{1}, 0, 0, 0);
//		dswrite.FlushCache();
//		
//		//polygonize
//		Driver driver = ogr.GetDriverByName("ESRI Shapefile");
//		// When creating this datasource, the file must not already exist.  This check can be handled with android. 
//		org.gdal.ogr.DataSource ds = driver.CreateDataSource(directory + "s3.shp");
//		SpatialReference srs = new SpatialReference(dswrite.GetProjection());
//		layer = ds.CreateLayer("NewLayer", srs);
//		gdal.Polygonize(dswrite.GetRasterBand(1), null, layer, 0);
//		ds.SyncToDisk();
//	}
//	
//	public static void Polygonize(int[][] rasterData) {
//		gdal.AllRegister();
//		ogr.RegisterAll();
//		String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/dem/";
//		String filepath = directory + "Feldun.tif";
//		Dataset dataset = gdal.Open(filepath);
//		
//		Driver driver = ogr.GetDriverByName("ESRI Shapefile");
//		org.gdal.ogr.DataSource openDS = ogr.Open(directory + "s3.shp");
//		SpatialReference wgs = new SpatialReference("GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]");
//		SpatialReference srs = new SpatialReference(dataset.GetProjection());
//		org.gdal.ogr.DataSource newds = driver.CreateDataSource(directory + "s4.shp");
//		Layer lay = newds.CreateLayer("temp", wgs);
//		FeatureDefn outFeatureDef = lay.GetLayerDefn();
//		
//		// for each polygon
//		for (int i = 0; i < layer.GetFeatureCount(); i++) {
//			Feature inFeature = layer.GetNextFeature();
//			Feature outFeature = new Feature(outFeatureDef);
//			Geometry geometry = inFeature.GetGeometryRef();
//			geometry.TransformTo(wgs);
//			PolygonOptions polyOpts = new PolygonOptions().strokeColor(Color.RED).fillColor(Color.TRANSPARENT);
//			for (int j = 0; j < geometry.GetGeometryRef(0).GetPointCount(); j++) {
//				polyOpts.add(new LatLng(geometry.GetGeometryRef(0).GetPoint(j)[1], geometry.GetGeometryRef(0).GetPoint(j)[0]));
//				MainActivity.map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(geometry.GetGeometryRef(0).GetPoint(j)[1], geometry.GetGeometryRef(0).GetPoint(j)[0])));
//			}
////			MainActivity.map.addPolygon(polyOpts);
//			outFeature.SetGeometry(geometry);
//			lay.CreateFeature(outFeature);			
//		}
//		openDS.SyncToDisk();
//	}
		
	//karthaus.nl/rdp/js/rdp.js
	public static float[][] ramerDouglasPeucker(float[][] points, float epsilon){
	    float[] firstPoint = points[0];
	    float[] lastPoint = points[points.length-1];
	    if (points.length < 3){
	        return points;
	    }
	    
	    //Get intermediate point of maximum perpendicular distance to the straight line
	    int index=-1;
	    float dist=0;
	    for (int i = 1; i < points.length-1; i++){
	        float cDist = findPerpendicularDistance(points[i],firstPoint,lastPoint);
	        if (cDist>dist){
	            dist=cDist;
	            index=i;
	        }
	    }
	    
	    //
	    if (dist > epsilon){
	        // iterate
	        float[][] l1 = Arrays.copyOfRange(points, 0, index + 1);
	        float[][] l2 = Arrays.copyOfRange(points, index, points.length - 1);
	        float[][] r1 = ramerDouglasPeucker(l1,epsilon);
	        float[][] r2 = ramerDouglasPeucker(l2,epsilon);
	        // concat r2 to r1 minus the end/startpoint that will be the same
	        float[][] rs= new float[2][r1.length + r2.length - 1];
	        System.arraycopy(r1, 0, rs, 0, r1.length);
	        System.arraycopy(r2, 1, rs, r1.length, r2.length-1); //skip first point of r2 as it is a duplicate of the last point of r1
	        return rs;
	    }else{
	        return new float[][]{firstPoint, lastPoint};
	    }
	}
	    
	    
	public static float findPerpendicularDistance(float[] p, float[] p1, float[] p2) {
	    // if start and end point are on the same x the distance is the difference in X.
	    float result;
	    float slope;
	    float intercept;
	    if (p1[0]==p2[0]){
	        result=Math.abs(p[0]-p1[0]);
	    }else{
	        slope = (p2[1] - p1[1]) / (p2[0] - p1[0]);
	        intercept = p1[1] - (slope * p1[0]);
	        result = (float) (Math.abs(slope * p[0] - p[1] + intercept) / Math.sqrt(Math.pow(slope, 2) + 1));
	    }
	   
	    return result;
	}
}
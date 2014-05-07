package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.List;
import org.waterapps.watershed.WatershedDataset.WatershedDatasetListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;

public class PitRaster {
	// bitmap represents rasterized elevation data
//	public Bitmap pitsBitmap = null;
	List<Pit> pitDataList;
	int[][] pitIdMatrix;
	int numrows;
	int numcols;
	private WatershedDatasetListener listener;
	static int status = 40;
	private float[][] dem;
	FlowDirectionCell[][] flowDirection;
	float cellSize;
	int maxPitId = 1; // positive pits need to be filled
	int minPitId = -1; // negative pits are connected to the edge of 

	// constructor method
	public PitRaster(float[][] dem, float[][] drainage,FlowDirectionCell[][] flowDirection, float inputCellSize, WatershedDatasetListener listener) {
		this.dem = dem;
		this.flowDirection = flowDirection;
		this.cellSize = inputCellSize;
		this.listener = listener;		
	}
	
//	public void constructPitRaster(int pitCellCount) {
//		numrows = flowDirection.length;
//		numcols = flowDirection[0].length;
//		pitIdMatrix = new int[numrows][numcols];
//		pitDataList = new ArrayList<Pit>(pitCellCount + (numrows*2) + (numcols*2) - 4);
//		maxPitId = -1;
//		
//		//identify catchments raster
//		
//		for (int c = 0; c < numcols; c++) {
//			for (int r = 0; r < numrows; r++) {
//				// Edge cells were marked with a null flow direction child.  Identify each edge cell and 
//				// those cells that flow to it as a unique pit with a negative id.  
//				if (flowDirection[r][c].childPoint == null) {
//					minPitId--;
//					Point pitPoint = new Point(c, r);
//					List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitPoint, minPitId);
//					Pit currentPit = new Pit(indicesDrainingToPit, minPitId, pitPoint);
//					pitDataList.add(currentPit);
//				} else if (flowDirection[r][c].childPoint.y < 0) {
//					maxPitId++;
//					Point pitPoint = new Point(c, r);
//					List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitPoint, maxPitId);
//					Pit currentPit = new Pit(indicesDrainingToPit, maxPitId, pitPoint);
//					pitDataList.add(currentPit);
//				}
//			}
//			status = (int) (40 + (25 * ((((c*numrows)))/((float) numrows*numcols))));
//			listener.simulationOnProgress(status, "Locating Surface Depressions");
//		}
//
//		// After identifying the pits matrix, gather pit data for each pit
//		for (int i = 0; i < pitDataList.size(); i++) {
//			if (pitDataList.get(i).pitId > -1){
//				pitDataList.get(i).completePitConstruction(null, cellSize, dem, pitIdMatrix);
//			} else {
//				pitDataList.get(i).completeNegativePitConstruction(null, cellSize, dem, pitIdMatrix);
//			}
//			status = (int) (65 + (25 * (i/(float)pitDataList.size())));
//			listener.simulationOnProgress(status, "Computing Surface Depression Dimensions");
//		}
//	}
	
	public void constructPitRaster(int pitCellCount) {
		numrows = flowDirection.length;
		numcols = flowDirection[0].length;
		pitIdMatrix = new int[numrows][numcols];
		pitDataList = new ArrayList<Pit>(pitCellCount + (numrows*2) + (numcols*2) - 4);
		
		//identify catchments raster
		
		for (int c = 0; c < numcols; c++) {
			for (int r = 0; r < numrows; r++) {
				// Edge cells were marked with a null flow direction child.  Identify each edge cell and 
				// those cells that flow to it as a unique pit with a negative id.  
				if (flowDirection[r][c].childPoint == null) {
					Point pitPoint = new Point(c, r);
					Pit currentPit = new Pit(minPitId, pitPoint);
					pitDataList.add(currentPit);
					minPitId--;
				} else if (flowDirection[r][c].childPoint.y < 0) {
					Point pitPoint = new Point(c, r);
					Pit currentPit = new Pit(maxPitId, pitPoint);
					pitDataList.add(currentPit);
					maxPitId++;
				}
			}
			status = (int) (40 + (25 * ((((c*numrows)))/((float) numrows*numcols))));
			listener.simulationOnProgress(status, "Locating Surface Depressions");
		}

		// After identifying the pits matrix, gather pit data for each pit
		for (int i = 0; i < pitDataList.size(); i++) {
			if (pitDataList.get(i).pitId > -1){
				List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitDataList.get(i).pitPoint, pitDataList.get(i).pitId);
				pitDataList.get(i).completePitConstruction(indicesDrainingToPit, null, cellSize, dem, pitIdMatrix);
			} else {
				List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitDataList.get(i).pitPoint, pitDataList.get(i).pitId);
				pitDataList.get(i).completeNegativePitConstruction(indicesDrainingToPit, null, cellSize, dem, pitIdMatrix);
			}
			status = (int) (65 + (25 * (i/(float)pitDataList.size())));
			listener.simulationOnProgress(status, "Computing Surface Depression Dimensions");
		}
	}
	
	public List<Point> findCellsDrainingToPoint(FlowDirectionCell[][] flowDirection, Point pitPoint, int pitId) {
		List<Point> indicesDrainingToPit = new ArrayList<Point>();
		indicesDrainingToPit.add(pitPoint);
		List<Point> indicesToCheck = new ArrayList<Point>();
		indicesToCheck.add(indicesDrainingToPit.get(0));
		while (!indicesToCheck.isEmpty()) {
			int r = indicesToCheck.get(0).y;
			int c = indicesToCheck.get(0).x;
			pitIdMatrix[r][c] = pitId;
			indicesToCheck.remove(0);

			if (flowDirection[r][c].parentList.isEmpty()) {
				continue;
			}
			for (int i = 0; i < flowDirection[r][c].parentList.size(); i++) {
				indicesDrainingToPit.add(flowDirection[r][c].parentList.get(i));
				indicesToCheck.add(flowDirection[r][c].parentList.get(i));
			}
		}
		return indicesDrainingToPit;
	}

	public int getIndexOf(int inputPitID) {
		for (int i = 0; i < pitDataList.size(); i++) {
			if (pitDataList.get(i).pitId == inputPitID) {
				return i;
			}
		}
		int pitListIndex = -1;
		return pitListIndex;
	}

	public Bitmap highlightSelectedPit(int selectedPitIndex) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		Bitmap icon = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.watershedelineation, options);
		Bitmap pitsBitmap = Bitmap.createScaledBitmap(icon, this.pitIdMatrix[0].length, this.pitIdMatrix.length, false);
		Bitmap highlightedPitBitmap = Bitmap.createBitmap(pitsBitmap);;
		Pit selectedPit = this.pitDataList.get(selectedPitIndex);
		for (int i = 0; i < selectedPit.pitBorderIndicesList.size(); i++) {
			highlightedPitBitmap.setPixel(pitIdMatrix[0].length - 1 - selectedPit.pitBorderIndicesList.get(i).x, selectedPit.pitBorderIndicesList.get(i).y, Color.BLACK);
		}
		return highlightedPitBitmap;
	}	
}
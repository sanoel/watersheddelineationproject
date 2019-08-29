package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.waterapps.watershed.WatershedDataset.WatershedDatasetListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

public class PitRaster {
	Bitmap catchmentsBitmap;
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
	public PitRaster(WatershedDataset watershedDataset, WatershedDatasetListener listener) { //float[][] dem, float[][] drainage, FlowDirectionCell[][] flowDirection, float inputCellSize, 
		this.dem = watershedDataset.getDem();
		this.flowDirection = watershedDataset.getFlowDirection();
		this.cellSize = watershedDataset.getCellSize();
		this.listener = watershedDataset.listener;		
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
		for (int c = numcols - 1; c > -1; c--) {
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
			status = (int) (40 + (25 * ((((((numcols - 1) - c)*numrows)))/((float) numrows*numcols))));
			listener.simulationOnProgress(status, "Locating Surface Depressions");
		}
		Log.w("max pit ID", Integer.toString(maxPitId - 1));

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
	
	public void drawCatchments(Context context) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.watershedelineation, options);
		catchmentsBitmap = Bitmap.createScaledBitmap(icon, dem[0].length, dem.length, false);
		Random random = new Random();
		for (int i = 0; i < this.pitDataList.size(); i++) {
			int red = random.nextInt(255);
			int green = random.nextInt(255);
			int blue = random.nextInt(255);
			int pitColor = Color.rgb(red,green,blue);
			List<Point> indicesToCheck = new ArrayList<Point>(this.pitDataList.get(i).area);
			indicesToCheck.add(this.pitDataList.get(i).pitPoint);
			
			while (!indicesToCheck.isEmpty()) {
				int r = indicesToCheck.get(0).y;
				int c = indicesToCheck.get(0).x;
				catchmentsBitmap.setPixel(this.dem[0].length - 1 - c, r, pitColor);
				indicesToCheck.remove(0);
				if (flowDirection[r][c].parentList.isEmpty()) {
					continue;
				}
				
				for (int j = 0; j < flowDirection[r][c].parentList.size(); j++) {
					
					indicesToCheck.add(flowDirection[r][c].parentList.get(j));
				}
			}
		}
	}

	public Bitmap selectCatchment(int selectedCatchmentIndex) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		Bitmap selectedCatchmentBitmap = Bitmap.createBitmap(catchmentsBitmap);
		Pit selectedCatchment = this.pitDataList.get(selectedCatchmentIndex);
		for (int i = 0; i < selectedCatchment.pitBorderIndicesList.size(); i++) {
			selectedCatchmentBitmap.setPixel(pitIdMatrix[0].length - 1 - selectedCatchment.pitBorderIndicesList.get(i).x, selectedCatchment.pitBorderIndicesList.get(i).y, Color.BLACK);
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					if (selectedCatchment.pitBorderIndicesList.get(i).y+y <= 0 || selectedCatchment.pitBorderIndicesList.get(i).y+y >= this.numrows || 
						selectedCatchment.pitBorderIndicesList.get(i).x+x <= 0 || selectedCatchment.pitBorderIndicesList.get(i).x+x >= this.numcols) {
						continue;
					} else if (pitIdMatrix[selectedCatchment.pitBorderIndicesList.get(i).y+y][selectedCatchment.pitBorderIndicesList.get(i).x+x] != selectedCatchment.pitId) {
						selectedCatchmentBitmap.setPixel((pitIdMatrix[0].length - 1) - (selectedCatchment.pitBorderIndicesList.get(i).x + x), selectedCatchment.pitBorderIndicesList.get(i).y + y, Color.WHITE);
					}
				}
			}
		}
		return selectedCatchmentBitmap;
	}	
}
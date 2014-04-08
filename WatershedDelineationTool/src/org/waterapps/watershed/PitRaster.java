package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.waterapps.watershed.WatershedDataset.WatershedDatasetListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

public class PitRaster {
	// bitmap represents rasterized elevation data
	public Bitmap pitsBitmap = null;
	List<Pit> pitDataList;
	int[][] pitIdMatrix;
	int numrows;
	int numcols;
	private WatershedDatasetListener listener;
	static int status = 40;
	private float[][] dem;
	FlowDirectionCell[][] flowDirection;
	float inputCellSizeX;
	float inputCellSizeY;
	int maxPitId;
	int minPitId;

	// constructor method
	public PitRaster(float[][] dem, float[][] drainage,FlowDirectionCell[][] flowDirection, float inputCellSizeX, float inputCellSizeY, WatershedDatasetListener listener) {
		this.dem = dem;
		this.flowDirection = flowDirection;
		this.inputCellSizeX = inputCellSizeX;
		this.inputCellSizeY = inputCellSizeY;
		this.listener = listener;		
	}
	
	public void constructPitRaster(int pitCellCount) {
		numrows = flowDirection.length;
		numcols = flowDirection[0].length;
		pitIdMatrix = new int[numrows][numcols];
		pitDataList = new ArrayList<Pit>(pitCellCount + (numrows*2) + (numcols*2) - 4);
		maxPitId = -1;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		Bitmap icon = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.watershedelineation, options);
		pitsBitmap = Bitmap.createScaledBitmap(icon, numcols, numrows, false);
		
		//identify catchments raster
//		Random random = new Random();
		for (int c = 0; c < numcols; c++) {
			for (int r = 0; r < numrows; r++) {
				// Edge cells were marked with a null flow direction child.  Identify each edge cell and 
				// those cells that flow to it as a unique pit with a negative id.  
				if (flowDirection[r][c].childPoint == null) {
					minPitId--;
					Point pitPoint = new Point(c, r);
//					int red = random.nextInt(255);
//					int green = random.nextInt(255);
//					int blue = random.nextInt(255);
//					int pitColor = Color.rgb(red,green,blue);
//					List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitPoint, minPitId, pitColor);
					List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitPoint, minPitId);
//					Pit currentPit = new Pit(indicesDrainingToPit, pitColor, minPitId);
					Pit currentPit = new Pit(indicesDrainingToPit, minPitId);
					pitDataList.add(currentPit);
				} else if (flowDirection[r][c].childPoint.y < 0) {
					maxPitId++;
					Point pitPoint = new Point(c, r);
//					int red = random.nextInt(255);
//					int green = random.nextInt(255);
//					int blue = random.nextInt(255);
//					int pitColor = Color.rgb(red,green,blue);
//					List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitPoint, maxPitId, pitColor);
					List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitPoint, maxPitId);
//					Pit currentPit = new Pit(indicesDrainingToPit, pitColor, maxPitId);
					Pit currentPit = new Pit(indicesDrainingToPit, maxPitId);
					pitDataList.add(currentPit);
				}
			}
			status = (int) (40 + (25 * ((((c*numrows)))/((float) numrows*numcols))));
			listener.simulationOnProgress(status, "Locating Surface Depressions");
		}

		// After identifying the pits matrix, gather pit data for each pit
		for (int i = 0; i < pitDataList.size(); i++) {
			if (pitDataList.get(i).pitId > -1){
				pitDataList.get(i).completePitConstruction(null, inputCellSizeX, inputCellSizeY, dem, pitIdMatrix);
			} else {
				pitDataList.get(i).completeNegativePitConstruction(null, inputCellSizeX, inputCellSizeY, dem, pitIdMatrix);
			}
			status = (int) (65 + (25 * (i/(float)pitDataList.size())));
			listener.simulationOnProgress(status, "Computing Surface Depression Dimensions");
		}
	}
//	public List<Point> findCellsDrainingToPoint(FlowDirectionCell[][] flowDirection, Point pitPoint, int pitId, int pitColor) {
	public List<Point> findCellsDrainingToPoint(FlowDirectionCell[][] flowDirection, Point pitPoint, int pitId) {
		List<Point> indicesDrainingToPit = new ArrayList<Point>();
		indicesDrainingToPit.add(pitPoint);
		List<Point> indicesToCheck = new ArrayList<Point>();
		indicesToCheck.add(indicesDrainingToPit.get(0));
		while (!indicesToCheck.isEmpty()) {
			int r = indicesToCheck.get(0).y;
			int c = indicesToCheck.get(0).x;
			pitIdMatrix[r][c] = pitId;
//			pitsBitmap.setPixel(pitIdMatrix[0].length - 1 - c, r, pitColor);
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
		Bitmap highlightedPitBitmap = Bitmap.createBitmap(pitsBitmap);;
		Pit selectedPit = this.pitDataList.get(selectedPitIndex);
		for (int i = 0; i < selectedPit.pitBorderIndicesList.size(); i++) {
			highlightedPitBitmap.setPixel(pitIdMatrix[0].length - 1 - selectedPit.pitBorderIndicesList.get(i).x, selectedPit.pitBorderIndicesList.get(i).y, Color.BLACK);
		}
		return highlightedPitBitmap;
	}	
}
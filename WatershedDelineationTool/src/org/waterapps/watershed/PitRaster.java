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
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				pitIdMatrix[r][c] = -1;
			}
		}
		pitDataList = new ArrayList<Pit>(pitCellCount);
		maxPitId = -1;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		
		Bitmap icon = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.watershedelineation, options);
		pitsBitmap = Bitmap.createScaledBitmap(icon, numcols, numrows, false);
		
//		Bitmap.Config config = Bitmap.Config.ARGB_8888;
//		pitsBitmap = Bitmap.createBitmap(numcols, numrows, config);
		for (int c = numcols-1; c > -1; c--) {
			for (int r = 0; r < numrows; r++) {
				// Boundary cells and those pits along the edge that do not
				// belong to a pit because they flow off the edge and not into a
				// pit should be marked with a negative pit ID.
				if (r == numrows - 1 || r == 0 || c == numcols - 1 || c == 0) {
					continue;
				}
				if (flowDirection[r][c].childPoint.y < 0) {
					maxPitId++;
					Point pitPoint = new Point(c, r);
					Random random = new Random();
					int red = random.nextInt(255);
					int green = random.nextInt(255);
					int blue = random.nextInt(255);
					int pitColor = Color.rgb(red,green,blue);
					List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, pitPoint, maxPitId, pitColor);
					Pit currentPit = new Pit(indicesDrainingToPit, pitColor, maxPitId);
					pitDataList.add(currentPit);
				}
			}
			status = (int) (40 + (25 * ((((numrows*numcols)-(c*numrows)))/((double) numrows*numcols))));
			listener.watershedDatasetOnProgress(status, "Locating Surface Depressions", null);
		}

		// After identifying the pits matrix, gather pit data for each pit
		for (int i = 0; i < pitDataList.size(); i++) {
			pitDataList.get(i).completePitConstruction(null, inputCellSizeX, inputCellSizeY, dem, pitIdMatrix);
			status = (int) (65 + (25 * (i/(double)pitDataList.size())));
			listener.watershedDatasetOnProgress(status, "Computing Surface Depression Dimensions", null);
		}
		identifyEdgePits();
	}

	public void identifyEdgePits() {
		int numrows = flowDirection.length;
		int numcols = flowDirection[0].length;

		Random random = new Random();
		int red;
		int green;
		int blue;
		int pitColor;

		//left side
		for (int r = 0; r < numrows; r++) {
			minPitId--;
			random = new Random();
			red = random.nextInt(255);
			green = random.nextInt(255);
			blue = random.nextInt(255);
			pitColor = Color.rgb(red,green,blue);
			
			List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, new Point(0, r), minPitId, pitColor);
			Pit currentPit = new Pit(indicesDrainingToPit, pitColor, minPitId);
			currentPit.completeNegativePitConstruction(null, inputCellSizeX, inputCellSizeY, dem, pitIdMatrix);
			pitDataList.add(currentPit);
		}

		//bottom side
		for (int c = 1; c < numcols-2; c++) { // 1 cell inside of DEM borders to avoid overlap of going down the rows on either side
			minPitId--;
			red = random.nextInt(255);
			green = random.nextInt(255);
			blue = random.nextInt(255);
			pitColor = Color.rgb(red,green,blue);
			
			List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, new Point(c, numrows-1), minPitId, pitColor);
			Pit currentPit = new Pit(indicesDrainingToPit, pitColor, minPitId);
			currentPit.completeNegativePitConstruction(null, inputCellSizeX, inputCellSizeY, dem, pitIdMatrix);
			pitDataList.add(currentPit);
		}

		//right side
		for (int r = 0; r < numrows; r++) {
			minPitId--;
			red = random.nextInt(255);
			green = random.nextInt(255);
			blue = random.nextInt(255);
			pitColor = Color.rgb(red,green,blue);
			
			List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, new Point(numcols-1, r), minPitId, pitColor);
			Pit currentPit = new Pit(indicesDrainingToPit, pitColor, minPitId);
			currentPit.completeNegativePitConstruction(null, inputCellSizeX, inputCellSizeY, dem, pitIdMatrix);
			pitDataList.add(currentPit);
		}

		//top side
		for (int c = 1; c < numcols-2; c++) { // 1 cell inside of DEM borders to avoid overlap of 
			minPitId--;
			red = random.nextInt(255);
			green = random.nextInt(255);
			blue = random.nextInt(255);
			pitColor = Color.rgb(red,green,blue);
			
			List<Point> indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, new Point(c, 0), minPitId, pitColor);
			Pit currentPit = new Pit(indicesDrainingToPit, pitColor, minPitId);
			currentPit.completeNegativePitConstruction(null, inputCellSizeX, inputCellSizeY, dem, pitIdMatrix);
			pitDataList.add(currentPit);			
		}		
	}

	public List<Point> findCellsDrainingToPoint(FlowDirectionCell[][] flowDirection, Point pitPoint, int pitId, int pitColor) {
		List<Point> indicesDrainingToPit = new ArrayList<Point>();
		indicesDrainingToPit.add(pitPoint);
		List<Point> indicesToCheck = new ArrayList<Point>();
		indicesToCheck.add(indicesDrainingToPit.get(0));
		while (!indicesToCheck.isEmpty()) {
			int r = indicesToCheck.get(0).y;
			int c = indicesToCheck.get(0).x;
			pitIdMatrix[r][c] = pitId;
			pitsBitmap.setPixel(pitIdMatrix[0].length - 1 - c, r, pitColor);
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
		//pass in maps object to draw on the map
		//convert from cartesian to 
		int[] colorarray = new int[this.pitIdMatrix.length*this.pitIdMatrix[0].length];
		Arrays.fill(colorarray, Color.TRANSPARENT);
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		pitsBitmap = Bitmap.createBitmap(colorarray, numcols, numrows, config);
		pitsBitmap = pitsBitmap.copy(config, true);
		for (int i = 0; i < this.pitDataList.size(); i++) {
			Pit currentPit = this.pitDataList.get(i);
			for (int j = 0 ; j < currentPit.pitBorderIndicesList.size(); j++) {
				pitsBitmap.setPixel(pitIdMatrix[0].length - 1 - currentPit.pitBorderIndicesList.get(j).x, currentPit.pitBorderIndicesList.get(j).y, currentPit.color);
			}
		}
		return pitsBitmap;
	}	
}
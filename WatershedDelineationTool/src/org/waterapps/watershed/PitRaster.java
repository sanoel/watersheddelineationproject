package org.waterapps.watershed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.waterapps.watershed.WatershedDataset.WatershedDatasetListener;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

public class PitRaster {
	// bitmap represents rasterized elevation data
	Bitmap pitsBitmap = null;
	List<Point> pitPointList = new ArrayList<Point>();
	List<Pit> pitDataList;
	int[][] pitIDMatrix;
	int numrows;
	int numcols;
	private WatershedDatasetListener listener;
	static int status = 40;
	static boolean pits_visibility = true;
	public static float alpha;

	// constructor method
	public PitRaster(float[][] dem, float[][] drainage,FlowDirectionCell[][] flowDirection, float inputCellSizeX, float inputCellSizeY, WatershedDatasetListener listener) {
		this.listener = listener;
		numrows = flowDirection.length;
		numcols = flowDirection[0].length;
		pitIDMatrix = new int[numrows][numcols];
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				pitIDMatrix[r][c] = -1;
			}
		}
		int pitIDCounter = 0;
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				// Boundary cells and those pits along the edge that do not
				// belong to a pit because they flow off the edge and not into a
				// pit should be marked as pitID -1.
				if (r == numrows - 1 || r == 0 || c == numcols - 1 || c == 0) {
					continue;
				}
				if (flowDirection[r][c].childPoint.y < 0) {
					Point pitPoint = new Point(c, r);
					pitPointList.add(pitPoint);
					List<Point> indicesDrainingToPit = new ArrayList<Point>();
					indicesDrainingToPit.add(pitPoint);
					indicesDrainingToPit = findCellsDrainingToPoint(flowDirection, indicesDrainingToPit);
					for (int i = 0; i < indicesDrainingToPit.size(); i++) {
						int rowidx = indicesDrainingToPit.get(i).y;
						int colidx = indicesDrainingToPit.get(i).x;
						pitIDMatrix[rowidx][colidx] = pitIDCounter;
					}
					pitIDCounter++;
				}
			}
			status = (int) (40 + (25 * (((r*numcols))/((double) numrows*numcols))));
			listener.watershedDatasetOnProgress(status, "Locating Surface Depressions");
		}

		// After identifying the pits matrix, gather pit data for each pit
		List<Long> pitTimes = new ArrayList<Long>(pitPointList.size());
		pitDataList = new ArrayList<Pit>(pitPointList.size());
		for (int i = 0; i < pitPointList.size(); i++) {
			Pit currentPit = new Pit(drainage, inputCellSizeX, inputCellSizeY, dem, flowDirection, pitIDMatrix, pitPointList.get(i), i);
			pitDataList.add(currentPit);
			status = (int) (65 + (25 * (i/(double)pitPointList.size())));
			listener.watershedDatasetOnProgress(status, "Computing Surface Depression Dimensions");
		}
		long sum = 0;
		for (Long pitTime : pitTimes) {
			sum += pitTime;
		}
		
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		pitsBitmap = Bitmap.createBitmap(numcols, numrows, config);
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				if (pitIDMatrix[r][c]== -1) {
					pitsBitmap.setPixel(c, r, Color.TRANSPARENT);
					continue;
				}
				int currentPitColor = pitDataList.get(pitIDMatrix[r][c]).color;
				pitsBitmap.setPixel(c, r, currentPitColor);
			}
			status = (int) (90 + (10 * (((r*numcols))/((double) numrows*numcols))));
			listener.watershedDatasetOnProgress(status, "Gathering Surface Storage Data");
		}
	}

	public List<Point> findCellsDrainingToPoint(FlowDirectionCell[][] flowDirection, List<Point> indicesDrainingToPit) {
		List<Point> indicesToCheck = new ArrayList<Point>();
		indicesToCheck.add(indicesDrainingToPit.get(0));
		while (!indicesToCheck.isEmpty()) {
			int r = indicesToCheck.get(0).y;
			int c = indicesToCheck.get(0).x;
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
			if (pitDataList.get(i).pitID == inputPitID) {
				return i;
			}
		}
		int pitListIndex = -1;
		return pitListIndex;
	}

	public int getMaximumPitID() {
		int MaxPitID = -1;
		for (int i = 0; i < pitDataList.size(); i++) {
			if (pitDataList.get(i).pitID > MaxPitID) {
				MaxPitID = pitDataList.get(i).pitID;
			}
		}

		return MaxPitID;
	}

	public Bitmap updatePitsBitmap() {
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		pitsBitmap = Bitmap.createBitmap(numcols, numrows, config);
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				if (r >= numrows - 1 || r <= 0 || c >= numcols - 1 || c <= 0) {
					pitsBitmap.setPixel(numcols - 1 - c, r, Color.TRANSPARENT);
					continue;
				}
				// verify that pitID exists
				if (this.getIndexOf(pitIDMatrix[r][c]) == -1) {
					int currentPitColor = Color.TRANSPARENT;
					pitsBitmap.setPixel(numcols - 1 - c, r, currentPitColor);
				} else {
					int currentPitColor = pitDataList.get(this.getIndexOf(pitIDMatrix[r][c])).color;
					pitsBitmap.setPixel(numcols - 1 - c, r, currentPitColor);
				}
			}
		}
		return pitsBitmap;
	}
	
	public Bitmap updatePitsBitmapOutlines() {
		//pass in maps object to draw on the map
		//convert from cartesian to 
		int[] colorarray = new int[this.pitIDMatrix.length*this.pitIDMatrix[0].length];
		Arrays.fill(colorarray, Color.TRANSPARENT);
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		pitsBitmap = Bitmap.createBitmap(colorarray, numcols, numrows, config);
		pitsBitmap = pitsBitmap.copy(config, true);
		for (int i = 0; i < this.pitDataList.size(); i++) {
			Pit currentPit = this.pitDataList.get(i);
			for (int j = 0 ; j < currentPit.pitBorderIndicesList.size(); j++) {
				pitsBitmap.setPixel(currentPit.pitBorderIndicesList.get(j).x, currentPit.pitBorderIndicesList.get(j).y, currentPit.color);
			}
		}
		return pitsBitmap;
	}
	
	public static void setAlpha(float a) {
		alpha = a;
	}
	
}
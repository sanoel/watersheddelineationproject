package com.precisionag.watersheddelineationtool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;

public class PitRaster {
	// bitmap represents rasterized elevation data
	Bitmap pitsBitmap = null;
	List<Point> pitPointList = new ArrayList<Point>();
	List<Pit> pitDataList;
	int[][] pitIDMatrix;
	int numrows;
	int numcols;

	// constructor method
	public PitRaster(double[][] DEM, double[][] drainage,FlowDirectionCell[][] flowDirection, BigDecimal cellSize, BigDecimal rainfallIntensity) {
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
		}

		// After identifying the pits matrix, gather pit data for each pit
		pitDataList = new ArrayList<Pit>(pitPointList.size());
		for (int i = 0; i < pitPointList.size(); i++) {
//			Log.w("PitRas-input pitid", Integer.toString(pitIdx));
			Pit currentPit = new Pit(drainage, cellSize, DEM, flowDirection, pitIDMatrix, pitPointList.get(i), i,	rainfallIntensity);
			pitDataList.add(currentPit);
		}
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		pitsBitmap = Bitmap.createBitmap(numcols, numrows, config);
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				if (pitIDMatrix[r][c]== -1) {
					pitsBitmap.setPixel(c, r, Color.BLACK);
					continue;
				}
				int currentPitColor = pitDataList.get(pitIDMatrix[r][c]).pitColor;
				pitsBitmap.setPixel(c, r, currentPitColor);
			}
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
				if (r == numrows - 1 || r == 0 || c == numcols - 1 || c == 0) {
					pitsBitmap.setPixel(c, r, Color.BLACK);
					continue;
				}
				// verify that pitID exists (
				if (this.getIndexOf(pitIDMatrix[r][c]) == -1) {
					int currentPitColor = Color.BLACK;
					pitsBitmap.setPixel(c, r, currentPitColor);
				} else {
					int currentPitColor = pitDataList.get(this.getIndexOf(pitIDMatrix[r][c])).pitColor;
					pitsBitmap.setPixel(c, r, currentPitColor);
				}
			}
		}
		return pitsBitmap;
	}
}
package com.precisionag.watersheddelineationtool;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class PitRaster {
	//bitmap represents rasterized elevation data
	Bitmap pitsBitmap = null;
	List<Point> pitPointList = new ArrayList<Point>();
	List<Pit> pitDataList = new ArrayList<Pit>();
	int[][] pitIDMatrix;
	
	
	//constructor method
	public PitRaster(double[][] DEM, double[][] drainage, FlowDirectionCell[][] flowDirection, int cellSize, double rainfallIntensity) {
		int numrows = flowDirection.length;
		int numcols = flowDirection[0].length;
		pitIDMatrix = new int[numrows][numcols];
		
		int pitIDCounter = 0;
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					pitIDMatrix[r][c] = -1;
					continue;
				}
				if (flowDirection[r][c].childPoint.y < 0) {
					Point pitPoint = new Point(c, r);
					pitPointList.add(pitPoint);
					ArrayList<Point> indicesDrainingToPit = new ArrayList<Point>();
					indicesDrainingToPit.add(pitPoint);
					indicesDrainingToPit = findCellsDrainingToPointNew(flowDirection, indicesDrainingToPit);
					for (int idx = 0; idx < indicesDrainingToPit.size(); idx++) {
						int rowidx = indicesDrainingToPit.get(idx).y;
						int colidx = indicesDrainingToPit.get(idx).x;
						pitIDMatrix[rowidx][colidx] = pitIDCounter;
					}
					pitIDCounter++;
				}
			}
		}
		
		// After identifying the pits matrix, gather pit data for each pit
		for (int pitIdx = 0; pitIdx < pitPointList.size(); pitIdx++) {
			Pit currentPit = new Pit(drainage, cellSize, DEM, flowDirection, pitIDMatrix, pitPointList.get(pitIdx), pitIdx, rainfallIntensity);
			pitDataList.add(currentPit);
		}
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		pitsBitmap = Bitmap.createBitmap(numcols, numrows, config);
		for (int r = 0; r < numrows; r++){
			for (int c = 0; c < numcols; c++){
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					pitsBitmap.setPixel(c, r, Color.BLACK);	
					continue;
				}
				int currentPitColor = pitDataList.get(pitIDMatrix[r][c]).pitColor;
				pitsBitmap.setPixel(c, r, currentPitColor);
			}
		}
	}
	
	public ArrayList<Point> findCellsDrainingToPointNew(FlowDirectionCell[][] flowDirection, ArrayList<Point> indicesDrainingToPit) {
		int i = 0;
		while (i < indicesDrainingToPit.size()) {
			int currentRow = indicesDrainingToPit.get(i).y;
			int currentColumn = indicesDrainingToPit.get(i).x;

			if (flowDirection[currentRow][currentColumn].parentList.isEmpty()){
				i++;
				continue;
			}
			for (int parentIdx = 0; parentIdx < flowDirection[currentRow][currentColumn].parentList.size(); parentIdx++) {
				indicesDrainingToPit.add(flowDirection[currentRow][currentColumn].parentList.get(parentIdx));
			}
			i++;
		}
		return indicesDrainingToPit;
	}
	
	public int getIndexOf(int inputPitID){
		for (int i = 0; i < pitDataList.size(); i++) {
			if (pitDataList.get(i).pitID == inputPitID) {
				return i;
			}
		}
		int pitListIndex = (Integer) null;
		return pitListIndex;
	}
	
	public int getMaximumPitID() {
		int MaxPitID = (Integer) null;
		for (int i = 0; i < pitDataList.size(); i++) {
			if (pitDataList.get(i).pitID > MaxPitID) {
				MaxPitID = pitDataList.get(i).pitID;
			}
		}
		
		return MaxPitID;
	}
	
}
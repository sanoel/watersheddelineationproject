package com.precisionag.watersheddelineationtool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.GroundOverlay;
import com.precisionag.watersheddelineationtool.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.res.AssetManager;
import android.view.Menu;

public class MainActivity extends Activity {
	GroundOverlay prevoverlay;
	LocationManager locationManager;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
//		Bitmap elevation = BitmapFactory.decodeResource(getResources(), R.drawable.home128);
		MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
		GoogleMap map = mapFragment.getMap();
		map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		map.setMyLocationEnabled(true);
		UiSettings uiSettings = map.getUiSettings();
		uiSettings.setRotateGesturesEnabled(false);
		uiSettings.setTiltGesturesEnabled(false);
		uiSettings.setZoomControlsEnabled(false);
		double[][] DEM = readDEMFile("DEM");
		double[][] flowDirection = computeFlowDirection(DEM, drainage, intensity);
		double[][] flowAccumulation = computeFlowAccumulation(flowDirection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private double[][] readDEMFile(String DEMName) {
		AssetManager assetManager = getAssets();
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(DEMName + ".asc")));
			String line = bufferedReader.readLine();
			String[] lineArray = line.split("\\s+");
			int numrows = Integer.parseInt(line);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			int numcols = Integer.parseInt(line);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			double xLLCorner = Double.parseDouble(line);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			double yLLCorner = Double.parseDouble(line);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			double cellSize = Double.parseDouble(line);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			double NaNValue = Double.parseDouble(line);
			
			double[][] DEM = new double[numrows][numcols]; 
			int r = 0;
			while (( line = bufferedReader.readLine()) != null){
				lineArray = line.split("\\s+");
				for (int c = 0; c < numcols; c++) {
				    DEM[r][c] = Double.parseDouble(lineArray[c]);
				}
			r++;
			}
		} finally {}
		return DEM;
	}
	
	// Flow Direction
	public double[][] computeFlowDirection(double[][] DEM, double[][] drainage, double intensity) {
		int numrows = DEM.length;
		int numcols = DEM[0].length;
		double[][] flowDirection = new double[numrows][numcols];
		
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				// If the drainage rate is greater than the accumulation rate
				// then the cell is a pit.
				
				if (r == numcols-1 || r == 0 || c == numrows-1 || c == 0) {
					flowDirection[r][c] = Double.NaN;
					continue;
				}
				
				if (drainage[r][c] >= intensity) {
					flowDirection[r][c] = -2;
					continue;
				}

				double minimumSlope = Double.NaN;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;}
						double distance = Math.pow((Math.pow(x, 2) + Math.pow(y, 2)),0.5);
						double slope = (DEM[r+y][c+x] - DEM[r][c])/distance;
						double angle = Math.atan2(y,x); 

						//maintain current minimum slope, minimum slope being the steepest downslope
						if (Double.isNaN(minimumSlope) || slope <= minimumSlope) {
							flowDirection[r][c] = angle % 2*Math.PI;
						}
					}
				}
				if (minimumSlope == 0) {
					flowDirection[r][c] = -4;
				}
				if (minimumSlope >= 0) {
					flowDirection[r][c] = -1;
				}
			}
		}
		return flowDirection;
	}

	// Flow Accumulation
	public Integer[][] computeFlowAccumulation(double[][] flowDirection) {
		int numrows = flowDirection.length;
		int numcols = flowDirection[0].length;
		Integer[][] flowAccumulation = new Integer[numrows][numcols];
		for (int c = 0; c < numrows; c++){
			for (int r = 0; r < numcols; r++){
				if (r == numcols-1 || r == 0 || c == numrows-1 || c == 0) {
					flowAccumulation[r][c] = null;
					continue;
				}
				if (flowAccumulation[r][c] == null) {
					flowAccumulation = recursiveFlowAccumulation(flowDirection, flowAccumulation, twoDToLinearIndexing(r, c, numrows));
				}
			}
		}
		return flowAccumulation;
	}

	// Flow Accumulation Recursive Function Component
	public Integer[][] recursiveFlowAccumulation(double[][] flowDirection, Integer[][] flowAccumulation, int Index) {
		int numrows = flowDirection.length;
		int numcols = flowDirection[0].length;
		int r = linearToTwoDIndexing(Index, numrows)[0];
		int c = linearToTwoDIndexing(Index, numrows)[1];
		for (int x = -1; x < 2; x++) {
			for (int y = -1; y < 2; y++){
				if (x == 0 && y == 0) {
					continue;}
				if (r+y >= numrows || r+y < 0 || c+x >= numcols || c+x < 0) {
					continue;}
				double angle = Math.atan2(y,x) - Math.PI;
				if (flowDirection[r+y][c+x] == angle % 2*Math.PI) {
					int neighborIndex = twoDToLinearIndexing(r+y, c+x, numrows);
					if (flowAccumulation[r+y][c+x] == null){
						recursiveFlowAccumulation(flowDirection, flowAccumulation, twoDToLinearIndexing(r, c, numrows));
					}
					flowAccumulation[r][c] = flowAccumulation[r][c] + flowAccumulation[r+y][c+x];
				}
			}
		}
		return flowAccumulation;
	}
	
	// Original Pit Definition
	public int[][] computePits(double[][] DEM, double[][] drainage, double[][] flowDirection, int cellSize, double Intensity) {
		int numrows = flowDirection.length;
		int numcols = flowDirection[0].length;
		int[][] pits = new int[numrows][numcols];
		List<Integer> pitIndicesList = new ArrayList<Integer>();
		List<Pit> pitDataList = new ArrayList<Pit>();
		
		int pitIDCounter = 1;
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				int currentCellIndex = twoDToLinearIndexing(r, c, numrows);
				if (flowDirection[r][c] < 0) {
					pitIndicesList.add(currentCellIndex);
					List<Integer> indicesDrainingToPit = new ArrayList<Integer>();
					indicesDrainingToPit = findCellsDrainingToPoint(currentCellIndex, numrows, numcols, flowDirection, indicesDrainingToPit);
					for (int idx = 0; idx < indicesDrainingToPit.size(); idx++) {
						int rowidx = linearToTwoDIndexing(indicesDrainingToPit.get(idx), numrows)[0];
						int colidx = linearToTwoDIndexing(indicesDrainingToPit.get(idx), numrows)[1];
						pits[rowidx][colidx] = pitIDCounter;
					}
				} else if (Double.isNaN(flowDirection[r][c])) {
					pits[r][c] = 0;
				}
			pitIDCounter = pitIDCounter + 1;
			}
		}
		pitIDCounter = 1;
		for (int pitIdx = 0; pitIdx < pitIndicesList.size(); pitIdx++) {
			Pit currentPit = new Pit(drainage, cellSize, DEM, flowDirection, pits, pitIndicesList.get(pitIdx), pitIDCounter);
			pitDataList.add(currentPit);
		}
		return pits;
	}
	
	public List<Integer> findCellsDrainingToPoint(int index, int rowSize, int columnSize, double[][] flowDirection, List<Integer> indicesDrainingToIndex) {
		indicesDrainingToIndex.add(index);
		int r = linearToTwoDIndexing(index, rowSize)[0];
		int c = linearToTwoDIndexing(index, rowSize)[1];
		for (int x = -1; x < 2; x++) {
			for (int y = -1; y < 2; y++){
				if (x == 0 && y == 0) {
					continue;}
				if (r+y > rowSize || r+y < 1 || c+x > columnSize || c+x < 1) {
					continue;}
				double angle = Math.atan2(y,x);
				if (flowDirection[r+y][c+x] == angle % 2*Math.PI) {
					int neighborIndex = twoDToLinearIndexing(r+y, c+x, rowSize);
					indicesDrainingToIndex.addAll(findCellsDrainingToPoint(neighborIndex, rowSize, columnSize, flowDirection, indicesDrainingToIndex));
				}
			}
		}
		return indicesDrainingToIndex;
	}

	public int[] linearToTwoDIndexing(int linearIndex, int numrows) {
		int[] rowcol = new int[2];
		rowcol[0] = linearIndex % numrows; // row index
		rowcol[1] = linearIndex/numrows;   // column index
		return rowcol;
	}
	public int twoDToLinearIndexing(int rowIndex, int columnIndex, int numrows) {
		int Index = (columnIndex*numrows) + rowIndex;

		return Index;
	}
}
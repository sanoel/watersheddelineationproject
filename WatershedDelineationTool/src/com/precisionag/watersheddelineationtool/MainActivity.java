package com.precisionag.watersheddelineationtool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.precisionag.watersheddelineationtool.CustomMarker;
import com.precisionag.watersheddelineationtool.R;

import android.location.LocationManager;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity implements OnMapClickListener {
	GroundOverlay prevoverlay;
	LocationManager locationManager;
	private Menu myMenu = null;
	RasterLayer raster;
	
	//marker variables
	int markerMode;
	List<CustomMarker> markers;
	private static final int ADD_MODE = 1;
	private static final int REMOVE_MODE = 2;
	double rainfallIntensity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
		final GoogleMap map = mapFragment.getMap();
		map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		map.setMyLocationEnabled(true);
		map.setOnMapClickListener(this);

		UiSettings uiSettings = map.getUiSettings();
		uiSettings.setRotateGesturesEnabled(false);
		uiSettings.setTiltGesturesEnabled(false);
		uiSettings.setZoomControlsEnabled(false);
		markers = new ArrayList<CustomMarker>();
		markerMode = 0;
		final double[][] DEM = readDEMFile("Test");
//		RasterLayer DEMRaster = new RasterLayer(DEM, new LatLng(0.0, 0.0), new LatLng(0.0, 0.0));
		int numrows = DEM.length;
		int numcols = DEM[0].length;
		final double[][] drainage = new double[numrows][numcols];
		RasterLayer drainageRaster = new RasterLayer(drainage, new LatLng(0.0, 0.0), new LatLng(0.0, 0.0));
		CustomMarker.setRaster(drainageRaster);
		CustomMarker.setActivity(this);
		CustomMarker.setMap(map);
		prevoverlay = drainageRaster.createOverlay(map);
		configSeekbar();
		Button button = (Button) findViewById(R.id.button_delineate);
		button.setOnClickListener(new View.OnClickListener() {
			
			//Called when the user clicks the Delineate button
			@Override
			public void onClick(View arg0) {
				FlowDirectionCell[][] flowDirection = computeFlowDirectionNew(DEM, drainage, rainfallIntensity);
//				RasterLayer flowDirectionRaster = new RasterLayer(flowDirection, new LatLng(0.0, 0.0), new LatLng(0.0, 0.0));
				Integer[][] flowAccumulation = computeFlowAccumulationNew(flowDirection);
				Bitmap flowAccumBitmap = colorFlowAccumulation(flowAccumulation);
				LatLng sw = new LatLng(40.974, -86.1991);
				LatLng ne = new LatLng(40.983, -86.1869);
				LatLngBounds fieldBounds = new LatLngBounds(sw, ne);
//				GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
//			     .image(BitmapDescriptorFactory.fromBitmap(flowAccumBitmap))
//			     .positionFromBounds(fieldBounds)
//			     .transparency(0));
//				groundOverlay.setVisible(true);
				int cellSize = 1;
				Log.w("onclick", "1");
				PitRaster pits = new PitRaster(DEM, drainage, flowDirection, cellSize, rainfallIntensity);
				Log.w("onclick", "2");
				Bitmap pitBitmap = pits.pitsBitmap;
				Log.w("onclick", "3");
				GroundOverlayOptions goverlayopts = new GroundOverlayOptions();
				Log.w("onclick", "4");
				goverlayopts.image(BitmapDescriptorFactory.fromBitmap(pitBitmap));
				Log.w("onclick", "5");
				goverlayopts.positionFromBounds(fieldBounds);
				Log.w("onclick", "6");
				goverlayopts.transparency(0);
				Log.w("onclick", "7");
				Log.w("onclick", Boolean.toString(map == null));
				GroundOverlay pitGroundOverlay = map.addGroundOverlay(goverlayopts);
//						
//						new GroundOverlayOptions()
//			     .image(BitmapDescriptorFactory.fromBitmap(pitBitmap))
//			     .positionFromBounds(fieldBounds)
//			     .transparency(0));
				pitGroundOverlay.setVisible(true);
//				RasterLayer flowAccumulationRaster = new RasterLayer(flowAccumulation, new LatLng(0.0, 0.0), new LatLng(0.0, 0.0));
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		myMenu = menu;
		// Make an action bar and don't display the app title
		ActionBar actionBar = getActionBar();
		getActionBar().setDisplayShowTitleEnabled(false);
		getActionBar().setDisplayShowHomeEnabled(false);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle Markerss	
		if (item.getItemId() == R.id.menu_add) {
			markerMode = ADD_MODE;
			return true;
		}
		else if (item.getItemId() == R.id.menu_remove) {
			markerMode = REMOVE_MODE;
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
	}

	public void onMapClick (LatLng point) {
		switch(markerMode) {
		case ADD_MODE:
			CustomMarker.getPixelCoords(point);
			markers.add(new CustomMarker(point));
			markerMode = 0;
			break;
		case REMOVE_MODE:
			Iterator<CustomMarker> i = markers.iterator();
			CustomMarker marker;

			while (i.hasNext()) {
				marker = i.next();
				if (marker.inBounds(point)) {
					marker.removeMarker();
					i.remove();
				}
			}
			break;
		default:
			break;
		}
	}
	
	private void configSeekbar() {
		SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
		seekBar.setMax(10);
		seekBar.setProgress(0);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				//get level from seekbar
				int rainfallSeekbarProgress = seekBar.getProgress();

				//update text block
				double rainfallAmount = ((double)rainfallSeekbarProgress);
				TextView waterElevationTextView = (TextView) findViewById(R.id.text2);
				String rainfall = new DecimalFormat("#.#").format(rainfallAmount);
				double rainfallIntensity = rainfallAmount/24;
				String waterElevationText = "Rainfall Event: 24-hour, " + rainfall + "inches";
				waterElevationTextView.setText(waterElevationText);
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				
			}
		});
	}
			
	private double[][] readDEMFile(String DEMName) {
		AssetManager assetManager = getAssets();
		double[][] DEM = null;
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(DEMName + ".asc")));
			String line = bufferedReader.readLine();
			String[] lineArray = line.split("\\s+");
			int numcols = Integer.parseInt(lineArray[1]);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			int numrows = Integer.parseInt(lineArray[1]);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			double xLLCorner = Double.parseDouble(lineArray[1]);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			double yLLCorner = Double.parseDouble(lineArray[1]);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			double cellSize = Double.parseDouble(lineArray[1]);
			line = bufferedReader.readLine();
			lineArray = line.split("\\s+");
			double NaNValue = Double.parseDouble(lineArray[1]);
			
			DEM = new double[numrows][numcols]; 
			int r = 0;
			while (( line = bufferedReader.readLine()) != null){
				lineArray = line.split("\\s+");
				for (int c = 0; c < numcols; c++) {
				    DEM[r][c] = Double.parseDouble(lineArray[c]);
				}
			r++;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {}
		return DEM;
	}

	// Flow Direction
	public FlowDirectionCell[][] computeFlowDirectionNew(double[][] DEM, double[][] drainage, double rainfallIntensity) {
		int numrows = DEM.length;
		int numcols = DEM[0].length;
		FlowDirectionCell[][] flowDirection = new FlowDirectionCell[numrows][numcols];
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				Point childPoint = null;
				
				// If the drainage rate is greater than the accumulation rate
				// then the cell is a pit.
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
					flowDirection[r][c] = flowDirectionCell;
					continue;
				}
				
//				if (drainage[r][c] >= rainfallIntensity) {
//					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(-2,-2);
//					flowDirectionCellMatrix[r][c] = flowDirectionCell;					
//					continue;
//				}

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
							minimumSlope = slope;
							childPoint = new Point(c+x, r+y);
							FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
							flowDirection[r][c] = flowDirectionCell;
						}
					}
				}
				
				// Identification of flat spot "pits" with undefined flow direction
				if (minimumSlope == 0) {
					childPoint = new Point(-4, -4);
					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
					flowDirection[r][c] = flowDirectionCell;
				}
				
				// Identification of Pits with undefined flow direction
				if (minimumSlope >= 0) {
					childPoint = new Point(-1, -1);
					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);					
					flowDirection[r][c] = flowDirectionCell;				
				}
			}
		}
		
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				Point currentPoint = new Point(c, r);
				ArrayList<Point> parentList = new ArrayList<Point>();				
				// If the drainage rate is greater than the accumulation rate
				// then the cell is a pit.
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					continue;
				}
				// Find all cells pointing to current cell. This comes after assuring that the current cell isn't on the border.
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;}
						if (r+y == numrows-1 || r+y == 0 || c+x == numcols-1 || c+x == 0) {
							continue;
						}
						if (flowDirection[r+y][c+x].childPoint.x == currentPoint.x && flowDirection[r+y][c+x].childPoint.y == currentPoint.y) {
							Point parentPoint = new Point(c+x, r+y); 
							parentList.add(parentPoint);
						}
					}
				}
				flowDirection[r][c].setParentList(parentList);
			}
		}
		return flowDirection;
	}
	
//	// Flow Direction
//	public double[][] computeFlowDirection(double[][] DEM, double[][] drainage, double rainfallIntensity) {
//		int numrows = DEM.length;
//		int numcols = DEM[0].length;
//		double[][] flowDirection = new double[numrows][numcols];
//		
//		for (int r = 0; r < numrows; r++) {
//			for (int c = 0; c < numcols; c++) {
//				// If the drainage rate is greater than the accumulation rate
//				// then the cell is a pit.
//				
//				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
//					flowDirection[r][c] = Double.NaN;
//					continue;
//				}
//				
//				if (drainage[r][c] >= rainfallIntensity) {
//					flowDirection[r][c] = -2;
//					continue;
//				}
//
//				double minimumSlope = Double.NaN;
//				for (int x = -1; x < 2; x++) {
//					for (int y = -1; y < 2; y++){
//						if (x == 0 && y == 0) {
//							continue;}
//						double distance = Math.pow((Math.pow(x, 2) + Math.pow(y, 2)),0.5);
//						double slope = (DEM[r+y][c+x] - DEM[r][c])/distance;
//						double angle = Math.atan2(y,x); 
//
//						//maintain current minimum slope, minimum slope being the steepest downslope
//						if (Double.isNaN(minimumSlope) || slope <= minimumSlope) {
//							flowDirection[r][c] = angle % 2*Math.PI;
//						}
//					}
//				}
//				if (minimumSlope == 0) {
//					flowDirection[r][c] = -4;
//				}
//				if (minimumSlope >= 0) {
//					flowDirection[r][c] = -1;
//				}
//			}
//		}
//		return flowDirection;
//	}

	// Flow Accumulation
	public Integer[][] computeFlowAccumulationNew(FlowDirectionCell[][] flowDirection) {
		int numrows = flowDirection.length;
		int numcols = flowDirection[0].length;
		Integer[][] flowAccumulation = new Integer[numrows][numcols];
		for (int r = 0; r < numrows; r++){
			for (int c = 0; c < numcols; c++){
				flowAccumulation[r][c] = 0;
			}
		}
		for (int r = 0; r < numrows; r++){
			for (int c = 0; c < numcols; c++){
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					continue;
				}
				flowAccumulation = recursiveFlowAccumulationNew(flowDirection, flowAccumulation, r, c);
			}
		}
		return flowAccumulation;
	}
	
	public Integer[][] recursiveFlowAccumulationNew(FlowDirectionCell[][] flowDirection, Integer[][] flowAccumulation, int r, int c) {
		int numrows = flowDirection.length;
		int numcols = flowDirection[0].length;
		if (flowDirection[r][c].childPoint.y == numrows-1 || flowDirection[r][c].childPoint.y == 0 || flowDirection[r][c].childPoint .x== numcols-1 || flowDirection[r][c].childPoint.x == 0) {
			return flowAccumulation;
		} else if (flowDirection[r][c].childPoint.y < 0){
			return flowAccumulation;
		} else {
		flowAccumulation[flowDirection[r][c].childPoint.y][flowDirection[r][c].childPoint.x] = flowAccumulation[flowDirection[r][c].childPoint.y][flowDirection[r][c].childPoint.x] + 1;
		flowAccumulation = recursiveFlowAccumulationNew(flowDirection, flowAccumulation, flowDirection[r][c].childPoint.y, flowDirection[r][c].childPoint.x);
			return flowAccumulation;
		}
	}
	
	public Bitmap colorFlowAccumulation(Integer[][] flowAccumulation) {
		int numrows = flowAccumulation.length;
		int numcols = flowAccumulation[0].length;
		Bitmap.Config config = Bitmap.Config.ARGB_8888;
		Bitmap flowAccumBitmap = Bitmap.createBitmap(numcols, numrows, config);
		for (int r = 0; r < numrows; r++){
			for (int c = 0; c < numcols; c++){
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					flowAccumBitmap.setPixel(c, r, Color.BLACK);	
					continue;
				}
				int intColor = Color.rgb(0, 0, 256/(1 + flowAccumulation[r][c]));
				flowAccumBitmap.setPixel(c, r, intColor);
			}
		}
		return flowAccumBitmap;
	}
	
//	// Flow Accumulation
//	public Integer[][] computeFlowAccumulation(double[][] flowDirection) {
//		int numrows = flowDirection.length;
//		int numcols = flowDirection[0].length;
//		Integer[][] flowAccumulation = new Integer[numrows][numcols];
//		for (int r = 0; r < numrows; r++){
//			for (int c = 0; c < numcols; c++){
//				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
//					continue;
//				}
//				if (flowAccumulation[r][c] == null) {
//					flowAccumulation = recursiveFlowAccumulation(flowDirection, flowAccumulation, twoDToLinearIndexing(r, c, numrows));
//				}
//			}
//		}
//		return flowAccumulation;
//	}

//	// Flow Accumulation Recursive Function Component
//	public Integer[][] recursiveFlowAccumulation(double[][] flowDirection, Integer[][] flowAccumulation, int Index) {
//		int numrows = flowDirection.length;
//		int numcols = flowDirection[0].length;
//		int r = linearToTwoDIndexing(Index, numrows)[0];
//		int c = linearToTwoDIndexing(Index, numrows)[1];
//		for (int x = -1; x < 2; x++) {
//			for (int y = -1; y < 2; y++){
//				if (x == 0 && y == 0) {
//					continue;}
//				if (r+y >= numrows || r+y < 0 || c+x >= numcols || c+x < 0) {
//					continue;}
//				double angle = Math.atan2(y,x) - Math.PI;
//				if (flowDirection[r+y][c+x] == angle % 2*Math.PI) {
//					int neighborIndex = twoDToLinearIndexing(r+y, c+x, numrows);
//					if (flowAccumulation[r+y][c+x] == null){
//						recursiveFlowAccumulation(flowDirection, flowAccumulation, twoDToLinearIndexing(r, c, numrows));
//					}
//					flowAccumulation[r][c] = flowAccumulation[r][c] + flowAccumulation[r+y][c+x];
//				}
//			}
//		}
//		return flowAccumulation;
//	}
	
	// Original Pit Definition
//	public int[][] computePits(double[][] DEM, double[][] drainage, FlowDirectionCell[][] flowDirection, int cellSize, double rainfallIntensity) {
//		int numrows = flowDirection.length;
//		int numcols = flowDirection[0].length;
//		int[][] pits = new int[numrows][numcols];
//		List<Integer> pitCellIndicesList = new ArrayList<Integer>();
//		List<Pit> pitDataList = new ArrayList<Pit>();
//		
//		int pitIDCounter = 1;
//		for (int r = 0; r < numrows; r++) {
//			for (int c = 0; c < numcols; c++) {
//				int currentCellIndex = twoDToLinearIndexing(r, c, numrows);
//				if (flowDirection[r][c].childPoint.y < 0) {
//					pitCellIndicesList.add(currentCellIndex);
//					ArrayList<Point> indicesDrainingToPit = new ArrayList<Point>();
//					Point pitCellIndices = new Point(c, r);
//					indicesDrainingToPit.add(pitCellIndices);
//					indicesDrainingToPit = findCellsDrainingToPointNew(flowDirection, indicesDrainingToPit);
//					for (int idx = 0; idx < indicesDrainingToPit.size(); idx++) {
//						int rowidx = indicesDrainingToPit.get(idx).y;
//						int colidx = indicesDrainingToPit.get(idx).x;
//						pits[rowidx][colidx] = pitIDCounter;
//					}
//				} else if (Double.isNaN(flowDirection[r][c].childPoint.x)) {
//					pits[r][c] = 0;
//				}
//			pitIDCounter = pitIDCounter + 1;
//			}
//		}
//		pitIDCounter = 1;
//		for (int pitIdx = 0; pitIdx < pitCellIndicesList.size(); pitIdx++) {
//			Pit currentPit = new Pit(drainage, cellSize, DEM, flowDirection, pits, pitCellIndicesList.get(pitIdx), pitIDCounter, rainfallIntensity);
//			pitDataList.add(currentPit);
//		}
//		return pits;
//	}
//	
//	public ArrayList<Point> findCellsDrainingToPointNew(FlowDirectionCell[][] flowDirection, ArrayList<Point> indicesDrainingToPit) {
//		int i = 0;
//		while (i < indicesDrainingToPit.size()) {
//			int currentRow = indicesDrainingToPit.get(i).y;
//			int currentColumn = indicesDrainingToPit.get(i).x;
//			for (int parentIdx = 0; parentIdx < flowDirection[currentRow][currentColumn].parentList.size(); parentIdx++) {
//				indicesDrainingToPit.add(flowDirection[currentRow][currentColumn].parentList.get(parentIdx));
//			}
//		}
//		return indicesDrainingToPit;
//	}
	
//	public List<Integer> findCellsDrainingToPoint(int index, int rowSize, int columnSize, double[][] flowDirection, List<Integer> indicesDrainingToIndex) {
//		indicesDrainingToIndex.add(index);
//		int r = linearToTwoDIndexing(index, rowSize)[0];
//		int c = linearToTwoDIndexing(index, rowSize)[1];
//		for (int x = -1; x < 2; x++) {
//			for (int y = -1; y < 2; y++){
//				if (x == 0 && y == 0) {
//					continue;}
//				if (r+y > rowSize || r+y < 1 || c+x > columnSize || c+x < 1) {
//					continue;}
//				double angle = Math.atan2(y,x);
//				if (flowDirection[r+y][c+x] == angle % 2*Math.PI) {
//					int neighborIndex = twoDToLinearIndexing(r+y, c+x, rowSize);
//					indicesDrainingToIndex.addAll(findCellsDrainingToPoint(neighborIndex, rowSize, columnSize, flowDirection, indicesDrainingToIndex));
//				}
//			}
//		}
//		return indicesDrainingToIndex;
//	}

//	public int[] linearToTwoDIndexing(int linearIndex, int numrows) {
//		int[] rowcol = new int[2];
//		rowcol[0] = linearIndex % numrows; // row index
//		rowcol[1] = linearIndex/numrows;   // column index
//		return rowcol;
//	}
//	public int twoDToLinearIndexing(int rowIndex, int columnIndex, int numrows) {
//		int Index = (columnIndex*numrows) + rowIndex;
//
//		return Index;
//	}
}
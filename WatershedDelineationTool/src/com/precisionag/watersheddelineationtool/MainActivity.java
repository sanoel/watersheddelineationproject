package com.precisionag.watersheddelineationtool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	BigDecimal rainfallDuration = new BigDecimal(24); // hours
	BigDecimal rainfallDepth = new BigDecimal(0.5*0.0254, MathContext.DECIMAL64); // inches
	BigDecimal rainfallIntensity = rainfallDepth.divide(rainfallDuration, MathContext.DECIMAL64);
	BigDecimal cellSize = new BigDecimal(3); // meters
	
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
		final double[][] DEM = readDEMFile("Test4");
		int numrows = DEM.length;
		int numcols = DEM[0].length;
		final double[][] drainage = new double[numrows][numcols];
//		RasterLayer drainageRaster = new RasterLayer(drainage, new LatLng(0.0, 0.0), new LatLng(0.0, 0.0));
//		CustomMarker.setRaster(drainageRaster);
//		CustomMarker.setActivity(this);
//		CustomMarker.setMap(map);
//		prevoverlay = drainageRaster.createOverlay(map);
		configSeekbar();
		Button button = (Button) findViewById(R.id.button_delineate);
		button.setOnClickListener(new View.OnClickListener() {
			
			//Called when the user clicks the Delineate button
			@Override
			public void onClick(View arg0) {
				WatershedDataset watershedDataset = new WatershedDataset(DEM, drainage, cellSize, rainfallDuration, rainfallDepth);
//				RasterLayer flowDirectionRaster = new RasterLayer(flowDirection, new LatLng(0.0, 0.0), new LatLng(0.0, 0.0));
//				Integer[][] flowAccumulation = computeFlowAccumulation(flowDirection);
//				Bitmap flowAccumBitmap = colorFlowAccumulation(flowAccumulation);
				LatLng sw = new LatLng(40.974, -86.1991);
				LatLng ne = new LatLng(40.983, -86.1869);
				LatLngBounds fieldBounds = new LatLngBounds(sw, ne);
//				GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
//			     .image(BitmapDescriptorFactory.fromBitmap(flowAccumBitmap))
//			     .positionFromBounds(fieldBounds)
//			     .transparency(0));
//				groundOverlay.setVisible(true);
				Bitmap pitBitmap = watershedDataset.pits.pitsBitmap;
				GroundOverlay pitGroundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
			     .image(BitmapDescriptorFactory.fromBitmap(pitBitmap))
			     .positionFromBounds(fieldBounds)
			     .transparency(0));
				pitGroundOverlay.setVisible(true);
				watershedDataset = fillPits(watershedDataset);
				Log.e("MA", Integer.toString(watershedDataset.pits.pitDataList.size()));
				Bitmap filledPitBitmap = watershedDataset.pits.pitsBitmap;
				GroundOverlay filledPitGroundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
			     .image(BitmapDescriptorFactory.fromBitmap(filledPitBitmap))
			     .positionFromBounds(fieldBounds)
			     .transparency(0));
				filledPitGroundOverlay.setVisible(true);
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
	public FlowDirectionCell[][] computeFlowDirectionNew(double[][] DEM, double[][] drainage, BigDecimal rainfallIntensity2) {
		int numrows = DEM.length;
		int numcols = DEM[0].length;
		FlowDirectionCell[][] flowDirection = new FlowDirectionCell[numrows][numcols];
		for (int r = 0; r < numrows; r++) {
			for (int c = 0; c < numcols; c++) {
				Point childPoint = null;
				
				// If the cell is along the border then it should remain a null
				if (r == numrows-1 || r == 0 || c == numcols-1 || c == 0) {
					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
					flowDirection[r][c] = flowDirectionCell;
					continue;
				}
				
				// If the drainage rate is greater than the accumulation rate
				// then the cell is a pit.
//				if (drainage[r][c] >= rainfallIntensity) {
//					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(-2,-2);
//					flowDirectionCellMatrix[r][c] = flowDirectionCell;					
//					continue;
//				}

				double minimumSlope = Double.NaN;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;
						}
						double distance = Math.pow((Math.pow(x, 2) + Math.pow(y, 2)),0.5);
						double slope = (DEM[r+y][c+x] - DEM[r][c])/distance;
//						double angle = Math.atan2(y,x); 

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
//				if (minimumSlope == 0) {
//					childPoint = new Point(-4, -4);
//					FlowDirectionCell flowDirectionCell = new FlowDirectionCell(childPoint);
//					flowDirection[r][c] = flowDirectionCell;
//				}
				
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
	
	// Recursive Flow Accumulation
	public Integer[][] computeFlowAccumulation(FlowDirectionCell[][] flowDirection) {
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
				if (flowAccumulation[r][c] == 0) {
					flowAccumulation = recursiveFlowAccumulation(flowDirection, flowAccumulation, r, c);
				}
			}
		}
		return flowAccumulation;
	}
	
	public Integer[][] recursiveFlowAccumulation(FlowDirectionCell[][] flowDirection, Integer[][] flowAccumulation, int r, int c) {
		int numrows = flowDirection.length;
		int numcols = flowDirection[0].length;
		flowAccumulation[r][c] = 1;
//		Log.w("Flow Accumulation", "r=" + Integer.toString(r) + " c="+ Integer.toString(c) +" " + Integer.toString(flowAccumulation[2][2]));
		for (int i = 0; i < flowDirection[r][c].parentList.size(); i++){
			Point currentParent = flowDirection[r][c].parentList.get(i);
			// skip and return if the parent is an edge cell
			if (currentParent.y == numrows-1 || currentParent.y == 0 || currentParent.x== numcols-1 || currentParent.x == 0) {
				return flowAccumulation;
				
				// Verify that the cell hasn't been computed already (value == 0). Each cell should only have the recursive function called on it once.
			} else if (flowAccumulation[currentParent.y][currentParent.x] == 0){
				flowAccumulation = recursiveFlowAccumulation(flowDirection, flowAccumulation, currentParent.y, currentParent.x);
			}
			flowAccumulation[r][c] = flowAccumulation[r][c] + flowAccumulation[currentParent.y][currentParent.x];
		}
		return flowAccumulation;
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
	
	// Wrapper function that simulates the rainfall event to iteratively fill pits to connect the surface until the rainfall event ends
	public WatershedDataset fillPits(WatershedDataset watershedDataset) {
//		WatershedDataset Dataset = new WatershedDataset(DEM, drainage, flowDirection, pits, cellSize, rainfallDuration, rainfallDepth);
		int fillcounter = 0;
		Collections.sort(watershedDataset.pits.pitDataList);
		Log.w("pits", "started filling");
		while (watershedDataset.pits.pitDataList.get(0).spilloverTime.doubleValue() < rainfallDuration.doubleValue()) {
			Log.w("MainAct-fillcounter", Integer.toString(fillcounter));
			watershedDataset = mergePits(watershedDataset);
			Collections.sort(watershedDataset.pits.pitDataList);
			if (watershedDataset.pits.pitDataList.isEmpty()) {
				break;
			}
			fillcounter++;
			Log.e("MA", Integer.toString(watershedDataset.pits.pitDataList.size()));
		}
		watershedDataset.pits.updatePitsBitmap();

		return watershedDataset;
	}
	
	// Merge two pits
	public WatershedDataset mergePitsNew(WatershedDataset watershedDataset) {
		int secondPitID = watershedDataset.pits.pitDataList.get(0).pitIdOverflowingInto; //The pit ID that the first pit overflows into
		Log.w("pits-firstPitID", Integer.toString(watershedDataset.pits.pitDataList.get(0).pitID));
		Log.w("pits-secondPitID", Integer.toString(secondPitID));
		
		// Fill the first pit and resolve flow direction. This must be completed before the new pit entry is created or else retention volumes will be incorrectly calculated (the first pit must be filled).
		watershedDataset = resolveFilledArea(watershedDataset);
		// Handle pits merging with other pits
		if (secondPitID != -1) {
			int mergedPitID = watershedDataset.pits.getMaximumPitID() + 1;
			int secondPitListIndex = watershedDataset.pits.getIndexOf(secondPitID);
			Log.w("pits-flowintoPitIndex", Integer.toString(secondPitListIndex));
			Log.w("pits-newmaxPitID", Integer.toString(mergedPitID));

			// re-ID the two merging pits with their new mergedPitID
			for (int i = 0; i < watershedDataset.pits.pitDataList.get(0).allPointsList.size(); i++) {
				watershedDataset.pits.pitIDMatrix[watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).y][watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).x] = mergedPitID;
			}
			for (int i = 0; i < watershedDataset.pits.pitDataList.get(secondPitListIndex).allPointsList.size(); i++) {
				watershedDataset.pits.pitIDMatrix[watershedDataset.pits.pitDataList.get(secondPitListIndex).allPointsList.get(i).y][watershedDataset.pits.pitDataList.get(secondPitListIndex).allPointsList.get(i).x] = mergedPitID;
			}
			
			// Update all pits that will overflow into either of the merging pits as now overflowing into the new mergedPitID
			for (int i = 0; i < watershedDataset.pits.pitDataList.size(); i++){
				if ((watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto == watershedDataset.pits.pitDataList.get(0).pitID) || (watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto == secondPitID)) {
					watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto = mergedPitID;
				}
			}
			
			//////////////////////////////////////////////
			// Create the new merged pit entry
			// single pit cell variables
			watershedDataset.pits.pitDataList.get(0).pitID = mergedPitID;
			watershedDataset.pits.pitDataList.get(0).pitBottomElevation = watershedDataset.pits.pitDataList.get(secondPitListIndex).pitBottomElevation;
			watershedDataset.pits.pitDataList.get(0).pitBottomPoint = watershedDataset.pits.pitDataList.get(secondPitListIndex).pitBottomPoint;
			// New pit takes color of the pit that is being overflowed into
			watershedDataset.pits.pitDataList.get(0).pitColor = watershedDataset.pits.pitDataList.get(secondPitListIndex).pitColor;
			// Whole pit depression variables
			watershedDataset.pits.pitDataList.get(0).allPointsList.addAll(watershedDataset.pits.pitDataList.get(secondPitListIndex).allPointsList);
			watershedDataset.pits.pitDataList.get(0).areaCellCount = new BigDecimal(watershedDataset.pits.pitDataList.get(0).allPointsList.size());
			// Border-dependent variables and calculations
			watershedDataset.pits.pitDataList.get(0).pitBorderIndicesList = new ArrayList<Point>(watershedDataset.pits.pitDataList.get(secondPitListIndex).pitBorderIndicesList);
			watershedDataset.pits.pitDataList.get(0).pitBorderIndicesList.addAll(watershedDataset.pits.pitDataList.get(0).pitBorderIndicesList);
			for (int i = 0; i < watershedDataset.pits.pitDataList.get(0).allPointsList.size(); i++) {
				Point currentPoint = watershedDataset.pits.pitDataList.get(0).allPointsList.get(i);
				int r = currentPoint.y;
				int c = currentPoint.x;
				boolean onBorder = false;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++){
						if (x == 0 && y == 0) {
							continue;}
						if (watershedDataset.pits.pitIDMatrix[r+y][c+x] != watershedDataset.pits.pitIDMatrix[r][c]) {
							BigDecimal currentElevation = new BigDecimal(watershedDataset.DEM[r][c]);
							BigDecimal neighborElevation = new BigDecimal(watershedDataset.DEM[r+y][c+x]);
							onBorder = true;
							if ((watershedDataset.pits.pitDataList.get(0).spilloverElevation == null) || (currentElevation.doubleValue() <= watershedDataset.pits.pitDataList.get(0).spilloverElevation.doubleValue() && neighborElevation.doubleValue() <= watershedDataset.pits.pitDataList.get(0).spilloverElevation.doubleValue())) {
								watershedDataset.pits.pitDataList.get(0).minOutsidePerimeterElevation = neighborElevation;
								watershedDataset.pits.pitDataList.get(0).minInsidePerimeterElevation = currentElevation;
								watershedDataset.pits.pitDataList.get(0).spilloverElevation = neighborElevation.max(currentElevation);
								watershedDataset.pits.pitDataList.get(0).pitOutletPoint = currentPoint;
								watershedDataset.pits.pitDataList.get(0).outletSpilloverFlowDirection = new Point(c+x, r+y);
								watershedDataset.pits.pitDataList.get(0).pitIdOverflowingInto = watershedDataset.pits.pitIDMatrix[r+y][c+x];
							}
						}
					}
				}
				if (onBorder == false) {
					watershedDataset.pits.pitDataList.get(0).pitBorderIndicesList.remove(currentPoint);
				}
			}
			// Volume/elevation-dependent variables and calculations
			watershedDataset.pits.pitDataList.get(0).filledVolume = watershedDataset.pits.pitDataList.get(0).retentionVolume;
			watershedDataset.pits.pitDataList.get(0).retentionVolume = watershedDataset.pits.pitDataList.get(0).filledVolume;
			watershedDataset.pits.pitDataList.get(0).cellCountToBeFilled = 0;
			for (int i = 0; i < watershedDataset.pits.pitDataList.get(0).allPointsList.size(); i++) {
				Point currentPoint = watershedDataset.pits.pitDataList.get(0).allPointsList.get(i);
				int r = currentPoint.y;
				int c = currentPoint.x;
				if (watershedDataset.DEM[r][c] < watershedDataset.pits.pitDataList.get(0).spilloverElevation.doubleValue()) {
					watershedDataset.pits.pitDataList.get(0).retentionVolume = watershedDataset.pits.pitDataList.get(0).retentionVolume.add(watershedDataset.pits.pitDataList.get(secondPitListIndex).spilloverElevation.subtract(new BigDecimal(watershedDataset.DEM[r][c]), MathContext.DECIMAL64).multiply(cellSize.pow(2, MathContext.DECIMAL64), MathContext.DECIMAL64), MathContext.DECIMAL64);
					watershedDataset.pits.pitDataList.get(0).cellCountToBeFilled = watershedDataset.pits.pitDataList.get(0).cellCountToBeFilled + 1;
				}
			}
			//Sum the drainage taking place in the pit
			watershedDataset.pits.pitDataList.get(0).pitDrainageRate = new BigDecimal(0);
			for (int listIdx = 0; listIdx < watershedDataset.pits.pitDataList.get(0).allPointsList.size(); listIdx++) {
				Point currentPoint = watershedDataset.pits.pitDataList.get(0).allPointsList.get(listIdx);
				int r = currentPoint.y;
				int c = currentPoint.x;
				watershedDataset.pits.pitDataList.get(0).pitDrainageRate = watershedDataset.pits.pitDataList.get(0).pitDrainageRate.add(new BigDecimal(watershedDataset.drainage[r][c]), MathContext.DECIMAL64);
			}
			watershedDataset.pits.pitDataList.get(0).netAccumulationRate = (rainfallIntensity.multiply(watershedDataset.pits.pitDataList.get(0).areaCellCount)).subtract(watershedDataset.pits.pitDataList.get(0).pitDrainageRate, MathContext.DECIMAL64);
			watershedDataset.pits.pitDataList.get(0).spilloverTime = watershedDataset.pits.pitDataList.get(0).retentionVolume.divide(cellSize.pow(2, MathContext.DECIMAL64).multiply(watershedDataset.pits.pitDataList.get(0).netAccumulationRate, MathContext.DECIMAL64), MathContext.DECIMAL64);

			//////////////////////////////////

			// Removed the second pit
			watershedDataset.pits.pitDataList.remove(secondPitListIndex);

			// Handle pits merging with pit ID -1 (areas flowing off the map)
		} else if (secondPitID == -1) {
			// re-ID the filled pit as pit ID -1
			for (int i = 0; i < watershedDataset.pits.pitDataList.get(0).allPointsList.size(); i++) {
				watershedDataset.pits.pitIDMatrix[watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).y][watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).x] = -1;
			}

			// Update all pits that will overflow into the filled pit as now overflowing into pit ID -1
			for (int i = 0; i < watershedDataset.pits.pitDataList.size(); i++){
				if (watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto == watershedDataset.pits.pitDataList.get(0).pitID) {
					watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto = -1;
				}
			}
			watershedDataset.pits.pitDataList.remove(0);
		}
		return watershedDataset;	
	}
	
	// Merge two pits
	public WatershedDataset mergePits(WatershedDataset watershedDataset) {
		int overflowIntoPitID = watershedDataset.pits.pitDataList.get(0).pitIdOverflowingInto; //The pit ID that the first pit overflows into
		Log.w("pits-firstPitID", Integer.toString(watershedDataset.pits.pitDataList.get(0).pitID));
		Log.w("pits-secondPitID", Integer.toString(overflowIntoPitID));
//		int pit1556index = watershedDataset.pits.pitDataList.get(watershedDataset.pits.getIndexOf(1556)).pitIdOverflowingInto;
//		Log.w("pits1556index", Integer.toString(pit1556index));
		// Fill the first pit and resolve flow direction. This must be completed before the new pit entry is created or else retention volumes will be incorrectly calculated (the first pit must be filled).
		watershedDataset = resolveFilledArea(watershedDataset);
		// Handle pits merging with other pits
		if (overflowIntoPitID != -1) {
			int mergedPitID = watershedDataset.pits.getMaximumPitID() + 1;
			int pitOverflowedIntoListIndex = watershedDataset.pits.getIndexOf(overflowIntoPitID);
			Log.w("pits-flowintoPitIndex", Integer.toString(pitOverflowedIntoListIndex));
			Log.w("pits-newmaxPitID", Integer.toString(mergedPitID));


			// re-ID the two merging pits with their new mergedPitID
			for (int i = 0; i < watershedDataset.pits.pitDataList.get(0).allPointsList.size(); i++) {
				watershedDataset.pits.pitIDMatrix[watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).y][watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).x] = mergedPitID;
			}
			for (int i = 0; i < watershedDataset.pits.pitDataList.get(pitOverflowedIntoListIndex).allPointsList.size(); i++) {
				watershedDataset.pits.pitIDMatrix[watershedDataset.pits.pitDataList.get(pitOverflowedIntoListIndex).allPointsList.get(i).y][watershedDataset.pits.pitDataList.get(pitOverflowedIntoListIndex).allPointsList.get(i).x] = mergedPitID;
			}
			// Create the new merged pit entry
			Pit mergedPit = new Pit(watershedDataset);
			
			// Update all pits that will overflow into either of the merging pits as now overflowing into the new mergedPitID
			for (int i = 0; i < watershedDataset.pits.pitDataList.size(); i++){
				if ((watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto == watershedDataset.pits.pitDataList.get(0).pitID) || (watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto == overflowIntoPitID)) {
					watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto = mergedPitID;
				}
			}
			// Update the pitDataList. To preserve the index pitOverflowedIntoListIndex, it must be removed before pit(0)
			pitOverflowedIntoListIndex = watershedDataset.pits.getIndexOf(overflowIntoPitID);
			watershedDataset.pits.pitDataList.add(mergedPit);
			watershedDataset.pits.pitDataList.remove(pitOverflowedIntoListIndex);
			watershedDataset.pits.pitDataList.remove(0);
			
			// Handle pits merging with pit ID -1 (areas flowing off the map)
		} else if (overflowIntoPitID == -1) {
			// re-ID the filled pit as pit ID -1
			for (int i = 0; i < watershedDataset.pits.pitDataList.get(0).allPointsList.size(); i++) {
				watershedDataset.pits.pitIDMatrix[watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).y][watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).x] = -1;
			}
			
			// Update all pits that will overflow into the filled pit as now overflowing into pit ID -1
			for (int i = 0; i < watershedDataset.pits.pitDataList.size(); i++){
				if (watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto == watershedDataset.pits.pitDataList.get(0).pitID) {
					watershedDataset.pits.pitDataList.get(i).pitIdOverflowingInto = -1;
				}
			}
			watershedDataset.pits.pitDataList.remove(0);
		}	

		return watershedDataset;
	}
	
	public WatershedDataset resolveFilledArea(WatershedDataset watershedDataset) {
		List<Point> allPointsToResolve = new ArrayList<Point>();
		List<Point> pointsToResolve = new ArrayList<Point>();
		// Adjust DEM elevations
		for (int i = 0; i < watershedDataset.pits.pitDataList.get(0).allPointsList.size(); i++) {
			if (watershedDataset.DEM[watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).y][watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).x] < watershedDataset.pits.pitDataList.get(0).spilloverElevation.doubleValue()) {
				watershedDataset.DEM[watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).y][watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).x] = watershedDataset.pits.pitDataList.get(0).spilloverElevation.doubleValue();
				Point pointToResolve = new Point(watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).x, watershedDataset.pits.pitDataList.get(0).allPointsList.get(i).y);
				allPointsToResolve.add(pointToResolve);
				//add each cell to a single flat flow direction cell object
			}
		}
		
		// Correct flow direction
		watershedDataset.flowDirection[watershedDataset.pits.pitDataList.get(0).pitOutletPoint.y][watershedDataset.pits.pitDataList.get(0).pitOutletPoint.x] = new FlowDirectionCell(watershedDataset.pits.pitDataList.get(0).outletSpilloverFlowDirection);
		pointsToResolve.add(watershedDataset.pits.pitDataList.get(0).pitOutletPoint);
		while (!pointsToResolve.isEmpty()) {
			Point currentPoint = pointsToResolve.get(0);
			pointsToResolve.remove(0);
			for (int x = -1; x < 2; x++) {
				for (int y = -1; y < 2; y++){
					if (x == 0 && y == 0) {
						continue;
					}
					Point neighborPoint = new Point(currentPoint.x + x, currentPoint.y + y);
					// check if the point is part of the flat area to be resolved, but not already on the list
					if (allPointsToResolve.contains(neighborPoint) && !pointsToResolve.contains(neighborPoint)) {
						watershedDataset.flowDirection[neighborPoint.y][neighborPoint.x].childPoint = currentPoint;
						pointsToResolve.add(neighborPoint);
						allPointsToResolve.remove(neighborPoint);
					}
				}			
			}
			
		}
		return watershedDataset;
	}
	
}
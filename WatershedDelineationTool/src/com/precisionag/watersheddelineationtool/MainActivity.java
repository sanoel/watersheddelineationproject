package com.precisionag.watersheddelineationtool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
		final double[][] DEM = readDEMFile("Test2");
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
				PitRaster pits = new PitRaster(DEM, drainage, flowDirection, cellSize, rainfallIntensity);
				Bitmap pitBitmap = pits.pitsBitmap;
				GroundOverlayOptions goverlayopts = new GroundOverlayOptions();
				GroundOverlay pitGroundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
			     .image(BitmapDescriptorFactory.fromBitmap(pitBitmap))
			     .positionFromBounds(fieldBounds)
			     .transparency(0));
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
	
	// Wrapper function that simulates the rainfall event to iteratively fill pits to connect the surface until the rainfall event ends
	public FilledDataset fillPits(PitRaster pits, double[][] drainage, FlowDirectionCell[][] flowDirection, double[][] DEM, int rainfallDuration, double rainfallDepth, double cellSize) {
		FilledDataset fillDataset = new FilledDataset(DEM, drainage, flowDirection, pits, cellSize, rainfallDuration, rainfallDepth);
		
		while (pits.pitDataList.get(0).spilloverTime < rainfallDuration || pits.pitDataList.isEmpty()) {
			Comparator mycomparator;
			Collections.sort(null);
			fillDataset = mergePits(fillDataset);
		}
		return fillDataset;
	}
	
	// Merge two pits
	public FilledDataset mergePits(FilledDataset fillDataset) {
		int mergedPitID = fillDataset.fillPits.getMaximumPitID() + 1;
		int pitOverflowedIntoListIndex = fillDataset.fillPits.getIndexOf(fillDataset.fillPits.pitDataList.get(0).spilloverPitID);
		
		fillDataset = resolveFilledArea(fillDataset);
		if (fillDataset.fillPits.pitDataList.get(0).spilloverPitID != 0) {
			for (int i = 0; i < fillDataset.fillPits.pitDataList.get(0).allPitPointsList.size(); i++) {
				fillDataset.fillPits.pitIDMatrix[fillDataset.fillPits.pitDataList.get(0).allPitPointsList.get(i).y][fillDataset.fillPits.pitDataList.get(0).allPitPointsList.get(i).x] = mergedPitID;
			}
			for (int i = 0; i < fillDataset.fillPits.pitDataList.get(pitOverflowedIntoListIndex).allPitPointsList.size(); i++) {
				fillDataset.fillPits.pitIDMatrix[fillDataset.fillPits.pitDataList.get(pitOverflowedIntoListIndex).allPitPointsList.get(i).y][fillDataset.fillPits.pitDataList.get(pitOverflowedIntoListIndex).allPitPointsList.get(i).x] = mergedPitID;
			}
			MergedPit mergedPit = new MergedPit(fillDataset);
			fillDataset.fillPits.pitDataList.add(mergedPit);
			fillDataset.fillPits.pitDataList.remove(0);
			fillDataset.fillPits.pitDataList.remove(pitOverflowedIntoListIndex);
		} else if (fillDataset.fillPits.pitDataList.get(0).spilloverPitID == 0) {
			for (int i = 0; i < fillDataset.fillPits.pitDataList.get(0).allPitPointsList.size(); i++) {
				fillDataset.fillPits.pitIDMatrix[fillDataset.fillPits.pitDataList.get(0).allPitPointsList.get(i).y][fillDataset.fillPits.pitDataList.get(0).allPitPointsList.get(i).x] = 0;
				fillDataset.fillPits.pitDataList.remove(0);
			}
		}	
		return fillDataset;
	}
	
	public FilledDataset resolveFilledArea(FilledDataset fillDataset) {
//		Adjust DEM and resolve flow Direction
		for (int i = 0; i < fillDataset.fillPits.pitDataList.get(0).allPitPointsList.size(); i++) {
			if (fillDataset.fillDEM[fillDataset.fillPits.pitDataList.get(0).allPitPointsList.get(i).y][fillDataset.fillPits.pitDataList.get(0).allPitPointsList.get(i).x] < fillDataset.fillPits.pitDataList.get(0).overflowPointElevation) {
				fillDataset.fillDEM[fillDataset.fillPits.pitDataList.get(0).allPitPointsList.get(i).y][fillDataset.fillPits.pitDataList.get(0).allPitPointsList.get(i).x] = fillDataset.fillPits.pitDataList.get(0).overflowPointElevation;
				//add each cell to a single flat flow direction cell object
			}
		}
		fillDataset.fillFlowDirection[fillDataset.fillPits.pitDataList.get(0).pitOutletPoint.y][fillDataset.fillPits.pitDataList.get(0).pitOutletPoint.x] = new FlowDirectionCell(fillDataset.fillPits.pitDataList.get(0).outletSpilloverFlowDirection);
		return fillDataset;
	}
	
}
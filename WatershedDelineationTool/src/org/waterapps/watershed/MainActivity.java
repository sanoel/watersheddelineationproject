package org.waterapps.watershed;

import static android.graphics.Color.HSVToColor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import com.filebrowser.DataFileChooser;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.gdal.gdal.gdal;
import org.waterapps.watershed.HelpActivity;
import org.waterapps.watershed.R;
import org.waterapps.watershed.ProgressFragment.ProgressFragmentListener;
import org.waterapps.watershed.ResultsPanelFragment.ResultsPanelFragmentListener;

import com.precisionag.lib.*;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnMapClickListener, ProgressFragmentListener, OnMarkerDragListener, ResultsPanelFragmentListener {
	ProgressFragmentListener pflistener;
	private Uri fileUri;
	LocationManager locationManager;
	private static Menu myMenu = null;
	public static GoogleMap map;
	public static SharedPreferences prefs;
	public static Context context;
	
	String wsdStatusMsg = "Reading DEM";
	public static String demDirectory = "/dem";
	public static float demAlpha;
	public static float delineationAlpha;
	public static float puddleAlpha;
	public static float pitsAlpha;

	public static int selectedPitIndex;
	private static final int FIRST_START = 42;
	private static final int INITIAL_LOAD = 6502;
	public static int hsvColors[];
	public static int hsvTransparentColors[];
	
	public static boolean coloring;
	public static boolean currentlyDrawing;
	public static boolean dem_visible;
	public static boolean pits_visible;
	public static boolean delineation_visible;
	public static boolean puddle_visible;
	public static boolean delineating = false;
	private boolean firstStart;

	public static TextView wsdProgressText;
	public static Button simulateButton;
	public static ProgressBar wsdProgressBar;
	
	public static GroundOverlay pitsOverlay;
	public static GroundOverlay delineationOverlay;
	public static GroundOverlay puddleOverlay;
	public static GroundOverlay demOverlay;	
	
	public static GroundOverlayOptions pitsOverlayOptions;
	public static GroundOverlayOptions delineationOverlayOptions;
	public static GroundOverlayOptions puddleOverlayOptions;
	public static GroundOverlayOptions demOverlayOptions;
	public static MarkerOptions delineationMarkerOptions;
	
	public static Marker delineationMarker;
//	static Polygon delineationPolygon;
	static ArrayList<Polyline> demOutlines;

	public static Point delineationPoint;
	public static Point clickedPoint;
	public static LatLngBounds demBounds;
	

	static ArrayList<DemFile> dems;
	public static WatershedDataset watershedDataset;
	ProgressFragment progressFragment = null;
	public static ResultsPanelFragment resultsFragment = null;
	public static Field field;
	ElevationRaster raster;
	DemFile currentlyLoaded;
	RainfallSimConfig delineation_settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		context = this;
		// Create Map
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		map = mapFragment.getMap();
		map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		map.setMyLocationEnabled(true);
		map.setOnMarkerDragListener(this);
		map.setOnMapClickListener(this);
		UiSettings uiSettings = map.getUiSettings();
		uiSettings.setRotateGesturesEnabled(false);
		uiSettings.setTiltGesturesEnabled(false);
		uiSettings.setZoomControlsEnabled(false);
        gdal.AllRegister();
		
		// Create array of DEM outlines to be shown and clickable on the map
		demOutlines = new ArrayList<Polyline>();
		scanDEMs();
		
		// Create a new Field object and hand it a template bitmap to help initialize (this works better than creating one from scratch)
		Field.setMapFragment(mapFragment);
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.watershedelineation);
		field = new Field(bitmap, new LatLng(0.0, 0.0), new LatLng(0.0, 0.0), 0.0, 0.0);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		RainfallSimConfig.setDepth(Float.parseFloat(prefs.getString("pref_rainfall_amount", "1.0f")));
		demDirectory = prefs.getString("dem_dir", Environment.getExternalStorageDirectory().toString() + "/dem");
		coloring = true;
		currentlyDrawing = false;
		
		// Set inital transparency and visibility toggle setting
		setDemAlpha(1 - (float) prefs.getInt("pref_key_dem_trans_level", 50) / 100.0f);
		setDelineationAlpha(1 - (float) prefs.getInt("pref_key_delin_trans_level", 50) / 100.0f);
		setPuddleAlpha(1 - (float) prefs.getInt("pref_key_puddle_trans_level", 50) / 100.0f);
		setCatchmentsAlpha(1 - (float) prefs.getInt("pref_key_catchments_trans_level", 50) / 100.0f);
		dem_visible = true;
		pits_visible = false;
		puddle_visible = false;
		delineation_visible = false;
		
		Runtime rt = Runtime.getRuntime();
		long maxMemory = rt.maxMemory();
		Log.v("onCreate", "maxMemory:" + Long.toString(maxMemory));

		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		int memoryClass = am.getMemoryClass();
		Log.v("onCreate", "memoryClass:" + Integer.toString(memoryClass));
		
		// Get colors for DEM coloring (I think)
		hsvColors = new int[256];
		hsvTransparentColors = new int[256];
		float hsvComponents[] = {1.0f, 0.75f, 0.75f};
		for(int i = 0; i<255; i++) {
			hsvComponents[0] = 360.0f*i/255.0f;
			hsvColors[i] = HSVToColor(hsvComponents);
			hsvTransparentColors[i] = HSVToColor(128, hsvComponents);
		}

		//show help on first app start
		firstStart = prefs.getBoolean("first_start", true);
		if (firstStart) {
			//show help
			Intent intent = new Intent(this, InitialHelpActivity.class);
			startActivityForResult(intent, FIRST_START);

			//keep it from happening again
			SharedPreferences.Editor edit = prefs.edit();
			edit = prefs.edit();
			edit.putBoolean("first_start", false);
			edit.commit();
		}

		//load initial DEM if help menu isn't being shown
		if(!firstStart) {
			loadInitialDEM();
		}
		wsdProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
		wsdProgressBar.setVisibility(View.INVISIBLE);
		wsdProgressText = (TextView)findViewById(R.id.progress_message);
		wsdProgressText.setTextColor(Color.WHITE);
		wsdProgressText.setVisibility(View.INVISIBLE);

		simulateButton = (Button) findViewById(R.id.button_simulate);
		simulateButton.setVisibility(View.VISIBLE);
		simulateButton.setEnabled(false);
		simulateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//getActionBar().hide();
				simulateButton.setVisibility(View.GONE);

				for (int i=0; i < watershedDataset.pits.pitDataList.size(); i++) {
				}
				if(watershedDataset == null){

				} else {
					new RunSimulation().execute(watershedDataset);
				}
				//				showProgressFragment();
			}
		});
	}

	public void onMapClick (LatLng point) {
		if (pits_visible) {
			clickedPoint = field.getXYFromLatLng(point);
			if (clickedPoint != null) {
//				if (delinBitmap.getPixel(numcols - 1 - clickedPoint.x, clickedPoint.y) == Color.RED) {
//					resultsFragment.updateResults(1);
//				} else {
				selectedPitIndex = watershedDataset.pits.getIndexOf(watershedDataset.pits.pitIdMatrix[clickedPoint.y][clickedPoint.x]);
				pitsOverlay.remove();
				pitsOverlayOptions = new GroundOverlayOptions()
				.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.pits.highlightSelectedPit(selectedPitIndex)))
				.positionFromBounds(field.getFieldBounds())
				.transparency(pitsAlpha)
				.visible(pits_visible)
				.zIndex(1);
				pitsOverlay = map.addGroundOverlay(pitsOverlayOptions);
				pits_visible = true;
				resultsFragment.updateResults(0);
			}
		}

		//load DEM if clicked on
		DemFile dem;
		if (currentlyLoaded.getBounds().contains(point)) {
		} else {
			for(int i = 0; i<dems.size(); i++) {
				dem = dems.get(i);
				if ( (currentlyLoaded == null) || !dem.getFilename().equals(currentlyLoaded.getFilename())) {
					if(dem.getBounds().contains(point)) {
						currentlyLoaded = dem;
						raster = new ElevationRaster();
						new ReadElevationRasterTask(this, raster, dem.getFilename()).execute(dem.getFileUri());
						SharedPreferences.Editor editor = prefs.edit();
						editor.putString("last_dem", dem.getFileUri().getPath());
						editor.commit();
					}
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		myMenu = menu;
		// Make an action bar and don't display the app title
		ActionBar actionBar = getActionBar();
		actionBar.setTitle("Watershed Delineation");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Choose DEM Menu Item
		if (item.getItemId() == R.id.test) {
			WatershedDataset.getCLU();
//			WatershedDataset.Polygonize();
			return true;
		}
		else if (item.getItemId() == R.id.menu_choose_dem) {
			Intent i = new Intent(this, DataFileChooser.class);
			i.putExtra("path", demDirectory);
			startActivityForResult(i, 1);
			return true;
		}  else if (item.getItemId() == R.id.menu_settings) {
			Bundle data = new Bundle();
			data.putString("returnIntent", "back"); //After choose return back to this
			Intent i = new Intent(this, SettingsActivity.class);
			i.putExtras(data);
			startActivity(i);
			return true;
		} else if (item.getItemId() == R.id.menu_new_sim) {
			if (resultsFragment != null) {
				hideResultsFragment();
			}
			delineationMarker.remove();
			simulateButton.setVisibility(View.VISIBLE);
			simulateButton.setEnabled(false);
			MenuItem mi = myMenu.findItem(R.id.menu_new_sim);
			mi.setVisible(false);
			watershedDataset = null;
			if (pitsOverlay != null) {
				pitsOverlay.remove();
				pitsOverlay = null;
			}
			if (delineationOverlay != null) {
				delineationOverlay.remove();
				delineationOverlay = null;
			}
			if (puddleOverlay != null) {
				puddleOverlay.remove();
				puddleOverlay = null;
			}
			if (demOverlay != null) {
				demOverlay.remove();
			}
			finish();
			startActivity(getIntent());
			return true;
		}else if (item.getItemId() == R.id.menu_center) {
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(field.getFieldBounds(), 50));
			return true;
		} else if(item.getItemId() == R.id.menu_dem) {
			SharedPreferences.Editor edit = prefs.edit();
			if (dem_visible) {
				dem_visible = false;
			} else {
				dem_visible = true;
			}
			demOverlay.setVisible(dem_visible);
			demOverlayOptions.visible(dem_visible);
			edit.putBoolean("pref_key_dem_vis", dem_visible);
			edit.commit();
			item.setChecked(dem_visible);
			return true;
		} else if(item.getItemId() == R.id.menu_catchments) {
			SharedPreferences.Editor edit = prefs.edit();
			if (pits_visible) {
				pits_visible = false;
			} else {
				pits_visible = true;
			}
			pitsOverlay.setVisible(pits_visible);
			pitsOverlayOptions.visible(pits_visible);

			edit.putBoolean("pref_key_pits_vis", pits_visible);
			edit.commit();
			item.setChecked(pits_visible);
			return true;
		} else if(item.getItemId() == R.id.menu_delineation) {
			SharedPreferences.Editor edit = prefs.edit();
			if (delineation_visible) {
				delineation_visible = false;
			} else {
				delineation_visible = true;
				showResultsFragment();
			}
			delineationOverlay.setVisible(delineation_visible);
			delineationOverlayOptions.visible(delineation_visible);
			edit.putBoolean("pref_key_delin_vis", delineation_visible);
			edit.commit();
			item.setChecked(delineation_visible);
			return true;
		} else if(item.getItemId() == R.id.menu_puddles) {
			SharedPreferences.Editor edit = prefs.edit();
			if (puddle_visible) {
				puddle_visible = false;
			} else {
				puddle_visible = true;
			}
			puddleOverlay.setVisible(puddle_visible);
			puddleOverlayOptions.visible(puddle_visible);

			edit.putBoolean("pref_key_puddle_vis", puddle_visible);
			edit.commit();
			item.setChecked(puddle_visible);
			return true;
		} else if (item.getItemId() == R.id.menu_help) {
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void ProgressFragmentDone(WatershedDataset watershedDataset) {
		MainActivity.watershedDataset = watershedDataset;
		getActionBar().show();
		getActionBar().setTitle("Watershed Delineation");
		MenuItem mi = myMenu.findItem(R.id.menu_catchments);
		mi.setEnabled(true);
		mi = myMenu.findItem(R.id.menu_puddles);
		mi.setEnabled(true);
		mi = myMenu.findItem(R.id.menu_new_sim).setVisible(true);
		mi = myMenu.findItem(R.id.menu_delineation);
		if (!mi.isEnabled()) {
			mi.setEnabled(true);
		}
		
		//Show the pits on the field
		pits_visible = true;
		pitsOverlayOptions = new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.pits.pitsBitmap))
		.positionFromBounds(field.getFieldBounds())
		.transparency(pitsAlpha)
		.visible(pits_visible)
		.zIndex(1);
		pitsOverlay = map.addGroundOverlay(pitsOverlayOptions);

		puddle_visible = true;
		puddleOverlayOptions = new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.drawPuddles()))
		.positionFromBounds(field.getFieldBounds())
		.transparency(puddleAlpha)
		.visible(puddle_visible)
		.zIndex(2);
		puddleOverlay = map.addGroundOverlay(puddleOverlayOptions);
		
		LatLng midfield = new LatLng((MainActivity.field.getFieldBounds().southwest.latitude + MainActivity.field.getFieldBounds().northeast.latitude)/2.0, (MainActivity.field.getFieldBounds().southwest.longitude + MainActivity.field.getFieldBounds().northeast.longitude)/2.0);
		delineationMarkerOptions = new MarkerOptions()
		.position(midfield);
		delineationMarker = MainActivity.map.addMarker(delineationMarkerOptions);
		delineationMarker.setDraggable(true);
		
		delineating = true;
		delineationPoint = field.getXYFromLatLng(delineationMarker.getPosition());
//		delineationPoint = new Point(302, 315);
//		Log.w("delinpoint", delineationPoint.toString());
		selectedPitIndex = watershedDataset.pits.getIndexOf(watershedDataset.pits.pitIdMatrix[delineationPoint.y][delineationPoint.x]);
		showResultsFragment();
		delineation_visible = true;
		new DelineateWatershedTask(delineationPoint).execute();
		delineating = false;
//		Debug.stopMethodTracing();
	}

	private void showProgressFragment(){
		FragmentManager fm = getSupportFragmentManager();
		ProgressFragment fragment = new ProgressFragment();
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.progress_fragment_container, fragment, "progress_fragment");
		ft.commit();
		progressFragment = fragment; //Set global so we have a reference to this fragment
	}

	private void hideProgressFragment(){
		FragmentManager fm = getSupportFragmentManager();
		ProgressFragment fragment = (ProgressFragment) fm.findFragmentByTag("progress_fragment");
		// Do transition
		FragmentTransaction ft = fm.beginTransaction();
		ft.remove(fragment);
		ft.commit();
		progressFragment = null;
	}

	public interface ProgressFragmentListener {
		public WatershedDataset ProgressFragmentGetData();
		public void ProgressFragmentDone(WatershedDataset watershedDataset);
	}
	
	//Rainfall Simulation Asynchronous Task
	private class RunSimulation extends AsyncTask<WatershedDataset, Object, WatershedDataset> implements WatershedDataset.WatershedDatasetListener {
		protected void onPreExecute() {
			super.onPreExecute();
			wsdProgressBar.setVisibility(View.VISIBLE);
		}

		protected WatershedDataset doInBackground(WatershedDataset... inputWds) {
			// Background Work
			inputWds[0].setTask(this);
			if (inputWds[0].fillPits() != true) {
			}
			return inputWds[0];
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			MainActivity.wsdProgressBar.setProgress((Integer) values[0]);
		}

		protected void onPostExecute(WatershedDataset result) {
			super.onPostExecute(result);
			ProgressFragmentDone(result);
		}

		@Override
		public void simulationOnProgress(int progress, String status) {
			Object[] array = new Object[3];
			array[0] = progress;
			array[1] = status;
			publishProgress(array);
		}

		@Override
		public void simulationDone() {
		}
	}

	@Override
	public WatershedDataset ProgressFragmentGetData() {
		return watershedDataset;
	}

	private void showResultsFragment(){
		FragmentManager fm = getSupportFragmentManager();
		ResultsPanelFragment fragment = new ResultsPanelFragment();
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.results_fragment_container, fragment, "results_fragment");
		ft.commit();
		resultsFragment = fragment; //Set global so we have a reference to this fragment
	}
	private void hideResultsFragment(){
		FragmentManager fm = getSupportFragmentManager();
		ResultsPanelFragment fragment = (ResultsPanelFragment) fm.findFragmentByTag("results_fragment");
		// Do transition
		FragmentTransaction ft = fm.beginTransaction();
		ft.remove(fragment);
		ft.commit();
		progressFragment = null;
	}

	@Override
	public WatershedDataset ResultsPanelFragmentGetData() {
		return watershedDataset;
	}

	public static float getAlpha() {
		return demAlpha;
	}

	public static void setDemAlpha(float a) {
		demAlpha = a;
		demOverlay.setTransparency(demAlpha);
	}

	public static void setCatchmentsAlpha(float a) {
		pitsAlpha = a;
		if (pitsOverlay != null) {
			pitsOverlay.setTransparency(pitsAlpha);
		}
	}

	public static void setPuddleAlpha(float a) {
		puddleAlpha = a;
		if (puddleOverlay != null) {
			puddleOverlay.setTransparency(puddleAlpha);
		}
	}
	
	public static void setDelineationAlpha(float a) {
		delineationAlpha = a;
		if (delineationOverlay != null) {
			delineationOverlay.setTransparency(delineationAlpha);
		}
	}

	//looks through contents of DEM directory and displays outlines of all DEMs there
	public static void scanDEMs() {
		//scan DEM directory
		String path = demDirectory;
		DemFile dem;
		dems = new ArrayList<DemFile>();
		File f = new File(path);
		Polyline outline;
		demOutlines = new ArrayList<Polyline>();

		if (f.isDirectory()) {
			File file[] = f.listFiles();

			for (int i=0; i < file.length; i++)
			{
				dem = ReadGeoTiffMetadata.readMetadata(file[i]);
				if(i==0) {
					demBounds = new LatLngBounds(new LatLng(dem.getSw_lat(), dem.getSw_long()),
							new LatLng(dem.getNe_lat(), dem.getNe_long()));
				}
				dems.add(dem);
				demOutlines.add(map.addPolyline(new PolylineOptions().add(new LatLng(dem.getSw_lat(), dem.getSw_long()))
						.add(new LatLng(dem.getSw_lat(), dem.getNe_long()))
						.add(new LatLng(dem.getNe_lat(), dem.getNe_long()))
						.add(new LatLng(dem.getNe_lat(), dem.getSw_long()))
						.add(new LatLng(dem.getSw_lat(), dem.getSw_long()))
						.color(Color.RED)));
				demBounds = demBounds.including(new LatLng(dem.getSw_lat(), dem.getSw_long()));
				demBounds = demBounds.including(new LatLng(dem.getNe_lat(), dem.getNe_long()));
			}
		}
	}

	//picks which DEM to load upon app start
	public void loadInitialDEM() {
		//attempt to load last used DEM, if it still exists
		File demFile = new File(prefs.getString("last_dem", "foo"));
		if(demFile.isFile()) {
			raster = new ElevationRaster();
			new ReadElevationRasterTask(this, raster, demFile.getName()).execute(UritoURI(Uri.fromFile(demFile)));
			setCurrentlyLoaded(prefs.getString("last_dem", "foo"));
			return;
		}
		String path = demDirectory;
		File f = new File(path);

		//if DEM dir doesn't exist, create it and copy sample TIFF in, then open it
		if (!f.isDirectory()) {
			f.mkdir();
			copyAssets();
			raster = new ElevationRaster();
			new ReadElevationRasterTask(this, raster).execute(UritoURI(Uri.fromFile(new File(demDirectory+"Feldun.tif"))));
			setCurrentlyLoaded(demDirectory+"Feldun.tif");
			return;
		}
		//selected directory exists
		else {
			//list files in DEM dir
			File file[] = f.listFiles();
			ArrayList<File> tiffs = new ArrayList<File>();

			//count number of TIFFs in dir
			int count = 0;
			for(int i = 0; i<file.length; i++) {
				if(file[i].getName().contains(".tif")) {
					count++;
					tiffs.add(file[i]);
				}
			}

			//if no TIFFs, copy sample into dir and open
			if (count == 0) {
				copyAssets();
				raster = new ElevationRaster();
				new ReadElevationRasterTask(this, raster).execute(UritoURI(Uri.fromFile(new File(demDirectory+"Feldun.tif"))));
				setCurrentlyLoaded(demDirectory+"Feldun.tif");
			}
			//if one TIFF, open it
			else if(count == 1) {
				raster = new ElevationRaster();
				new ReadElevationRasterTask(this, raster, tiffs.get(0).getName()).execute(UritoURI(Uri.fromFile(tiffs.get(0))));
				setCurrentlyLoaded(tiffs.get(0).getPath());
			}
			//if multiple TIFFs, let user choose
			else {
				Intent intent = new Intent(context, DataFileChooser.class);
				intent.putExtra("path", demDirectory);
				startActivityForResult(intent, INITIAL_LOAD);
			}
		}
	}
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		//handle data from file manager

		if (requestCode == FIRST_START) {
			loadInitialDEM();
			return;
		}
		System.out.print("Intent Handler");
		System.out.print(data);

		if (data != null) {
			if (data.getData().toString().contains(".tif")) {
				fileUri = data.getData();
				java.net.URI juri = null;
				try {
					juri = new java.net.URI(fileUri.getScheme(),
							fileUri.getSchemeSpecificPart(),
							fileUri.getFragment());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				ElevationRaster raster = new ElevationRaster();
				String filename = fileUri.getPath().split("/")[fileUri.getPath().split("/").length-1];
				new ReadElevationRasterTask(this, raster, filename).execute(juri);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("last_dem", fileUri.getPath());
				editor.commit();

				setCurrentlyLoaded(prefs.getString("last_dem", "foo"));
				return;
			}
		}
		if (requestCode == INITIAL_LOAD && data == null) {
			raster = new ElevationRaster();
			DemFile demToLoad = dems.get(0);
			String filename = demToLoad.getFilename();
			new ReadElevationRasterTask(this, raster, filename).execute(demToLoad.getFileUri());
		}
	}

	public static void onFileRead(ElevationRaster inputRaster) {
		field.setBitmap(inputRaster.getBitmap());
		field.setBounds(inputRaster.getBounds());

		map.animateCamera(CameraUpdateFactory.newLatLngBounds(inputRaster.getBounds(), 50));
		field.updatePolyLine();
		updateColors(field);
		new LoadWatershedDatasetTask(inputRaster).execute();
	}

	public static void updateColors(Field field) {
		if (!currentlyDrawing) {
			currentlyDrawing = true;
			int width = field.getElevationBitmap().getWidth();
			int height = field.getElevationBitmap().getHeight();
			int[] pixels = new int[width * height];
			field.getElevationBitmap().getPixels(pixels, 0, width, 0, 0, width, height);
			Bitmap bitmap = field.getElevationBitmap().copy(field.getElevationBitmap().getConfig(), true);
			int c;
			for (int i = 0; i < (width * height); i++) {
				c=pixels[i] & 0xFF;
				pixels[i] =  hsvColors[c];
			}
			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
			field.setBitmap(bitmap);
			//remove old map overlay and create new one
			field.createOverlay();
			if (dem_visible) {
				demOverlay.setTransparency(demAlpha);
			}
			currentlyDrawing = false;
		}
	}

	//copies a file from assets to SD
	private void copyAssets() {
		AssetManager assetManager = getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
		}
		String filename = "Feldun.tif";
		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open(filename);
			File outFile = new File(demDirectory, filename);
			out = new FileOutputStream(outFile);
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch(IOException e) {
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}

	//tell app which DEM is currently loaded, so it isn't reloaded if clicked on
	public void setCurrentlyLoaded(String filename) {
		DemFile dem;
		for(int i = 0; i<dems.size(); i++) {
			dem = dems.get(i);
			if (filename.equals(dem.getFilename())) {
				currentlyLoaded = dem;
			}
		}
	}

	private URI UritoURI(Uri fileUri) {
		URI juri = null;
		try {
			juri = new java.net.URI(fileUri.getScheme(),
					fileUri.getSchemeSpecificPart(),
					fileUri.getFragment());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return juri;
	}

	//remove polylines showing DEM outlines, for use when DEM folder is changed
	public static void removeDemOutlines() {
		Iterator<Polyline> outlines = demOutlines.iterator();
		while(outlines.hasNext()) {
			outlines.next().remove();
		}
	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		if (!delineating) {
			delineating = true;
			delineationPoint = field.getXYFromLatLng(delineationMarker.getPosition());
			if (delineationPoint == null) {
				Toast toast = Toast.makeText(context, "Location to delineate must be within bounded area.", Toast.LENGTH_SHORT);
				toast.show();
			} else {
				if(resultsFragment == null || resultsFragment.isVisible() == false){
					showResultsFragment();
				}
				delineationMarkerOptions.position(delineationMarker.getPosition())
//				.title(Integer.toString(watershedDataset.pits.pitIdMatrix[delineationPoint.y][delineationPoint.x]));
				.title(delineationPoint.toString());
				new DelineateWatershedTask(delineationPoint).execute();			
			}
		}
		delineating = false;
	}
	@Override
	public void onMarkerDrag(Marker marker) {		
	}
	@Override
	public void onMarkerDragStart(Marker marker) {		
	}
}

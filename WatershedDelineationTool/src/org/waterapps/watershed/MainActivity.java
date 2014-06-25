package org.waterapps.watershed;

import java.io.File;

import org.waterapps.lib.DemLoadUtils;
import org.waterapps.lib.WmacDemLoadUtilsListener;
import org.waterapps.watershed.ProgressFragment.ProgressFragmentListener;
import org.waterapps.watershed.ResultsPanelFragment.ResultsPanelFragmentListener;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.filebrowser.DataFileChooser;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.openatk.openatklib.atkmap.ATKMap;
import com.openatk.openatklib.atkmap.ATKSupportMapFragment;
import com.openatk.openatklib.atkmap.listeners.ATKMapClickListener;
import com.openatk.openatklib.atkmap.listeners.ATKPointClickListener;
import com.openatk.openatklib.atkmap.listeners.ATKPointDragListener;
import com.openatk.openatklib.atkmap.models.ATKPoint;
import com.openatk.openatklib.atkmap.views.ATKPointView;

public class MainActivity extends FragmentActivity implements ATKMapClickListener, ProgressFragmentListener, ATKPointDragListener, ATKPointClickListener, ResultsPanelFragmentListener, WmacDemLoadUtilsListener {
	ProgressFragmentListener pflistener;
	LocationManager locationManager;
	private static Menu myMenu = null;
	public static ATKMap map;
	private ATKSupportMapFragment atkmapFragment;
	private UiSettings mapSettings;
	// Startup position
	private static final float START_LAT = 40.428712f;
	private static final float START_LNG = -86.913819f;
	private static final float START_ZOOM = 17.0f;
	
	public static SharedPreferences prefs;
	public static Context context;
	
	String wsdStatusMsg = "Reading DEM";
	public static float demAlpha;
	public static float delineationAlpha;
	public static float puddleAlpha;
	public static float pitsAlpha;

	public static int selectedPitIndex;
	private static final int FIRST_START = 42;
	private static final int INITIAL_LOAD = 6502;
	private static final int FILE_CHOOSER = 6503;
	private static final int FILE_PATH_CHOOSER = 6504;
	
	public static boolean demVisible;
	public static boolean pitsVisible;
	public static boolean delineationVisible;
	public static boolean puddleVisible;
	public static boolean delineating = false;
	private boolean firstStart;

	public static TextView wsdProgressText;
	public static Button simulateButton;
	public static ProgressBar wsdProgressBar;
	
	public static GroundOverlay pitsOverlay;
	public static GroundOverlay delineationOverlay;
	public static GroundOverlay puddleOverlay;
//	public static GroundOverlayOptions pitsOverlayOptions;
//	public static GroundOverlayOptions delineationOverlayOptions;
//	public static GroundOverlayOptions puddleOverlayOptions;
	
	public static ATKPointView delineationMarker;
	public static Point delineationPointRC;
	public static Point clickedPoint;

	public static WatershedDataset watershedDataset;
	ProgressFragment progressFragment = null;
	public static ResultsPanelFragment resultsFragment = null;
	RainfallSimConfig delineation_settings;
	DemLoadUtils demLoadUtils;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		FragmentManager fm = getSupportFragmentManager();
		atkmapFragment = (ATKSupportMapFragment) fm.findFragmentById(R.id.map);
		
		if (savedInstanceState == null) {
			// First incarnation of this activity.
			atkmapFragment.setRetainInstance(true);
		} else {
			// Reincarnated activity. The obtained map is the same map instance in the previous
			// activity life cycle. There is no need to reinitialize it.
			map = atkmapFragment.getAtkMap();
		}
		setUpMapIfNeeded();
		context = this;		
		UiSettings uiSettings = map.getUiSettings();
		uiSettings.setRotateGesturesEnabled(false);
		uiSettings.setTiltGesturesEnabled(false);
		uiSettings.setZoomControlsEnabled(false);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		RainfallSimConfig.setDepth(Float.parseFloat(prefs.getString("pref_rainfall_amount", "1.0f")));
		
		demLoadUtils = new DemLoadUtils(context, map, prefs);
		demLoadUtils.registerWmacDemLoadUtilsListener(this);
		
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
			demLoadUtils.loadInitialDem();
			
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

				for (int i=0; i < WatershedDataset.pits.pitDataList.size(); i++) {
				}
				if(watershedDataset == null){

				} else {
					new RunSimulation().execute(watershedDataset);
				}
				//				showProgressFragment();
			}
		});
		
		// Set inital transparency and visibility toggle setting
		setDemAlpha(1 - (float) prefs.getInt("pref_key_dem_trans_level", 50) / 100.0f);
		setDelineationAlpha(1 - (float) prefs.getInt("pref_key_delin_trans_level", 50) / 100.0f);
		setPuddleAlpha(1 - (float) prefs.getInt("pref_key_puddle_trans_level", 50) / 100.0f);
		setCatchmentsAlpha(1 - (float) prefs.getInt("pref_key_catchments_trans_level", 50) / 100.0f);
		demVisible = true;
		pitsVisible = false;
		puddleVisible = false;
		delineationVisible = false;
	}
	
	//Group: ATK
	private void setUpMapIfNeeded() {
		if (map == null) {
			//Map is null try to find it
			map = ((ATKSupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getAtkMap();
		}

		if (atkmapFragment.getRetained() == false) {
			//New map, we need to set it up
			setUpMap();
			
			//Move to where we were last time
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Float startLat = prefs.getFloat("StartupLat", START_LAT);
			Float startLng = prefs.getFloat("StartupLng", START_LNG);
			Float startZoom = prefs.getFloat("StartupZoom", START_ZOOM);
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(startLat, startLng), startZoom));
		}
		
		//Setup atkmap listeners, note: these are the atkmap listeners, per object listeners can override these ie. (ATKPoint.setOnClickListener())
		map.setOnPointClickListener(this);
		map.setOnPointDragListener(this);
//		map.setOnPolygonClickListener(this);
		map.setOnMapClickListener(this);
	}
	
	private void setUpMap() {
		//Set map settings
		mapSettings = map.getUiSettings();
		mapSettings.setZoomControlsEnabled(false);
		mapSettings.setMyLocationButtonEnabled(false);
		mapSettings.setTiltGesturesEnabled(false);
		map.setMyLocationEnabled(true);
		map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
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

	// Action Bar and Overflow Menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Choose DEM Menu Item
		if (item.getItemId() == R.id.test) {
//			WatershedDataset.getCLU();
			WatershedDataset.polygonize(demLoadUtils.getLoadedDemData().getFilePath(), WatershedDataset.pits.pitIdMatrix, demLoadUtils.getDemDirectory()+"/catchments");
			return true;
		}
		else if (item.getItemId() == R.id.menu_choose_dem) {
			Intent i = new Intent(this, DataFileChooser.class);
			i.putExtra("path", demLoadUtils.getDemDirectory());
			startActivityForResult(i, FILE_CHOOSER);
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
			if (demLoadUtils.getLoadedDemData().getGroundOverlay() != null) {
				demLoadUtils.getLoadedDemData().getGroundOverlay().remove();
			}
			demLoadUtils = null;
			finish();
			startActivity(getIntent());
			return true;
		}else if (item.getItemId() == R.id.menu_center) {
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(demLoadUtils.getLoadedDemData().getDemFile().getBounds(), 50));
			return true;
		} else if(item.getItemId() == R.id.menu_dem) {
			SharedPreferences.Editor edit = prefs.edit();
			if (demVisible) {
				demVisible = false;
			} else {
				demVisible = true;
			}
			demLoadUtils.getLoadedDemData().getGroundOverlay().setVisible(demVisible);
			edit.putBoolean("pref_key_dem_vis", demVisible);
			edit.commit();
			item.setChecked(demVisible);
			return true;
		} else if(item.getItemId() == R.id.menu_catchments) {
			SharedPreferences.Editor edit = prefs.edit();
			if (pitsVisible) {
				pitsVisible = false;
			} else {
				pitsVisible = true;
			}
			pitsOverlay.setVisible(pitsVisible);
			edit.putBoolean("pref_key_pits_vis", pitsVisible);
			edit.commit();
			item.setChecked(pitsVisible);
			return true;
		} else if(item.getItemId() == R.id.menu_delineation) {
			SharedPreferences.Editor edit = prefs.edit();
			if (delineationVisible) {
				delineationVisible = false;
			} else {
				delineationVisible = true;
				showResultsFragment();
			}
			delineationOverlay.setVisible(delineationVisible);
			edit.putBoolean("pref_key_delin_vis", delineationVisible);
			edit.commit();
			item.setChecked(delineationVisible);
			return true;
		} else if(item.getItemId() == R.id.menu_puddles) {
			SharedPreferences.Editor edit = prefs.edit();
			if (puddleVisible) {
				puddleVisible = false;
			} else {
				puddleVisible = true;
			}
			puddleOverlay.setVisible(puddleVisible);

			edit.putBoolean("pref_key_puddle_vis", puddleVisible);
			edit.commit();
			item.setChecked(puddleVisible);
			return true;
		} else if (item.getItemId() == R.id.menu_help) {
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	// Finished loading the DEM
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
		
		//Show the catchment/pit map
		pitsVisible = true;
		GroundOverlayOptions pitsOverlayOptions = new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.altDrawPits()))
		.positionFromBounds(demLoadUtils.getLoadedDemData().getBounds())
		.transparency(pitsAlpha)
		.visible(pitsVisible)
		.zIndex(1);
		pitsOverlay = map.addGroundOverlay(pitsOverlayOptions);

		// Show the filled areas
		puddleVisible = true;
		GroundOverlayOptions puddleOverlayOptions = new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.drawPuddles()))
		.positionFromBounds(demLoadUtils.getLoadedDemData().getBounds())
		.transparency(puddleAlpha)
		.visible(puddleVisible)
		.zIndex(2);
		puddleOverlay = map.addGroundOverlay(puddleOverlayOptions);
		
		//Add Delineation Point to the center of the DEM and delineate this watershed to begin with
		LatLng where = map.getCameraPosition().target;
		ATKPoint newPoint = new ATKPoint("Delineator", where);
		delineationMarker = map.addPoint(newPoint);
		delineationMarker.setSuperDraggable(true);
		delineationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(), 100, 200);
		delineating = true;
		delineationPointRC = demLoadUtils.getLoadedDemData().getXYFromLatLng(delineationMarker.getAtkPoint().position);
//		delineationPoint = new Point(302, 315);
//		Log.w("delinpoint", delineationPoint.toString());
		
		// Display results corresponding to selected DEM or delineated watershed area
		selectedPitIndex = WatershedDataset.pits.getIndexOf(WatershedDataset.pits.pitIdMatrix[delineationPointRC.y][delineationPointRC.x]);
		showResultsFragment();
		delineationVisible = true;
		new DelineateWatershedTask(delineationPointRC, demLoadUtils).execute();
	}

	//Group: watershedUI
	private void showProgressFragment(){
		FragmentManager fm = getSupportFragmentManager();
		ProgressFragment fragment = new ProgressFragment();
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.progress_fragment_container, fragment, "progress_fragment");
		ft.commit();
		progressFragment = fragment; //Set global so we have a reference to this fragment
	}
	
	//Group: watershedUI
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

	//Group: watershedUI
	private void showResultsFragment(){
		FragmentManager fm = getSupportFragmentManager();
		ResultsPanelFragment fragment = new ResultsPanelFragment();
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.results_fragment_container, fragment, "results_fragment");
		ft.commit();
		resultsFragment = fragment; //Set global so we have a reference to this fragment
	}
	//Group: watershedUI
	private void hideResultsFragment(){
		FragmentManager fm = getSupportFragmentManager();
		ResultsPanelFragment fragment = (ResultsPanelFragment) fm.findFragmentByTag("results_fragment");
		// Do transition
		FragmentTransaction ft = fm.beginTransaction();
		ft.remove(fragment);
		ft.commit();
		progressFragment = null;
	}
	//Group: watershedUI
	@Override
	public WatershedDataset ResultsPanelFragmentGetData() {
		return watershedDataset;
	}

	//Group: watershedUI
	public float getAlpha() {
		return demAlpha;
	}

	//Group: watershedUI
	public void setDemAlpha(float a) {
		demAlpha = a;
		if (demLoadUtils.getLoadedDemData() != null) {
			if (demLoadUtils.getLoadedDemData().getGroundOverlay() != null) {
				demLoadUtils.getLoadedDemData().getGroundOverlay().setTransparency(demAlpha);
			}
		}
		
	}

	//Group: watershedUI
	public void setCatchmentsAlpha(float a) {
		pitsAlpha = a;
		if (pitsOverlay != null) {
			pitsOverlay.setTransparency(pitsAlpha);
		}
	}

	//Group: watershedUI
	public void setPuddleAlpha(float a) {
		puddleAlpha = a;
		if (puddleOverlay != null) {
			puddleOverlay.setTransparency(puddleAlpha);
		}
	}
	
	//Group: watershedUI
	public void setDelineationAlpha(float a) {
		delineationAlpha = a;
		if (delineationOverlay != null) {
			delineationOverlay.setTransparency(delineationAlpha);
		}
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		// Handle data from file manager
		if (requestCode == FIRST_START) {
			demLoadUtils.loadInitialDem();
			return;
		}
		// Handle return from the file chooser
//		if (requestCode == INITIAL_LOAD) {
//			demLoadUtils.loadFileChooserData(data);
//			return;
//		}
		
		if (requestCode == FILE_PATH_CHOOSER) {
			demLoadUtils.setNewDemDirectory(data.getStringExtra("directory"));  //edits the path pref within this function
		}
		
		if (requestCode == FILE_CHOOSER) {
			if (data.getData().toString().contains(".tif")) {
				File f = new File(data.getData().getPath());
				if (demLoadUtils.getDemDirectory() != f.getParent()) {
					demLoadUtils.setNewDemDirectory(f.getParent());
				}
				demLoadUtils.loadDem(data.getData().getPath());
			}
		}
	}
	
	public void onMapClick (LatLng latLng) {
		if (pitsVisible) {
			clickedPoint = demLoadUtils.getLoadedDemData().getXYFromLatLng(latLng);
			if (clickedPoint != null) {
//				if (delinBitmap.getPixel(numcols - 1 - clickedPoint.x, clickedPoint.y) == Color.RED) {
//					resultsFragment.updateResults(1);
//				} else {
				selectedPitIndex = WatershedDataset.pits.getIndexOf(WatershedDataset.pits.pitIdMatrix[clickedPoint.y][clickedPoint.x]);
				pitsOverlay.remove();
				GroundOverlayOptions pitsOverlayOptions = new GroundOverlayOptions()
				.image(BitmapDescriptorFactory.fromBitmap(WatershedDataset.pits.highlightSelectedPit(selectedPitIndex)))
				.positionFromBounds(demLoadUtils.getLoadedDemData().getBounds())
				.transparency(pitsAlpha)
				.visible(pitsVisible)
				.zIndex(1);
				pitsOverlay = map.addGroundOverlay(pitsOverlayOptions);
				pitsVisible = true;
				resultsFragment.updateResults(0);
			}
		}

		demLoadUtils.loadClickedDem(latLng);
	}


	@Override
	public boolean onPointDrag(ATKPointView pointView) {
		return false;
	}

	@Override
	public boolean onPointDragEnd(ATKPointView pointView) {
		if (!delineating) {
			delineating = true;
			delineationPointRC = demLoadUtils.getLoadedDemData().getXYFromLatLng(delineationMarker.getAtkPoint().position);
			if (delineationPointRC == null) {
				Toast toast = Toast.makeText(context, "Location to delineate must be within bounded area.", Toast.LENGTH_SHORT);
				toast.show();
			} else {
				if(resultsFragment == null || resultsFragment.isVisible() == false){
					showResultsFragment();
				}
//				delineationMarkerOptions.position(delineationMarker.getAtkPoint().position)
//				.title(delineationPointRC.toString());
				new DelineateWatershedTask(delineationPointRC, demLoadUtils).execute();			
			}
		}
		delineating = false;
		return false;
	}

	@Override
	public boolean onPointDragStart(ATKPointView pointView) {
		return false;
	}

	@Override
	public boolean onPointClick(ATKPointView pointView) {
		return false;
	}

	@Override
	public void onDemDataLoad() {
		new LoadWatershedDatasetTask(demLoadUtils.getLoadedDemData()).execute();
	}
}

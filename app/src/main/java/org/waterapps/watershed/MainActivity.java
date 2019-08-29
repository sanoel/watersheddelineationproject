package org.waterapps.watershed;

import java.io.File;

import org.waterapps.lib.DemData;
import org.waterapps.lib.DemLoadUtils;
import org.waterapps.lib.WmacListener;
import org.waterapps.watershed.ProgressFragment.ProgressFragmentListener;
import org.waterapps.watershed.ResultsPanelFragment.ResultsPanelFragmentListener;

import android.Manifest;
import android.app.ActionBar;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.openatk.openatklib.atkmap.ATKMap;
import com.openatk.openatklib.atkmap.ATKSupportMapFragment;
import com.openatk.openatklib.atkmap.listeners.ATKMapClickListener;
import com.openatk.openatklib.atkmap.listeners.ATKPointClickListener;
import com.openatk.openatklib.atkmap.listeners.ATKPointDragListener;
import com.openatk.openatklib.atkmap.listeners.ATKPolygonClickListener;
import com.openatk.openatklib.atkmap.models.ATKPoint;
import com.openatk.openatklib.atkmap.views.ATKPointView;
import com.openatk.openatklib.atkmap.views.ATKPolygonView;

public class MainActivity extends FragmentActivity implements ATKSupportMapFragment.onMapReadyListener, ATKMapClickListener, ProgressFragmentListener, ATKPointDragListener, ATKPointClickListener, ResultsPanelFragmentListener, WmacListener, ATKPolygonClickListener, WatershedDatasetDoneListener, OnSharedPreferenceChangeListener {
	private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
	private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 11;
	private static final int MY_PERMISSION_READ_EXTERNAL_STORAGE = 11;
	private static final int MY_PERMISSION_WRITE_EXTERNAL_STORAGE = 11;

	ProgressFragmentListener pflistener;
	LocationManager locationManager;
	private static Menu myMenu = null;
	private ATKMap map;
	private ATKSupportMapFragment atkmapFragment;
	private UiSettings mapSettings;
	// Startup position
	private static final float START_LAT = 40.428712f;
	private static final float START_LNG = -86.913819f;
	private static final float START_ZOOM = 17.0f;
	
	public SharedPreferences prefs;
	public Context context;
	
	String wsdStatusMsg = "Reading DEM";
//	private static float demAlpha;
//	private static float delineationAlpha;
//	private static float puddleAlpha;
//	private static float catchmentsAlpha;

	private static int selectedCatchmentIndex;
	private static final int FIRST_START = 42;
	private static final int INITIAL_LOAD = 6502;
	private static final int FILE_CHOOSER = 6503;
	private static final int FILE_PATH_CHOOSER = 6504;
	
	public static boolean delineating = false;
	private boolean firstStart;

	private TextView wsdProgressText;
	private Button simulateButton;
	private ProgressBar wsdProgressBar;
	
	private GroundOverlay catchmentsOverlay;
	private GroundOverlay delineationOverlay;
	private GroundOverlay puddleOverlay;
	
	private ATKPointView delineationMarker;
	private ATKPointView selectedCatchmentOutlet;
	private ATKPointView selectedCatchmentBottom;
	private Point delineationXyPoint;
	private Point clickedPoint;

	private WatershedDataset watershedDataset;
	ProgressFragment progressFragment = null;
	private ResultsPanelFragment resultsFragment = null;
	DelineationAppConfigs appConfigs;
	private DemLoadUtils demLoadUtils;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		FragmentManager fm = getSupportFragmentManager();
		this.atkmapFragment = (ATKSupportMapFragment) fm.findFragmentById(R.id.map);

		if (savedInstanceState == null) {
			// First incarnation of this activity.
			atkmapFragment.setRetainInstance(true);
		} else {
			// Reincarnated activity. The obtained map is the same map instance in the previous
			// activity life cycle. There is no need to reinitialize it.
			map = atkmapFragment.getAtkMap();
		}
	}

	@Override
	public void onMapReadyNow(ATKSupportMapFragment atkSmp) {
		if (atkSmp instanceof ATKSupportMapFragment) {
			setUpMapIfNeeded();
			context = this;
			UiSettings uiSettings = map.getUiSettings();
			uiSettings.setRotateGesturesEnabled(false);
			uiSettings.setTiltGesturesEnabled(false);
			uiSettings.setZoomControlsEnabled(false);

			prefs = PreferenceManager.getDefaultSharedPreferences(this);
			DelineationAppConfigs.setDepth(Float.parseFloat(prefs.getString("pref_rainfall_amount", "1.0f")));


			if (ContextCompat.checkSelfPermission(this,
					android.Manifest.permission.READ_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {

				// Permission is not granted
				// Should we show an explanation?
				if (ActivityCompat.shouldShowRequestPermissionRationale(this,
						android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
					// Show an explanation to the user *asynchronously* -- don't block
					// this thread waiting for the user's response! After the user
					// sees the explanation, try again to request the permission.
				} else {
					// No explanation needed; request the permission
					ActivityCompat.requestPermissions(this,
							new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
							MY_PERMISSION_READ_EXTERNAL_STORAGE);

					// MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
					// app-defined int constant. The callback method gets the
					// result of the request.
				}
			} else {
				// Permission has already been granted
				DemLoadUtils demLoadUtils2 = new DemLoadUtils(this, map, prefs, MY_PERMISSION_WRITE_EXTERNAL_STORAGE);
				this.demLoadUtils = demLoadUtils2;
				setDemLoadUtils(this.demLoadUtils);
				this.demLoadUtils.registerWmacListener(this);
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
			if (!firstStart) {
				Log.w("DEMLOADUTILS", Boolean.toString(this.demLoadUtils==null));
				this.demLoadUtils.loadInitialDem(this);
			}
			wsdProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
			wsdProgressBar.setVisibility(View.INVISIBLE);
			wsdProgressBar.getProgressDrawable().setColorFilter(4892122, Mode.MULTIPLY);
			wsdProgressText = (TextView) findViewById(R.id.progress_message);
			wsdProgressText.setTextColor(Color.WHITE);
			wsdProgressText.setVisibility(View.INVISIBLE);

			simulateButton = (Button) findViewById(R.id.button_simulate);
			simulateButton.setVisibility(View.INVISIBLE);
			simulateButton.setEnabled(false);
			simulateButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					//getActionBar().hide();
					simulateButton.setVisibility(View.GONE);

					for (int i = 0; i < watershedDataset.getPitRaster().pitDataList.size(); i++) {
					}
					if (watershedDataset == null) {

					} else {
						new RunSimulation().execute(watershedDataset);
					}
				}
			});
		}
		
		// Set inital transparency and visibility toggle setting
//		setDemAlpha(1 - (float) prefs.getInt("pref_key_dem_trans_level", 50) / 100.0f);
//		setDelineationAlpha(1 - (float) prefs.getInt("pref_key_delin_trans_level", 50) / 100.0f);
//		setPuddleAlpha(1 - (float) prefs.getInt("pref_key_puddle_trans_level", 50) / 100.0f);
//		setCatchmentsAlpha(1 - (float) prefs.getInt("pref_key_catchments_trans_level", 50) / 100.0f);
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
		map.setOnPolygonClickListener(this);
		map.setOnMapClickListener(this);
	}
	
	private void setUpMap() {
		//Set map settings
		mapSettings = map.getUiSettings();
		mapSettings.setZoomControlsEnabled(false);
		mapSettings.setMyLocationButtonEnabled(false);
		mapSettings.setTiltGesturesEnabled(false);
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {

			// Permission is not granted
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					android.Manifest.permission.ACCESS_FINE_LOCATION)) {
				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
			} else {
				// No explanation needed; request the permission
				ActivityCompat.requestPermissions(this,
						new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
						MY_PERMISSION_ACCESS_FINE_LOCATION);

				// MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			}
		} else {
			// Permission has already been granted
			map.setMyLocationEnabled(true);
		}

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

	@Override
	public void onAttachFragment(Fragment fragment) {
		if (fragment instanceof ATKSupportMapFragment) {
			ATKSupportMapFragment atkMapFrag = (ATKSupportMapFragment) fragment;
			atkMapFrag.setOnMapReadyListener(this);
		}
	}

	// Action Bar and Overflow Menu
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Choose DEM Menu Item
//		if (item.getItemId() == R.id.test) {
////			WatershedDataset.getCLU();
////			WatershedDataset.writeRaster(demLoadUtils.getLoadedDemData().getFilePath(), WatershedDataset.pits.pitIdMatrix, demLoadUtils.getDemDirectory()+"/catchments.tif");
//			return true;
//		}
		if (item.getItemId() == R.id.menu_choose_dem) {
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
			simulateButton.setVisibility(View.INVISIBLE);
			simulateButton.setEnabled(false);
			MenuItem mi = myMenu.findItem(R.id.menu_new_sim);
			mi.setVisible(false);
			watershedDataset = null;
			if (catchmentsOverlay != null) {
				catchmentsOverlay.remove();
				catchmentsOverlay = null;
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
			setDemLoadUtils(null);
			finish();
			startActivity(getIntent());
			return true;
		}else if (item.getItemId() == R.id.menu_center) {
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(demLoadUtils.getLoadedDemData().getDemFile().getBounds(), 50));
			return true;
		} else if(item.getItemId() == R.id.menu_dem) {
			if (demLoadUtils.getLoadedDemData().getGroundOverlay().isVisible()) {
				demLoadUtils.getLoadedDemData().getGroundOverlay().setVisible(false);
			} else {
				demLoadUtils.getLoadedDemData().getGroundOverlay().setVisible(true);
			}
			item.setChecked(demLoadUtils.getLoadedDemData().getGroundOverlay().isVisible());
			return true;
		} else if(item.getItemId() == R.id.menu_catchments) {
			if (catchmentsOverlay.isVisible()) {
				catchmentsOverlay.setVisible(false);
				selectedCatchmentOutlet.hide();
				selectedCatchmentBottom.hide();
//				hideResultsFragment();
			} else {
				catchmentsOverlay.setVisible(true);
				selectedCatchmentOutlet.show();
				selectedCatchmentBottom.show();
//				showResultsFragment();
			}
			item.setChecked(catchmentsOverlay.isVisible());
			return true;
		} else if(item.getItemId() == R.id.menu_delineation) {
			if (delineationOverlay.isVisible()) {
				delineationOverlay.setVisible(false);
				delineationMarker.hide();
//				hideResultsFragment();
			} else {
				delineationOverlay.setVisible(true);
				delineationMarker.show();
//				showResultsFragment();
			}
			item.setChecked(delineationOverlay.isVisible());
			return true;
		} else if(item.getItemId() == R.id.menu_puddles) {
			if (puddleOverlay.isVisible()) {
				puddleOverlay.setVisible(false);
			} else {
				puddleOverlay.setVisible(true);
			}
			item.setChecked(puddleOverlay.isVisible());
			return true;
		} else if (item.getItemId() == R.id.menu_help) {
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		} else if (item.getItemId() == R.id.menu_export) {
			exportOutputs();
			return true;
		}else {
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void exportOutputs() {
	    DialogFragment newFragment = new ExportDialogFragment();
	    Bundle bundle = new Bundle();
	    bundle.putString("demDirectory", getDemLoadUtils().getDemDirectory());
	    bundle.putString("filePath", getDemLoadUtils().getLoadedDemData().getFilePath());
	    ((ExportDialogFragment)newFragment).catchmentRaster = getWatershedDataset().getPitRaster().pitIdMatrix;
	    ((ExportDialogFragment)newFragment).delineationRaster = getWatershedDataset().getDelineationRaster(delineationXyPoint);
	    newFragment.show(getFragmentManager(), "export");
	}

	// Finished loading the DEM
	@Override
	public void ProgressFragmentDone(WatershedDataset watershedDataset) {
		this.watershedDataset = watershedDataset;
		watershedDataset.getPitRaster().drawCatchments(this);
		getActionBar().show();
		getActionBar().setTitle("Watershed Delineation");
		wsdProgressBar.setVisibility(View.INVISIBLE);
		MenuItem mi = myMenu.findItem(R.id.menu_catchments).setEnabled(true);
		mi = myMenu.findItem(R.id.menu_puddles).setEnabled(true);
		mi = myMenu.findItem(R.id.menu_new_sim).setVisible(true);
		mi = myMenu.findItem(R.id.menu_export).setVisible(true);
		mi = myMenu.findItem(R.id.menu_delineation).setEnabled(true);
		
		//Show the catchment map
		GroundOverlayOptions catchmentsOverlayOptions = new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.getPitRaster().catchmentsBitmap))
		.positionFromBounds(demLoadUtils.getLoadedDemData().getBounds())
		.transparency(1 - (float) prefs.getInt("pref_key_catchments_trans_level", 50) / 100.0f)
		.visible(true)
		.zIndex(1);
		catchmentsOverlay = map.addGroundOverlay(catchmentsOverlayOptions);
		
		//Create Markers for catchment outlet and depression bottom points
		LatLng where = map.getCameraPosition().target;
		ATKPoint newPoint = new ATKPoint("Catchment Bottom", where);
		selectedCatchmentBottom = map.addPoint(newPoint);
		selectedCatchmentBottom.setIcon(BitmapDescriptorFactory.defaultMarker(200.0f), 100, 200);
		selectedCatchmentBottom.hide();
		newPoint = new ATKPoint("Catchment Outlet", where);
		selectedCatchmentOutlet = map.addPoint(newPoint);
		selectedCatchmentOutlet.setIcon(BitmapDescriptorFactory.defaultMarker(120.0f), 100, 200);
		selectedCatchmentOutlet.hide();
		
		// Show the filled areas
		GroundOverlayOptions puddleOverlayOptions = new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.drawPuddles()))
		.positionFromBounds(demLoadUtils.getLoadedDemData().getBounds())
		.transparency(1 - (float) prefs.getInt("pref_key_puddle_trans_level", 50) / 100.0f)
		.visible(true)
		.zIndex(2);
		puddleOverlay = map.addGroundOverlay(puddleOverlayOptions);
		
		//Add Delineation Point to the center of the DEM and delineate this watershed to begin with
		where = map.getCameraPosition().target;
		newPoint = new ATKPoint("Delineator", where);
		delineationMarker = map.addPoint(newPoint);
		delineationMarker.setSuperDraggable(true);
		delineationMarker.setIcon(BitmapDescriptorFactory.defaultMarker(), 100, 200);
		delineationXyPoint = demLoadUtils.getLoadedDemData().getXYFromLatLng(delineationMarker.getAtkPoint().position);
//		delineationPoint = new Point(302, 315);
//		Log.w("delinpoint", delineationPoint.toString());
		
		// Display results corresponding to selected DEM or delineated watershed area
		selectedCatchmentIndex = watershedDataset.getPitRaster().getIndexOf(watershedDataset.getPitRaster().pitIdMatrix[delineationXyPoint.y][delineationXyPoint.x]);
		showResultsFragment();
		delineating = true;
		new DelineateWatershedTask(delineationXyPoint, demLoadUtils, this).execute();
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
			wsdProgressBar.setProgress((Integer) values[0]);
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
		Bundle bundle = new Bundle();
		bundle.putDouble("delineationArea", watershedDataset.getDelineatedArea());
		fragment.setArguments(bundle);
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
	
//	public float getAlpha() {
//		return demAlpha;
//	}
	
	public void setDemAlpha() {
//		demAlpha = a;
		if (demLoadUtils.getLoadedDemData() != null) {
			if (demLoadUtils.getLoadedDemData().getGroundOverlay() != null) {
				demLoadUtils.getLoadedDemData().getGroundOverlay().setTransparency(1 - (float) prefs.getInt("pref_key_dem_trans_level", 50) / 100.0f);
			}
		}
		
	}

	public void setCatchmentsAlpha() {
//		catchmentsAlpha = a;
		if (catchmentsOverlay != null) {
			catchmentsOverlay.setTransparency(1 - (float) prefs.getInt("pref_key_catchments_trans_level", 50) / 100.0f);
		}
	}

	public void setPuddleAlpha() {
//		puddleAlpha = a;
		if (puddleOverlay != null) {
			puddleOverlay.setTransparency(1 - (float) prefs.getInt("pref_key_puddle_trans_level", 50) / 100.0f);
		}
	}
	
	//Group: watershedUI
	public void setDelineationAlpha() {
//		delineationAlpha = a;
		if (delineationOverlay != null) {
			delineationOverlay.setTransparency(1 - (float) prefs.getInt("pref_key_delin_trans_level", 50) / 100.0f);
		}
	}
	public void restartSimulation() {
		if (resultsFragment != null) {
			hideResultsFragment();
		}
		if (delineationMarker != null) {
			delineationMarker.remove();
		}
		simulateButton.setVisibility(View.INVISIBLE);
		myMenu.findItem(R.id.menu_new_sim).setVisible(false);
		watershedDataset = null;
		if (catchmentsOverlay != null) {
			catchmentsOverlay.remove();
			catchmentsOverlay = null;
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
		setDemLoadUtils(null);
//		new LoadWatershedDatasetTask(demLoadUtils.getLoadedDemData()).execute();
		LoadWatershedDatasetTask lwdt = new LoadWatershedDatasetTask(this);
		lwdt.listener = this;
		lwdt.execute();
	}
	
	public void clearOverlays(){
		if (resultsFragment != null) {
			hideResultsFragment();
		}
		if (delineationMarker != null) {
			delineationMarker.remove();
		}
		simulateButton.setVisibility(View.INVISIBLE);
		myMenu.findItem(R.id.menu_new_sim).setVisible(false);
		watershedDataset = null;
		if (catchmentsOverlay != null) {
			catchmentsOverlay.remove();
			catchmentsOverlay = null;
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
	}
	
	public GroundOverlay getDelineationOverlay() {
		return delineationOverlay;
	}
	
	public void setDelineationOverlay(Bitmap delineationBitmap) {
		GroundOverlayOptions delineationOverlayOptions = new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(delineationBitmap))
		.positionFromBounds(demLoadUtils.getLoadedDemData().getBounds())
		.transparency(1 - (float) prefs.getInt("pref_key_delin_trans_level", 50) / 100.0f)
		.visible(true)
		.zIndex(3);
		delineationOverlay = map.addGroundOverlay(delineationOverlayOptions);
        double conv = 0.000247105;
        if ("feet".equals("feet")) {
            conv = 1.0/43560;
        }
		resultsFragment.updateResultsDelineation(watershedDataset.getDelineatedArea()*conv*Math.pow(watershedDataset.getCellSize(), 2));
	}
	
	public void updateDelineationOverlay(BitmapDescriptor bDF) {
		if (delineationOverlay != null) {
			delineationOverlay.setImage(bDF);
            double conv = 0.000247105;
            if ("feet".equals("feet")) {
                conv = 1.0/43560;
            }
			resultsFragment.updateResultsDelineation(watershedDataset.getDelineatedArea()*conv*Math.pow(watershedDataset.getCellSize(), 2));
		}
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		// Handle data from file manager
		if (requestCode == FIRST_START) {
			demLoadUtils.loadInitialDem(this);
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
			//TODO Disallow choosing the current demfile!!!
			if (data == null) {
			}
			else if (data.getData().toString().contains(".tif")) {
				File f = new File(data.getData().getPath());
				if (demLoadUtils.getDemDirectory() != f.getParent()) {
					demLoadUtils.setNewDemDirectory(f.getParent());
				}
				demLoadUtils.loadDem(this, data.getData().getPath());
			}
		}
	}
	
	public void onMapClick (LatLng latLng) {
		if (catchmentsOverlay != null) {
			if (catchmentsOverlay.isVisible()) {
				clickedPoint = demLoadUtils.getLoadedDemData().getXYFromLatLng(latLng);
				if (clickedPoint != null) {
					selectedCatchmentIndex = watershedDataset.getPitRaster().getIndexOf(watershedDataset.getPitRaster().pitIdMatrix[clickedPoint.y][clickedPoint.x]);
					catchmentsOverlay.setImage(BitmapDescriptorFactory.fromBitmap(watershedDataset.getPitRaster().selectCatchment(selectedCatchmentIndex)));
					Pit selectedCatchment = watershedDataset.getPitRaster().pitDataList.get(selectedCatchmentIndex);
					resultsFragment.updateResultsCatchment(selectedCatchment.pitId, watershedDataset.getPitRaster().pitDataList.get(MainActivity.selectedCatchmentIndex).area*0.000247105*Math.pow(watershedDataset.getCellSize(), 2));

					if (selectedCatchment.pitId > 0) {
						LatLng where = demLoadUtils.getLoadedDemData().getLatLngFromXY(selectedCatchment.pitOutletPoint.x, selectedCatchment.pitOutletPoint.y);
						selectedCatchmentOutlet.getAtkPoint().position = where;
						selectedCatchmentOutlet.update();
						selectedCatchmentOutlet.show();
						where = demLoadUtils.getLoadedDemData().getLatLngFromXY(selectedCatchment.pitPoint.x, selectedCatchment.pitPoint.y);
						selectedCatchmentBottom.getAtkPoint().position = where;
						selectedCatchmentBottom.update();
						selectedCatchmentBottom.show();
					} else {
						selectedCatchmentBottom.hide();
						LatLng where = demLoadUtils.getLoadedDemData().getLatLngFromXY(selectedCatchment.pitOutletPoint.x, selectedCatchment.pitOutletPoint.y);
						selectedCatchmentOutlet.getAtkPoint().position = where;
						selectedCatchmentOutlet.update();
						selectedCatchmentOutlet.show();
					} 
				} else {
					deselectCatchment();
				}
			}
		}
	}

	public void deselectCatchment() {
		selectedCatchmentOutlet.hide();
		selectedCatchmentBottom.hide();
		catchmentsOverlay.setImage(BitmapDescriptorFactory.fromBitmap(watershedDataset.getPitRaster().catchmentsBitmap));
		double conv = 0.000247105;
		if ("feet".equals("feet")) {
			conv = 1.0/43560;
		}
		resultsFragment.updateResultsDelineation(watershedDataset.getDelineatedArea()*conv*Math.pow(watershedDataset.getCellSize(), 2));
	}

	@Override
	public boolean onPointDrag(ATKPointView pointView) {
		return false;
	}

	@Override
	public boolean onPointDragEnd(ATKPointView pointView) {
		if (!delineating) {
			delineating = true;
			selectedCatchmentBottom.hide();
			selectedCatchmentOutlet.hide();
			delineationXyPoint = demLoadUtils.getLoadedDemData().getXYFromLatLng(delineationMarker.getAtkPoint().position);
			if (delineationXyPoint == null) {
				Toast toast = Toast.makeText(context, "Location to delineate must be within bounded area.", Toast.LENGTH_SHORT);
				toast.show();
			} else {
				if(resultsFragment == null || resultsFragment.isVisible() == false){
					showResultsFragment();
				}
				new DelineateWatershedTask(delineationXyPoint, demLoadUtils, this).execute();			
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
		watershedDataset = null;
		getDemData().getOutlinePolygon().setOnClickListener(this);
//		new LoadWatershedDatasetTask(demLoadUtils.getLoadedDemData()).execute();
		LoadWatershedDatasetTask lwdt = new LoadWatershedDatasetTask(this);
		lwdt.listener = this;
		lwdt.execute();
	}

	@Override
	public boolean onPolygonClick(ATKPolygonView polygonView) {
		if (((String)polygonView.getAtkPolygon().id).equals(demLoadUtils.getLoadedDemData().getFilePath())) {
			return false;
		} else {
			clearOverlays();
			//in this case, is the given context parameter "this" equal to this main activity class or is it overwridden?
			demLoadUtils.loadClickedDem(this, (String) polygonView.getAtkPolygon().id);
		}
		return false;
	}

	@Override
	public DemLoadUtils getDemLoadUtils() {
		return demLoadUtils;
	}

	@Override
	public void setDemLoadUtils(DemLoadUtils demLoadUtils) {
		this.demLoadUtils = demLoadUtils;
	}

	@Override
	public DemData getDemData() {
		return this.demLoadUtils.getLoadedDemData();
	}
	
	public WatershedDataset getWatershedDataset() {
		return this.watershedDataset;
	}

	public TextView getWsdProgressText() {
		return wsdProgressText;
	}
	
	public ProgressBar getWsdProgressBar() {
		return wsdProgressBar;
	}

	@Override
	public void onWatershedDatasetLoaded(WatershedDataset result) {
		watershedDataset = result;
		simulateButton.setVisibility(View.VISIBLE);
		simulateButton.setEnabled(true);
	}

	public Point getDelineationPoint() {
		return delineationXyPoint;
	}

	//Perform UI work onSharedPreferenceChanged
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("pref_key_dem_trans_level")) {
            setDemAlpha();
        } else if (key.equals("pref_key_delin_trans_level")) {
            this.setDelineationAlpha();
        } else if (key.equals("pref_key_puddle_trans_level")) {
            this.setPuddleAlpha();
        } else if (key.equals("pref_key_catchments_trans_level")) {
            setCatchmentsAlpha();
        } else if (key.equals("pref_rainfall_amount")) {
        	DelineationAppConfigs.setDepth(Float.parseFloat(sharedPreferences.getString("pref_rainfall_amount", "1.0")));
            getWatershedDataset().recalculatePitsForNewRainfall();
        }
		
	}
}

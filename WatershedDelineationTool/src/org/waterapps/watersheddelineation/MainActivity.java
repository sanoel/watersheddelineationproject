package org.waterapps.watersheddelineation;

import static android.graphics.Color.HSVToColor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import org.waterapps.watersheddelineation.R;
import org.waterapps.watersheddelineation.ProgressFragment.ProgressFragmentListener;

import com.precisionag.lib.*;

import org.waterapps.watersheddelineation.HelpActivity;

import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
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

public class MainActivity extends FragmentActivity implements OnMapClickListener, ProgressFragmentListener, OnMarkerDragListener {
	private Uri fileUri;
	static GroundOverlay pitsOverlay;
	GroundOverlay delinOverlay;
	LocationManager locationManager;
	private static Menu myMenu = null;
	static String demDirectory = "/dem";
	static float alpha;
	public static int hsvColors[];
	public static int hsvTransparentColors[];
	//marker variables
	int markerMode;
	public static SharedPreferences prefs;
	RainfallSimConfig delineation_settings;
	//    private TextView progressMessage;
	public static WatershedDataset watershedDataset;
	Bitmap filledPitBitmap;
	int demStatus = 0;
	double[][] DEM;
	public static ProgressBar wsdProgressBar;
	int wsdStatus = 0;
	String wsdStatusMsg = "Reading DEM";
	public static TextView wsdProgressText;
	public static Button simulateButton;
	public static Button delineateButton;
	ProgressFragment progressFragment = null;
	public static Field field;
	public static boolean coloring;
	public static boolean currentlyDrawing;
	public static GoogleMap map;
	LatLng sw; //= new LatLng(40.974, -86.1991);
	LatLng ne; // = new LatLng(40.983, -86.1869);
	LatLngBounds fieldBounds; // = new LatLngBounds(sw, ne);
	double xLLCorner;
	double yLLCorner;
	int numrows;
	int numcols;
	double cellSize;
	public static boolean dem_visibility = true;
	public static Context context;
	static ArrayList<DemFile> dems;
	DemFile currentlyLoaded;
	Point delineationPoint;
	static Marker delineationMarker;
	private boolean firstStart;
	private static final int FIRST_START = 42;
	private static final int INITIAL_LOAD = 6502;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		map = mapFragment.getMap();
		map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		map.setMyLocationEnabled(true);
		map.setOnMarkerDragListener(this);
		map.setOnMapClickListener(this);
		Field.setMapFragment(mapFragment);

		UiSettings uiSettings = map.getUiSettings();
		uiSettings.setRotateGesturesEnabled(false);
		uiSettings.setTiltGesturesEnabled(false);
		uiSettings.setZoomControlsEnabled(false);
        context = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
        demDirectory = prefs.getString("dem_dir", Environment.getExternalStorageDirectory().toString() + "/dem");
		markerMode = 0;
		coloring = true;
		currentlyDrawing = false;
		alpha = 0.5f;
		hsvColors = new int[256];
		hsvTransparentColors = new int[256];
		float hsvComponents[] = {1.0f, 0.75f, 0.75f};
		for(int i = 0; i<255; i++) {
			hsvComponents[0] = 360.0f*i/255.0f;
			hsvColors[i] = HSVToColor(hsvComponents);
			hsvTransparentColors[i] = HSVToColor(128, hsvComponents);
		}
				
        scanDEMs();

        //show help on first app start
        firstStart = prefs.getBoolean("first_start", true);
        if (firstStart) {
            //show help
            Intent intent = new Intent(this, HelpActivity.class);
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
				getActionBar().hide();
				showProgressFragment();
			}
		});
		
		delineateButton = (Button) findViewById(R.id.button_delineate);
		delineateButton.setVisibility(View.INVISIBLE);
		delineateButton.setEnabled(true);		
		delineateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				delineationPoint = field.getXYFromLatLng(delineationMarker.getPosition());
				if (delineationPoint == null) {
					Toast toast = Toast.makeText(context, "Location to delineate must be within bounded area.", Toast.LENGTH_LONG);
					toast.show();
				} else {
					pitsOverlay.remove();
					if (delinOverlay != null) {
						delinOverlay.remove();
					}
					delinOverlay = map.addGroundOverlay(new GroundOverlayOptions()
					.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.delineate(delineationPoint)))
					.positionFromBounds(field.getFieldBounds())
					.transparency(PitRaster.alpha));
					delinOverlay.setVisible(true);
					delineationMarker.setPosition(delineationMarker.getPosition());
					delineationMarker.setTitle("Area: " + Double.toString(WatershedDataset.delineatedArea*0.000247105*9.0)+ " acres");
				}
			}
		});
		
	}

	public void onMapClick (LatLng point) {
		 //load DEM if clicked on
        DemFile dem;
        for(int i = 0; i<dems.size(); i++) {
            dem = dems.get(i);
            if ( (currentlyLoaded == null) || !dem.getFilename().equals(currentlyLoaded.getFilename())) {
                if(dem.getBounds().contains(point)) {
                    currentlyLoaded = dem;
                    ElevationRaster raster = new ElevationRaster();
                    new ReadElevationRasterTask(this, raster, dem.getFilename()).execute(dem.getFileUri());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("last_dem", dem.getFileUri().getPath());
                    editor.commit();
                }
            }
        }
    }
	
	private void showProgressFragment(){
		FragmentManager fm = getSupportFragmentManager();
		ProgressFragment fragment = new ProgressFragment();
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(R.id.fragment_container, fragment, "progress_fragment");
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
		//		drawCatchmentPolygons();
	}

	private void drawCatchmentPolygons() {
		for (int i = 0; i < watershedDataset.pits.pitDataList.size(); i++) {
			PolygonOptions polygonOptions = new PolygonOptions();
			for (int j = 0; j < watershedDataset.pits.pitDataList.get(i).pitBorderIndicesList.size(); j++) {
				double x = xLLCorner + watershedDataset.pits.pitDataList.get(i).pitBorderIndicesList.get(j).x*cellSize;
				double y = yLLCorner + (numrows*cellSize) - watershedDataset.pits.pitDataList.get(i).pitBorderIndicesList.get(j).y*cellSize;
				LatLng latlng = UtmXyToLatLng(x, y, 16, "N");
				polygonOptions.add(latlng);
			}
			double x = xLLCorner + watershedDataset.pits.pitDataList.get(i).pitBorderIndicesList.get(0).x*cellSize;
			double y = yLLCorner + (numrows*cellSize) - watershedDataset.pits.pitDataList.get(i).pitBorderIndicesList.get(0).y*cellSize;
			LatLng latlng = UtmXyToLatLng(x, y, 16, "N");
			polygonOptions.add(latlng);
			polygonOptions.strokeColor(Color.RED);
			polygonOptions.fillColor(Color.BLUE);
			map.addPolygon(polygonOptions);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		myMenu = menu;
		// Make an action bar and don't display the app title
		ActionBar actionBar = getActionBar();
		actionBar.setTitle("Watershed Delineation Tool");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Choose DEM Menu Item
		if (item.getItemId() == R.id.menu_choose_dem) {
			Intent i = new Intent("com.filebrowser.DataFileChooser");
            i.putExtra("path", demDirectory);
			startActivityForResult(i, 1);
			return true;
		}  else if (item.getItemId() == R.id.menu_settings) {
			Bundle data = new Bundle();
			data.putString("returnIntent", "back"); //After choose return back to this
			Intent i = new Intent("org.waterapps.watersheddelineation.SettingsActivity");
			i.putExtras(data);
			startActivity(i);
			return true;
		} else if (item.getItemId() == R.id.menu_center) {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(field.getFieldBounds(), 50));
            return true;
        } else if(item.getItemId() == R.id.menu_dem) {
			if (dem_visibility) {
				dem_visibility = false;
				Field.prevoverlay.setTransparency(1);
			}
			else {
				dem_visibility = true;
				Field.prevoverlay.setTransparency(alpha);
			}
			item.setChecked(dem_visibility);
			return true;
		} else if(item.getItemId() == R.id.menu_catchments) {
			if (PitRaster.pits_visibility) {
				PitRaster.pits_visibility = false;
				pitsOverlay.setTransparency(1);
			}
			else {
				PitRaster.pits_visibility = true;
				pitsOverlay.setTransparency(PitRaster.alpha);
			}
			item.setChecked(PitRaster.pits_visibility);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		} 
	}

	@Override
	public void ProgressFragmentDone(WatershedDataset watershedDataset) {
		MainActivity.watershedDataset = watershedDataset;
		hideProgressFragment();
		getActionBar().show();
		//Show the pits on the field
		pitsOverlay = map.addGroundOverlay(new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(watershedDataset.pits.pitsBitmap))
		.positionFromBounds(field.getFieldBounds())
		.transparency(PitRaster.alpha));
		pitsOverlay.setVisible(true);
		simulateButton.setVisibility(View.GONE);
//		simulateButton.setEnabled(true);
		MenuItem mi = myMenu.findItem(R.id.menu_catchments);
		mi.setEnabled(true);
		LatLng midfield = new LatLng((MainActivity.field.getFieldBounds().southwest.latitude + MainActivity.field.getFieldBounds().northeast.latitude)/2.0, (MainActivity.field.getFieldBounds().southwest.longitude + MainActivity.field.getFieldBounds().northeast.longitude)/2.0);
		delineationMarker = MainActivity.map.addMarker(new MarkerOptions()
		.position(midfield)
		.title("Delineate Here"));
		delineationMarker.setDraggable(true);
		delineateButton.setVisibility(View.VISIBLE);
		
	}

	@Override
	public WatershedDataset ProgressFragmentGetData() {
		return watershedDataset;
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		//handle data from file manager
		
		if (requestCode == FIRST_START) {
	        loadInitialDEM();
	        return;
	    }
		
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
				Log.w("here","6");
				new ReadElevationRasterTask(this, raster).execute(juri);
			}
			else {
				Toast toast = Toast.makeText(this, "File selected was not a GEOTiff file.", Toast.LENGTH_LONG);
				toast.show();
			}
		}
		
		if (requestCode == INITIAL_LOAD) {
	        ElevationRaster raster = new ElevationRaster();
	        DemFile demToLoad = dems.get(0);
	        String filename = demToLoad.getFilename();
	        Log.w("here","7");
	        new ReadElevationRasterTask(this, raster, filename).execute(demToLoad.getFileUri());
	    }
	}

	public static void onFileRead(ElevationRaster raster) {
		Log.w("onfileread", "twice?");
		Bitmap bitmap = raster.getBitmap();
		field = new Field(bitmap, raster.getLowerLeft(), raster.getUpperRight(), 0.0, 0.0);
		field.setBitmap(raster.getBitmap());
		field.setBounds(raster.getBounds());

		map.animateCamera(CameraUpdateFactory.newLatLngBounds(raster.getBounds(), 50));
		field.updatePolyLine();
		updateColors(field);
		new LatLng((MainActivity.field.getFieldBounds().southwest.latitude + MainActivity.field.getFieldBounds().northeast.latitude)/2.0, (MainActivity.field.getFieldBounds().southwest.longitude + MainActivity.field.getFieldBounds().northeast.longitude)/2.0);
		new LoadWatershedDatasetTask(raster).execute();
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
	        GroundOverlay ppo = Field.prevoverlay;
	        Field.prevoverlay = field.createOverlay(map);
	        if (dem_visibility) {
	            Field.prevoverlay.setTransparency(alpha);
	        }
	        ppo.remove();
	        currentlyDrawing = false;
	    }
	}

	public double[] LatLngToUtmXy(LatLng latlng, int zone) {
		//From http://home.hiwaay.net/~taylorc/toolbox/geography/geoutm.html
		//Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
		//GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.	

		//set ellipsoid parameters to WGS84
		double sm_a = 6378137; //ellipsoidal major axis
		double sm_b = 6356752.314; //ellipsoidal minor axis
		double utmScaleFactor = 0.9996;
		double lambda0 = zone*6 - 183; //origin longitude
		double[] xy = new double[2];
		/* Precalculate ep2 */
		double ep2 = (Math.pow (sm_a, 2.0) - Math.pow (sm_b, 2.0)) / Math.pow (sm_b, 2.0);

		/* Precalculate nu2 */
		double nu2 = ep2 * Math.pow (Math.cos (latlng.latitude), 2.0);
		/* Precalculate N */
		double N = Math.pow (sm_a, 2.0) / (sm_b * Math.sqrt (1 + nu2));
		/* Precalculate t */
		double t = Math.tan (latlng.latitude);
		double t2 = t * t;
		//		double tmp = (t2 * t2 * t2) - Math.pow (t, 6.0);
		/* Precalculate l */
		double l = latlng.longitude - lambda0;

		/* Precalculate coefficients for l**n in the equations below
	           so a normal human being can read the expressions for easting
	           and northing
	           -- l**1 and l**2 have coefficients of 1.0 */
		double l3coef = 1.0 - t2 + nu2;
		double l4coef = 5.0 - t2 + 9 * nu2 + 4.0 * (nu2 * nu2);
		double l5coef = 5.0 - 18.0 * t2 + (t2 * t2) + 14.0 * nu2
				- 58.0 * t2 * nu2;
		double l6coef = 61.0 - 58.0 * t2 + (t2 * t2) + 270.0 * nu2
				- 330.0 * t2 * nu2;
		double l7coef = 61.0 - 479.0 * t2 + 179.0 * (t2 * t2) - (t2 * t2 * t2);
		double l8coef = 1385.0 - 3111.0 * t2 + 543.0 * (t2 * t2) - (t2 * t2 * t2);

		/* Calculate easting (x) */
		xy[0] = N * Math.cos (latlng.latitude) * l
				+ (N / 6.0 * Math.pow (Math.cos (latlng.latitude), 3.0) * l3coef * Math.pow (l, 3.0))
				+ (N / 120.0 * Math.pow (Math.cos (latlng.latitude), 5.0) * l5coef * Math.pow (l, 5.0))
				+ (N / 5040.0 * Math.pow (Math.cos (latlng.latitude), 7.0) * l7coef * Math.pow (l, 7.0));

		/* Calculate northing (y) */
		xy[1] = ArcLengthOfMeridian (latlng.latitude)
				+ (t / 2.0 * N * Math.pow (Math.cos (latlng.latitude), 2.0) * Math.pow (l, 2.0))
				+ (t / 24.0 * N * Math.pow (Math.cos (latlng.latitude), 4.0) * l4coef * Math.pow (l, 4.0))
				+ (t / 720.0 * N * Math.pow (Math.cos (latlng.latitude), 6.0) * l6coef * Math.pow (l, 6.0))
				+ (t / 40320.0 * N * Math.pow (Math.cos (latlng.latitude), 8.0) * l8coef * Math.pow (l, 8.0));

		xy[0] = xy[0] * utmScaleFactor + 500000.0;
		xy[1] = xy[1] *utmScaleFactor;
		if (xy[1] < 0.0) {
			xy[1] = xy[1] + 10000000.0;
		}
		return xy;
	}

	public double ArcLengthOfMeridian (double lat) {
		double sm_a = 6378137; //ellipsoidal major axis
		double sm_b = 6356752.314; //ellipsoidal minor axis
		//    Computes the ellipsoidal distance from the equator to a point at a given latitude
		/* Precalculate n */
		double n = (sm_a - sm_b) / (sm_a + sm_b);

		/* Precalculate alpha */
		double alpha = ((sm_a + sm_b) / 2.0)
				* (1.0 + (Math.pow (n, 2.0) / 4.0) + (Math.pow (n, 4.0) / 64.0));

		/* Precalculate beta */
		double beta = (-3.0 * n / 2.0) + (9.0 * Math.pow (n, 3.0) / 16.0)
				+ (-3.0 * Math.pow (n, 5.0) / 32.0);

		/* Precalculate gamma */
		double gamma = (15.0 * Math.pow (n, 2.0) / 16.0)
				+ (-15.0 * Math.pow (n, 4.0) / 32.0);

		/* Precalculate delta */
		double delta = (-35.0 * Math.pow (n, 3.0) / 48.0)
				+ (105.0 * Math.pow (n, 5.0) / 256.0);

		/* Precalculate epsilon */
		double epsilon = (315.0 * Math.pow (n, 4.0) / 512.0);

		/* Now calculate the sum of the series and return */
		double result = alpha* (lat + (beta * Math.sin (2.0 * lat))
				+ (gamma * Math.sin (4.0 * lat))
				+ (delta * Math.sin (6.0 * lat))
				+ (epsilon * Math.sin (8.0 * lat)));

		return result;
	}



	public LatLng UtmXyToLatLng(double inputX, double inputY, int zone, String NS){
		//From http://home.hiwaay.net/~taylorc/toolbox/geography/geoutm.html
		//Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
		//GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
		//Similar information may also be found in Map Projections--A Working Manual by John Snyder

		//		Convert XY coordinates from Transverse Mercator to Universal Transverse Mercator using a scale factor (and false northing for Southern Hemisphere)
		//set ellipsoid parameters to WGS84
		double sm_a = 6378137.0; //ellipsoidal major axis
		double sm_b = 6356752.314; //ellipsoidal minor axis
		double utmScaleFactor = 0.9996;
		double x = (inputX - 500000.0)/utmScaleFactor;
		double y = inputY;
		if (NS == "S") {
			y = inputY - 10000000.0;
		}
		y = y/utmScaleFactor;

		//set parameters to UTM zone 12 north
		//		double phi0 = 0; //origin latitude: equator
		double lambda0deg = zone*6 - 183.0; //origin longitude in degrees
		double lambda0 = lambda0deg * Math.PI / 180.0;

		double phif = calcFootpointLatitude(y);
		double ep2 = (Math.pow(sm_a,2) - Math.pow(sm_b,2.0)) / Math.pow(sm_b,2.0);
		double cf = Math.cos(phif);
		double nuf2 = ep2*Math.pow(cf, 2.0);
		double Nf = Math.pow(sm_a, 2.0) / (sm_b * Math.sqrt(1.0+nuf2));
		double Nfpow = Nf;
		double tf = Math.tan(phif);
		double tf2 = tf*tf;
		double tf4 = tf2*tf2;

		/* Precalculate fractional coefficients for x**n in the equations
        below to simplify the expressions for latitude and longitude. */
		double x1frac = 1.0 / (Nfpow * cf);
		Nfpow = Nfpow * Nf;   //now equals Nf**2)
		double x2frac = tf / (2.0 * Nfpow);
		Nfpow = Nfpow * Nf;   //now equals Nf**3)
		double x3frac = 1.0 / (6.0 * Nfpow * cf);
		Nfpow = Nfpow * Nf;   // now equals Nf**4)
		double x4frac = tf / (24.0 * Nfpow);
		Nfpow = Nfpow *Nf;   // now equals Nf**5)
		double x5frac = 1.0 / (120.0 * Nfpow * cf);
		Nfpow = Nfpow * Nf;   // now equals Nf**6)
		double x6frac = tf / (720.0 * Nfpow);
		Nfpow = Nfpow * Nf;   // now equals Nf**7)
		double x7frac = 1.0 / (5040.0 * Nfpow * cf);
		Nfpow = Nfpow *Nf;   // now equals Nf**8)
		double x8frac = tf / (40320.0 * Nfpow);

		// Precalculate polynomial coefficients for x**n.
		//-- x**1 does not have a polynomial coefficient.
		double x2poly = -1.0 - nuf2;
		double x3poly = -1.0 - 2 * tf2 - nuf2;
		double x4poly = 5.0 + 3.0 * tf2 + 6.0 * nuf2 - 6.0 * tf2 * nuf2 - 3.0 * (nuf2 *nuf2) - 9.0 * tf2 * (nuf2 * nuf2);
		double x5poly = 5.0 + 28.0 * tf2 + 24.0 * tf4 + 6.0 * nuf2 + 8.0 * tf2 * nuf2;
		double x6poly = -61.0 - 90.0 * tf2 - 45.0 * tf4 - 107.0 * nuf2 + 162.0 * tf2 * nuf2;
		double x7poly = -61.0 - 662.0 * tf2 - 1320.0 * tf4 - 720.0 * (tf4 * tf2);
		double x8poly = 1385.0 + 3633.0 * tf2 + 4095.0 * tf4 + 1575.0 * (tf4 * tf2);

		// Calculate latitude (phi) in radians
		double lat = phif + x2frac * x2poly * (x * x)
				+ x4frac * x4poly * Math.pow (x, 4.0)
				+ x6frac * x6poly * Math.pow (x, 6.0)
				+ x8frac * x8poly * Math.pow (x, 8.0);

		// Calculate longitude (lambda) in radians
		double lon = lambda0 + (x1frac * x)
				+ (x3frac * x3poly * Math.pow (x, 3.0))
				+ (x5frac * x5poly * Math.pow (x, 5.0))
				+ (x7frac * x7poly * Math.pow (x, 7.0));
		lat = lat * 180.0 / Math.PI;
		lon = lon * 180.0 / Math.PI;

		return new LatLng(lat, lon);
	}

	private double calcFootpointLatitude(double y) {
		double sm_a = 6378137.0; //ellipsoidal major axis
		double sm_b = 6356752.314; //ellipsoidal minor axis
		// Precalculate n (Eq. 10.18)
		double n = (sm_a - sm_b) / (sm_a + sm_b);

		// Precalculate alpha_ (Eq. 10.22 or Eq. 10.17)
		double alpha_ = ((sm_a + sm_b) / 2.0)
				* (1 + (Math.pow (n, 2.0) / 4) + (Math.pow (n, 4.0) / 64));
		// Precalculate y_ (Eq. 10.23)
		double y_ = y / alpha_;
		// Precalculate beta_ (Eq. 10.22)
		double beta_ = (3.0 * n / 2.0) + (-27.0 * Math.pow (n, 3.0) / 32.0)
				+ (269.0 * Math.pow (n, 5.0) / 512.0);
		// Precalculate gamma_ (Eq. 10.22)
		double gamma_ = (21.0 * Math.pow (n, 2.0) / 16.0)
				+ (-55.0 * Math.pow (n, 4.0) / 32.0);

		// Precalculate delta_ (Eq. 10.22)
		double delta_ = (151.0 * Math.pow (n, 3.0) / 96.0)
				+ (-417.0 * Math.pow (n, 5.0) / 128.0);

		// Precalculate epsilon_ (Eq. 10.22)
		double epsilon_ = (1097.0 * Math.pow (n, 4.0) / 512.0);

		// Now calculate the sum of the series (Eq. 10.21)
		double result = y_ + (beta_ * Math.sin (2.0 * y_))
				+ (gamma_ * Math.sin (4.0 * y_))
				+ (delta_ * Math.sin (6.0 * y_))
				+ (epsilon_ * Math.sin (8.0 * y_));

		return result;
	}

	public static float getAlpha() {
		return alpha;
	}
	
	public static void setDemAlpha(float a) {
		alpha = a;
		if (dem_visibility) {
			Field.prevoverlay.setTransparency(alpha);
		} else {
			Field.prevoverlay.setTransparency(1);	
		}
	}
	
	public static void setCatchmentsAlpha(float a) {
		PitRaster.alpha = a;
		if (pitsOverlay != null) {
			if (PitRaster.pits_visibility) {
				pitsOverlay.setTransparency(PitRaster.alpha);
			} else {
				pitsOverlay.setTransparency(1);	
			}
		}
	}

	public static void scanDEMs() {
        //scan DEM directory
        String path = demDirectory;
        DemFile dem;
        dems = new ArrayList<DemFile>();
        File f = new File(path);
        if (f.isDirectory()) {
            File file[] = f.listFiles();

            for (int i=0; i < file.length; i++)
            {
                dem = ReadGeoTiffMetadata.readMetadata(file[i]);
                dems.add(dem);
                map.addPolyline(new PolylineOptions().add(new LatLng(dem.getSw_lat(), dem.getSw_long()))
                        .add(new LatLng(dem.getSw_lat(), dem.getNe_long()))
                        .add(new LatLng(dem.getNe_lat(), dem.getNe_long()))
                        .add(new LatLng(dem.getNe_lat(), dem.getSw_long()))
                        .add(new LatLng(dem.getSw_lat(), dem.getSw_long()))
                        .color(Color.RED));
            }
        }
    }
	public void setCurrentlyLoaded(String filename) {
        DemFile dem;
        for(int i = 0; i<dems.size(); i++) {
            dem = dems.get(i);
            Log.d("filename", filename);
            Log.d("dem filename", dem.getFilename());
            if (filename.equals(dem.getFilename())) {
                currentlyLoaded = dem;
            }
        }
    }
	
	public void loadInitialDEM() {
        //attempt to load last used DEM, if it still exists
        File demFile = new File(prefs.getString("last_dem", "foo"));
        if(demFile.isFile()) {
            ElevationRaster raster = new ElevationRaster();
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
            ElevationRaster raster = new ElevationRaster();
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
                ElevationRaster raster = new ElevationRaster();
                new ReadElevationRasterTask(this, raster).execute(UritoURI(Uri.fromFile(new File(demDirectory+"Feldun.tif"))));
                setCurrentlyLoaded(demDirectory+"Feldun.tif");
            }
            //if one TIFF, open it
            else if(count == 1) {
                ElevationRaster raster = new ElevationRaster();
                new ReadElevationRasterTask(this, raster, tiffs.get(0).getName()).execute(UritoURI(Uri.fromFile(tiffs.get(0))));
                setCurrentlyLoaded(tiffs.get(0).getPath());
            }
            //if multiple TIFFs, let user choose
            else {
                Intent intent = new Intent("com.filebrowser.DataFileChooser");
                intent.putExtra("path", demDirectory);
                startActivityForResult(intent, INITIAL_LOAD);
            }
        }
    }

    private void copyAssets() {
        AssetManager assetManager = getAssets();
        try {
            assetManager.list("");
        } catch (IOException e) {
//            Log.e("tag", "Failed to get asset file list.", e);
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
//                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
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

	@Override
	public void onMarkerDrag(Marker marker) {
	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
	}

	@Override
	public void onMarkerDragStart(Marker marker) {
	}
}

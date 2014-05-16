package org.waterapps.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.openatk.openatklib.atkmap.ATKMap;
import com.openatk.openatklib.atkmap.views.ATKPolygonView;

public class DemLoadUtils {
	List<DemFile> demFiles;
	DemData loadedDemData;
	String demDirectory;
	private static final int INITIAL_LOAD = 6502;
	Context context;
	SharedPreferences prefs;
	static ATKMap map;
	WmacDemLoadUtilsListener wmacListener;

	public DemLoadUtils(Context context, ATKMap map, SharedPreferences prefs) {
		this.context = context;
		DemLoadUtils.map = map;
		this.prefs = prefs;
		this.demDirectory = prefs.getString("dem_dir", Environment.getExternalStorageDirectory().toString() + "/dem");
		scanDems();
	}

	public void loadDem(String filePath) {
		// Update tappable area and boundary line color of current loaded and previously loaded DemData objects
		if (this.loadedDemData != null) {
			this.loadedDemData.getDemFile().getOutlinePolygon().setFillColor(Color.argb(64, 255, 0, 0));
			this.loadedDemData.getDemFile().getOutlinePolygon().setStrokeColor(Color.RED);
			this.loadedDemData.getDemFile().getTapPoint().show();
		}
		DemFile demFile = getDemFileFromFilePath(filePath);
		new ReadDemDataTask(context, this, wmacListener, demFile).execute(Uri.parse(Uri.encode(filePath)));
	}

	public void loadFileChooserData(Intent data) {
		if (data != null) {
			if (data.getData().toString().contains(".tif")) {
				loadDem(data.getData().getPath());
				return;
			}
		}
		// Try to load the first demFile if no file was successfully chosen.
		if (data == null) {
			DemFile demToLoad = demFiles.get(0);
			String filename = demToLoad.getFilePath();
			loadDem(filename);
		}
	}

	public void loadClickedDem(LatLng point) {
		DemFile demFile;
		for(int i = 0; i< demFiles.size(); i++) {
			demFile = demFiles.get(i);
			// Don't load currently loaded Dem
			if (!demFile.getFilePath().equals(loadedDemData.getFilePath())) {
				if(demFile.getBounds().contains(point)) {
					loadDem(demFile.getFilePath());
					//Only DEMs in the current directory are clickable so no need to set dem_dir pref
				}
			}
		}
	}

	/**
	 *Converts Android Uri to Java URI
	 * @param fileUri Uri to be converted
	 * @return Converted URI
	 */
	//    private URI UriToURI(Uri fileUri) {
	//        URI juri = null;
	//        try {
	//        juri = new java.net.URI(fileUri.getScheme(),
	//                fileUri.getSchemeSpecificPart(),
	//                fileUri.getFragment());
	//        } catch (URISyntaxException e) {
	//            e.printStackTrace();
	//        }
	//        return juri;
	//    }

	/**
	 *Looks through contents of DEM directory and displays outlines of all DEMs there
	 */
	public void scanDems() {
		//scan DEM directory
		File f = new File(demDirectory);
		demFiles = new ArrayList<DemFile>(f.listFiles().length);

		if (f.isDirectory()) {
			File file[] = f.listFiles();

			for (int i=0; i < file.length; i++) {
				if (file[i].isFile()) {
					DemFile demFile = new DemFile(file[i], GdalUtils.readDemFileBounds(file[i]), map);
					demFiles.add(demFile);
				}
			}
		}
	}



	/**
	 * Clears all DemFile data, removes outlines from the map, etc.
	 */
	public void clearDemFiles() {
		for (int i = 0; i < demFiles.size(); i++) {
			demFiles.get(i).getOutlinePolygon().remove();
		}
		demFiles = null;
	}

	/**
	 * Clear the loaded DemFile
	 */
	public void clearLoadedDemFile() {
		loadedDemData.getGroundOverlay().remove();
		loadedDemData = null;
	}

	/**
	 * Picks which DEM to load upon app start
	 */
	public void loadInitialDem() {
		//1) Attempt to load last used DEM, if it still exists
		File file = new File(prefs.getString("last_dem", "foo"));
		if(file.exists() && file.isFile()) {
			loadDem(file.getPath());
			return;
		}

		File f = new File(demDirectory);
		//2) If DEM dir doesn't exist, create it, copy sample TIFF in, then open it, and save dem and dem_dir to prefs
		if (!f.exists()) {
			f.mkdir();
			copyAssets(demDirectory, context);
			loadDem(demDirectory+"/Feldun.tif");


			setDemDirectoryPreference(demDirectory);
			return;

			//3) Else if the dem directory exists, but it is a file.
		} else if(f.isFile()) {
			for(int i=0; i<10; i++) {
				demDirectory = demDirectory + Integer.toString(i);
				f = new File(demDirectory);
				if (!f.exists()) {
					f.mkdir();
					copyAssets(demDirectory, context);                 
					loadDem(demDirectory+"/Feldun.tif"); 
					setDemDirectoryPreference(demDirectory);
					return;
				}
			}
			return;

			//4) Otherwise, the dem directory exists.  Count number of tiffs in dir and handle.
		} else { 
			File[] files = f.listFiles();
			int count = 0;
			for(int i = 0; i<f.listFiles().length; i++) {
				if(files[i].getName().contains(".tif")) {
					count++;
				}
			}

			//4a) if no TIFFs, copy sample into dir and open
			if (count == 0) {
				copyAssets(demDirectory, context);
				loadDem(demDirectory+"/Feldun.tif");

				//4b) if one TIFF, open it    
			} else if(count == 1) {
				loadDem(files[0].getName());

				//4c) if multiple TIFFs, let user choose    
			} else {
				Intent intent = new Intent("com.filebrowser.DataFileChooser");
				intent.putExtra("path", demDirectory);
				((Activity) context).startActivityForResult(intent, INITIAL_LOAD);
			}
		}
	}

	/**
	 * Copies a file from assets to SD
	 * @param path Path to asset
	 */
	private void copyAssets(String path, Context context) {
		AssetManager assetManager = context.getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			Log.e("tag", "Failed to get asset file list.", e);
		}
		String filename = "Feldun.tif";
		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open(filename);
			File outFile = new File(path, filename);
			out = new FileOutputStream(outFile);
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch(IOException e) {
			Log.e("tag", "Failed to copy asset file: " + filename, e);
		}
	}


	/**
	 * copies a file
	 * @param in input file stream
	 * @param out output file stream
	 * @throws IOException
	 */
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}

	private boolean inside(ATKPolygonView poly, LatLng l) {
		Iterator<LatLng> iter = poly.getAtkPolygon().boundary.iterator();
		LatLngBounds bounds = new LatLngBounds(iter.next(), iter.next());
		while(iter.hasNext()) {
			bounds = bounds.including(iter.next());
		}
		return bounds.contains(l);
	}

	private void removeDemOutlines() {
		for (int i = 0; i < demFiles.size(); i++) {
			demFiles.get(i).getOutlinePolygon().remove();
		}
	}

	public String getDemDirectory() {
		return demDirectory;
	}

	public DemData getLoadedDemData() {
		return loadedDemData;
	}

	public void setNewDemDirectory(String demDirectory) {
		this.demDirectory = demDirectory;
		setDemDirectoryPreference(demDirectory);
		clearDemFiles();
		scanDems();
	}

	void setDemFilePreference(String filePath) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("last_dem", filePath);
		editor.commit();
	}

	public void setDemDirectoryPreference(String demDirectory) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("dem_dir", demDirectory);
		editor.commit();
	}

	private DemFile getDemFileFromFilePath(String filePath) {
		DemFile demFile = null;
		for(int i = 0; i< demFiles.size(); i++) {
			demFile = demFiles.get(i);
			if (demFile.getFilePath().equals(filePath)) {
				return demFile;
			}
		}
		return demFile;
	}

	public void registerWmacDemLoadUtilsListener(WmacDemLoadUtilsListener listener) {
		this.wmacListener = listener;
	}

	public ATKMap getMap() {
		return this.map;
	}

	public void setLoadedDemData(DemData demData) {
		this.loadedDemData = demData;
	}
}

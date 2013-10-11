package com.precisionag.lib;

import java.net.URI;

import org.waterapps.watershed.MainActivity;


import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.content.Context;

import static android.util.Log.*;

public class ReadElevationRasterTask extends AsyncTask <URI, Integer, ElevationRaster> {
	Context context;
	ElevationRaster data;
	ProgressDialog dialog;
    String filename;
	
	public ReadElevationRasterTask(Context con, ElevationRaster raster) {
		context = con;
		data = raster;
        filename = "the default elevation file";
	}

    public ReadElevationRasterTask(Context con, ElevationRaster raster, String filename) {
        context = con;
        data = raster;
        this.filename = filename;
    }

	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(context);
		//dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setMax(100);
		dialog.setMessage("Loading elevations into map from " + filename);
		dialog.show();
	}
	
	protected ElevationRaster doInBackground(URI... params) {
		publishProgress(0);
		
		ReadElevationRaster readObject = null;
		
		//select proper reader for filetype
		if (params[0].getPath().contains(".hdr")) readObject = new ReadGridFloat();
//        Log.i("geotiff", "About to read geotiff");
        if (params[0].getPath().contains(".tif")) readObject = new ReadGeoTiff();
		data = readObject.readFromFile(params[0]);
		publishProgress(100);
		return data;
	}
	
	protected void onProgressUpdate(Integer... progress) {
		dialog.setProgress(progress[0]);
	}
	
	protected void onPostExecute(ElevationRaster rasters) {
		dialog.dismiss();
		Toast.makeText(context, "File load complete", Toast.LENGTH_LONG).show();
		MainActivity.onFileRead(rasters);
	}

}

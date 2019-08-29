package org.waterapps.lib;

import com.google.android.gms.maps.CameraUpdateFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Makes ReadDemData interface into an AsyncTask to run off of UI thread
 */
public class ReadDemDataTask extends AsyncTask<Uri, Integer, DemData> {
    Context context;
    DemData demData;
    ProgressDialog dialog;
    WmacListener listener;
    DemFile demFile;
    DemLoadUtils demLoadUtils;

    /**
     * Constructor which includes filename to display in progress bar
     * @param demData 
     * @param context
     * @param demLoadUtils 
     * @param listener 
     * @param filePath
     */
    public ReadDemDataTask(Context context, DemLoadUtils demLoadUtils, WmacListener listener, DemFile demFile) {
    	this.demFile = demFile;
    	this.demData = demFile.readDemData();
        this.context = context;
        this.listener = listener;
        this.demLoadUtils = demLoadUtils;
    }

    /**
     * Creates progress bar before file is read
     */
    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setMax(100);
        dialog.setMessage("Loading elevations into map from " + demFile.getFilePath().split("/")[demFile.getFilePath().split("/").length-1]);
//        dialog.show();
    }

    /**
     * The actual file reading part
     * @param params Uri of file to be read
     * @return DEM data
     */
	@Override
	protected DemData doInBackground(Uri... params) {
        publishProgress(0);
        //TODO This reader handles only geotiff files (.tif) at the moment.  We could probably handle others.
//        this.demData  = new DemData(demFile);
        this.demData = GdalUtils.readDemData(demData);
		demLoadUtils.setLoadedDemData(demData);
		demLoadUtils.getLoadedDemData().makeElevationBitmap(context);
        publishProgress(100);
        return demData;
	}

    protected void onProgressUpdate(Integer... progress) {
        dialog.setProgress(progress[0]);
    }

    /**
     * Once the file read is complete, pass data to handler in MainActivity
     * @param demData
     */
    protected void onPostExecute(DemData demData) {
		demData.getDemFile().getOutlinePolygon().setStrokeColor(Color.BLACK);
		demData.getDemFile().getOutlinePolygon().setFillColor(Color.TRANSPARENT);
		demData.getDemFile().getOutlinePolygon().setLabel("");
//		demData.getDemFile().getTapPoint().hide();
		demLoadUtils.getLoadedDemData().createOverlay(demLoadUtils.getMap());
		demLoadUtils.getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(demLoadUtils.getLoadedDemData().getDemFile().getBounds(), 50));
		demLoadUtils.setDemFilePreference(demData.getFilePath());
        dialog.dismiss();
        listener.onDemDataLoad();
    }
}

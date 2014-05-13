package org.waterapps.lib;

import java.io.File;
import java.net.URI;

import org.waterapps.watershed.MainActivity;

import com.google.android.gms.maps.CameraUpdateFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Makes ReadDemData interface into an AsyncTask to run off of UI thread
 */
public class ReadDemDataTask extends AsyncTask<URI, Integer, DemData> {
    Context context;
    DemData demData;
    ProgressDialog dialog;
    String filePath;

    /**
     * Constructor which includes filename to display in progress bar
     * @param demData 
     * @param context
     * @param filePath
     */
    public ReadDemDataTask(DemData demData, Context context, String filePath) {
        this.demData = demData;
        this.context = context;
        this.filePath = filePath;
    }

    /**
     * Creates progress bar before file is read
     */
    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setMax(100);
        dialog.setMessage("Loading elevations into map from " + filePath.split("/")[filePath.split("/").length-1]);
        dialog.show();
    }

    /**
     * The actual file reading part
     * @param params URI of file to be read
     * @return DEM data
     */
    protected DemData doInBackground(URI... params) {
        publishProgress(0);
        //TODO This reader handles only geotiff files (.tif) at the moment.  We could probably handle others.
        GdalUtils.readDemData(demData, filePath);
        
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
		demData.getDemFile().getTapPoint().hide();
        dialog.dismiss();
        DemLoadUtils.onFileRead(demData);
    }

}

package com.precisionag.lib;

import org.waterapps.watersheddelineation.MainActivity;
import org.waterapps.watersheddelineation.WatershedDataset;
import org.waterapps.watersheddelineation.WatershedDataset.WatershedDatasetListener;

import com.tiffdecoder.TiffDecoder;


import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

public class LoadWatershedDatasetTask extends AsyncTask <String, String, WatershedDataset> implements WatershedDatasetListener {
	float[][] DEM;
	
	public LoadWatershedDatasetTask(ElevationRaster raster) {
		DEM = raster.getData();
	}

	protected void onPreExecute() {
		super.onPreExecute();
		MainActivity.wsdProgressBar.setVisibility(View.VISIBLE);
		MainActivity.wsdProgressText.setVisibility(View.VISIBLE);
	}

	protected WatershedDataset doInBackground(String... params) {		 
		// Background Work
		float cellSizeX = TiffDecoder.nativeTiffGetScaleX();
		float cellSizeY = TiffDecoder.nativeTiffGetScaleY();
		float noDataVal = Float.valueOf(TiffDecoder.nativeTiffGetNoData());
		WatershedDataset watershedDataset = new WatershedDataset(DEM, cellSizeX, cellSizeY, noDataVal, this);
		return watershedDataset;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		MainActivity.wsdProgressBar.setProgress(Integer.parseInt(values[0]));
		MainActivity.wsdProgressText.setText(values[1]);
	}

	protected void onPostExecute(WatershedDataset result) {
		super.onPostExecute(result);
		MainActivity.wsdProgressBar.setVisibility(View.GONE);
		MainActivity.wsdProgressText.setVisibility(View.GONE);
		MainActivity.watershedDataset = result;
		MainActivity.simulateButton.setEnabled(true);
		Toast toast = Toast.makeText(MainActivity.context, Integer.toString(result.noDataCellsRemoved) + " total NoData cells removed from the DEM.", Toast.LENGTH_LONG);
		toast.show();
	}

	public void watershedDatasetOnProgress(int progress, String status) {
		String[] array = new String[2];
		array[0] = Integer.toString(progress);
		array[1] = status;
		publishProgress(array);
	}

	public void watershedDatasetDone() {

	}
	

}

package com.precisionag.lib;

import org.waterapps.watershed.MainActivity;
import org.waterapps.watershed.WatershedDataset;
import org.waterapps.watershed.WatershedDataset.WatershedDatasetListener;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.tiffdecoder.TiffDecoder;


import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

public class LoadWatershedDatasetTask extends AsyncTask <String, Object, WatershedDataset> implements WatershedDatasetListener {
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
	protected void onProgressUpdate(Object... values) {
		MainActivity.wsdProgressBar.setProgress((Integer) values[0]);
		MainActivity.wsdProgressText.setText((String) values[1]);
	}

	protected void onPostExecute(WatershedDataset result) {
		super.onPostExecute(result);
		MainActivity.wsdProgressBar.setVisibility(View.GONE);
		MainActivity.wsdProgressText.setVisibility(View.GONE);
		MainActivity.watershedDataset = result;
		MainActivity.simulateButton.setEnabled(true);
		
//		MainActivity.pitsOverlay = MainActivity.map.addGroundOverlay(new GroundOverlayOptions()
//		.image(BitmapDescriptorFactory.fromBitmap(MainActivity.watershedDataset.pits.pitsBitmap))
//		.positionFromBounds(MainActivity.field.getFieldBounds())
//		.transparency(MainActivity.catchmentsAlpha));
	}

	public void watershedDatasetOnProgress(int progress, String status, Bitmap bitmap) {
		//android.os.Debug.waitForDebugger();
		Object[] array = new Object[2];
		array[0] = progress;
		array[1] = status;
		publishProgress(array);
	}

	public void watershedDatasetDone() {

	}
	

}

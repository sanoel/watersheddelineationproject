package org.waterapps.watershed;

import org.waterapps.lib.DemData;
import org.waterapps.watershed.MainActivity;
import org.waterapps.watershed.WatershedDataset;
import org.waterapps.watershed.WatershedDataset.WatershedDatasetListener;

import android.os.AsyncTask;
import android.view.View;

public class LoadWatershedDatasetTask extends AsyncTask <String, Object, WatershedDataset> implements WatershedDatasetListener {
	float[][] DEM;
	float cellSize;
	float noDataVal;
	
	public LoadWatershedDatasetTask(DemData rasters) {
		DEM = rasters.getData();
		this.cellSize = rasters.getCellSize();
		this.noDataVal = rasters.getNoDataVal();
	}

	protected void onPreExecute() {
		super.onPreExecute();
		MainActivity.wsdProgressBar.setVisibility(View.VISIBLE);
		MainActivity.wsdProgressText.setVisibility(View.VISIBLE);
	}

	protected WatershedDataset doInBackground(String... params) {
		WatershedDataset watershedDataset = new WatershedDataset(DEM, cellSize, noDataVal, this);
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

	public void simulationOnProgress(int progress, String status) {
		//android.os.Debug.waitForDebugger();
		Object[] array = new Object[2];
		array[0] = progress;
		array[1] = status;
		publishProgress(array);
	}

	public void simulationDone() {

	}
	

}

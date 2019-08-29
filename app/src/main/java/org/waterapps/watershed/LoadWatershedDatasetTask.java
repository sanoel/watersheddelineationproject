package org.waterapps.watershed;

import org.waterapps.lib.DemData;
import org.waterapps.watershed.MainActivity;
import org.waterapps.watershed.WatershedDataset;
import org.waterapps.watershed.WatershedDataset.WatershedDatasetListener;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadWatershedDatasetTask extends AsyncTask <String, Object, WatershedDataset> implements WatershedDatasetListener {
	float[][] DEM;
	float cellSize;
	float noDataVal;
	MainActivity mainActivity;
	ProgressBar wsdProgressBar;
	TextView wsdProgressText;
	WatershedDatasetDoneListener listener = null;
	
//	public LoadWatershedDatasetTask(DemData rasters) {
	public LoadWatershedDatasetTask(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
		DEM = mainActivity.getDemLoadUtils().getLoadedDemData().getElevationData();
		this.cellSize = mainActivity.getDemLoadUtils().getLoadedDemData().getCellSize();
		this.noDataVal = mainActivity.getDemLoadUtils().getLoadedDemData().getNoDataVal();
		this. wsdProgressBar = mainActivity.getWsdProgressBar();
		this. wsdProgressText = mainActivity.getWsdProgressText();
	}

	protected void onPreExecute() {
		super.onPreExecute();
		wsdProgressBar.setVisibility(View.VISIBLE);
		wsdProgressText.setVisibility(View.VISIBLE);
	}

	protected WatershedDataset doInBackground(String... params) {
		WatershedDataset watershedDataset = new WatershedDataset(DEM, cellSize, noDataVal, mainActivity, this);
		return watershedDataset;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		wsdProgressBar.setProgress((Integer) values[0]);
		wsdProgressText.setText((String) values[1]);
	}

	protected void onPostExecute(WatershedDataset result) {
		super.onPostExecute(result);
		wsdProgressBar.setVisibility(View.GONE);
		wsdProgressText.setVisibility(View.GONE);
		listener.onWatershedDatasetLoaded(result);
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

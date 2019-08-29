package org.waterapps.watershed;

import org.waterapps.lib.DemLoadUtils;
import org.waterapps.watershed.MainActivity;
import org.waterapps.watershed.WatershedDataset.DelineationListener;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;

public class DelineateWatershedTask extends AsyncTask <String, Bitmap, Bitmap> implements DelineationListener {

	private DemLoadUtils demLoadUtils;
	private MainActivity mainActivity;

	public DelineateWatershedTask(Point delineationPoint, DemLoadUtils demLoadUtils, MainActivity mainActivity) {
		this.demLoadUtils = demLoadUtils;
		this.mainActivity = mainActivity;
	}

	protected void onPreExecute() {
		super.onPreExecute();
	}

	protected Bitmap doInBackground(String... params) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPurgeable = true;
		options.inInputShareable = true;
		Bitmap delineationBitmap = mainActivity.getWatershedDataset().delineate(BitmapFactory.decodeResource(mainActivity.context.getResources(), R.drawable.watershedelineation, options), mainActivity.getDelineationPoint(), this);
		return delineationBitmap;
	}

	@Override
	protected void onProgressUpdate(Bitmap... values) {
	}

	protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		if (mainActivity.getDelineationOverlay() != null) {
			mainActivity.updateDelineationOverlay(BitmapDescriptorFactory.fromBitmap(result));
		} else {	
			mainActivity.setDelineationOverlay(result);
		}
		MainActivity.delineating = false;
	}

	@Override
	public void delineationDone() {
	}

	@Override
	public void delineationOnProgress(Bitmap bitmap) {
//		publishProgress(bitmap);		
	}


}

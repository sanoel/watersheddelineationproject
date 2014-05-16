package org.waterapps.lib;

import org.waterapps.watershed.MainActivity;
import org.waterapps.watershed.WatershedDataset.DelineationListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;

public class DelineateWatershedTask extends AsyncTask <String, Bitmap, Bitmap> implements DelineationListener {

	private DemLoadUtils demLoadUtils;

	public DelineateWatershedTask(Point delineationPoint, DemLoadUtils demLoadUtils) {
		this.demLoadUtils = demLoadUtils;
	}

	protected void onPreExecute() {
		super.onPreExecute();
	}

	protected Bitmap doInBackground(String... params) {
		Bitmap delineationBitmap = MainActivity.watershedDataset.delineate(MainActivity.delineationPointRC, this);
		return delineationBitmap;
	}

	@Override
	protected void onProgressUpdate(Bitmap... values) {
	}

	protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		if (MainActivity.delineationOverlay != null) {
			MainActivity.delineationOverlay.setImage(BitmapDescriptorFactory.fromBitmap(result));
		} else {
			GroundOverlayOptions delineationOverlayOptions = new GroundOverlayOptions()
			.image(BitmapDescriptorFactory.fromBitmap(result))
			.positionFromBounds(demLoadUtils.getLoadedDemData().getBounds())
			.transparency(MainActivity.delineationAlpha)
			.visible(MainActivity.delineationVisible)
			.zIndex(3);
			MainActivity.delineationOverlay = MainActivity.map.addGroundOverlay(delineationOverlayOptions);
		}
	}

	@Override
	public void delineationDone() {
		MainActivity.delineating = false;
	}

	@Override
	public void delineationOnProgress(Bitmap bitmap) {
//		publishProgress(bitmap);		
	}


}

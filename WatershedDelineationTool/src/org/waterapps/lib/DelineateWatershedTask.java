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
			MainActivity.delineationOverlayOptions = new GroundOverlayOptions()
			.image(BitmapDescriptorFactory.fromBitmap(result))
			.positionFromBounds(demLoadUtils.getLoadedDemData().getBounds())
			.transparency(MainActivity.delineationAlpha)
			.visible(MainActivity.delineationVisible)
			.zIndex(3);
			MainActivity.delineationOverlay = MainActivity.map.addGroundOverlay(MainActivity.delineationOverlayOptions);
		}
		
//		MainActivity.map.clear();
//		System.gc();
		//Redraw delineation marker
//		MainActivity.delineationMarker = MainActivity.map.addMarker(MainActivity.delineationMarkerOptions);
//		MainActivity.delineationMarker.setDraggable(true);
//		
//		//Redraw the delineation overlay
//		MainActivity.delineationOverlayOptions = new GroundOverlayOptions()
//		.image(BitmapDescriptorFactory.fromBitmap(result))
//		.positionFromBounds(MainActivity.field.getFieldBounds())
//		.transparency(MainActivity.delineationAlpha)
//		.visible(MainActivity.delineation_visible)
//		.zIndex(3);
//		MainActivity.delineationOverlay = MainActivity.map.addGroundOverlay(MainActivity.delineationOverlayOptions);
//		
//		//Redraw elevation DEM
//		MainActivity.demOverlay = MainActivity.map.addGroundOverlay(MainActivity.demOverlayOptions);
//		
//		//Redraw catchments
//		MainActivity.pitsOverlay = MainActivity.map.addGroundOverlay(MainActivity.pitsOverlayOptions);
//		
//		//Redraw puddles map
//		MainActivity.puddleOverlay = MainActivity.map.addGroundOverlay(MainActivity.puddleOverlayOptions);
//		
//		if(MainActivity.resultsFragment != null){
//			MainActivity.resultsFragment.updateResults(1);
//		}
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

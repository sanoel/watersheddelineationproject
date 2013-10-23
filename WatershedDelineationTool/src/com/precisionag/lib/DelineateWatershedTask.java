package com.precisionag.lib;

import org.waterapps.watershed.MainActivity;
import org.waterapps.watershed.WatershedDataset;
import org.waterapps.watershed.WatershedDataset.DelineationListener;
import org.waterapps.watershed.WatershedDataset.WatershedDatasetListener;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tiffdecoder.TiffDecoder;


import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

public class DelineateWatershedTask extends AsyncTask <String, Bitmap, Bitmap> implements DelineationListener {
	int BUFFER_ONE = 0;
	int BUFFER_TWO = 1;
	int buffer = 0;
	

	public DelineateWatershedTask(Point delineationPoint) {
	}

	protected void onPreExecute() {
		super.onPreExecute();
	}

	protected Bitmap doInBackground(String... params) {
		System.gc();
		Bitmap delineationBitmap = MainActivity.watershedDataset.delineate(MainActivity.delineationPoint, this);
		return delineationBitmap;
	}

	@Override
	protected void onProgressUpdate(Bitmap... values) {
//		if (MainActivity.delineation_visible) {
//			if(MainActivity.resultsFragment != null){
//				MainActivity.resultsFragment.updateResults(1);
//			}
//			if (buffer == BUFFER_ONE) {
//				if (MainActivity.delineationOverlay != null) {
//					MainActivity.delineationOverlay.remove();
//				}
//				MainActivity.delineationOverlay = MainActivity.map.addGroundOverlay(new GroundOverlayOptions()
//				.image(BitmapDescriptorFactory.fromBitmap(values[0]))
//				.positionFromBounds(MainActivity.field.getFieldBounds())
//				.transparency(MainActivity.delineationAlpha)
//				.zIndex(1));
//				MainActivity.delineationOverlay.setVisible(MainActivity.delineation_visible);
//
//				if (MainActivity.delineationOverlayBuffer != null) {
//					MainActivity.delineationOverlayBuffer.remove();
//				}
//				buffer = BUFFER_TWO;
//			} else if (buffer == BUFFER_TWO) {
//				if (MainActivity.delineationOverlayBuffer != null) {
//					MainActivity.delineationOverlayBuffer.remove();
//				}
//				MainActivity.delineationOverlayBuffer = MainActivity.map.addGroundOverlay(new GroundOverlayOptions()
//				.image(BitmapDescriptorFactory.fromBitmap(values[0]))
//				.positionFromBounds(MainActivity.field.getFieldBounds())
//				.transparency(MainActivity.delineationAlpha)
//				.zIndex(0));
//				MainActivity.delineationOverlayBuffer.setVisible(MainActivity.delineation_visible);
//
//				if (MainActivity.delineationOverlay != null) {
//					MainActivity.delineationOverlay.remove();
//				}
//				buffer = BUFFER_ONE;
//			}
//		}
	}

	protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		MainActivity.map.clear();
		System.gc();
		//Redraw delineation marker
		MainActivity.delineationMarker = MainActivity.map.addMarker(MainActivity.delineationMarkerOptions);
		MainActivity.delineationMarker.setDraggable(true);
		
		//Redraw the delineation overlay
		MainActivity.delineationOverlayOptions = new GroundOverlayOptions()
		.image(BitmapDescriptorFactory.fromBitmap(result))
		.positionFromBounds(MainActivity.field.getFieldBounds())
		.transparency(MainActivity.delineationAlpha)
		.visible(MainActivity.delineation_visible)
		.zIndex(3);
		MainActivity.delineationOverlay = MainActivity.map.addGroundOverlay(MainActivity.delineationOverlayOptions);
		
		//Redraw elevation DEM
		MainActivity.demOverlay = MainActivity.map.addGroundOverlay(MainActivity.demOverlayOptions);
		
		//Redraw catchments
		MainActivity.pitsOverlay = MainActivity.map.addGroundOverlay(MainActivity.pitsOverlayOptions);
		
		//Redraw puddles map
		MainActivity.puddleOverlay = MainActivity.map.addGroundOverlay(MainActivity.puddleOverlayOptions);
		
		if(MainActivity.resultsFragment != null){
			MainActivity.resultsFragment.updateResults(1);
		}
	}

	@Override
	public void delineationDone() {		
	}

	@Override
	public void delineationOnProgress(Bitmap bitmap) {
		publishProgress(bitmap);		
	}


}

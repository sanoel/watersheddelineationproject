package com.precisionag.watersheddelineationtool;

import java.text.DecimalFormat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;

public class CustomMarker {
	private static GoogleMap map;
	private static RasterLayer raster;
	private static Activity activity;
	private LatLng location;
	private Bitmap image;
	private GroundOverlay overlay;
	
	public CustomMarker(LatLng point) {
		location = point;
		double pixelValue = raster.pixelValueFromLatLng(point);
		String title;

		if (pixelValue == 0.0) {
			title = "Not in field!";
		}
		else {
			String elevation = new DecimalFormat("#.#").format(pixelValue);
			title = "Elevation: " + elevation + "m";
		}
		Bitmap markerBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.tile_riser);			
		markerBitmap = markerBitmap.copy(markerBitmap.getConfig(), true);
		Canvas c = new Canvas(markerBitmap);
		Paint paint = new Paint();
		paint.setTextSize(75);
		c.drawText(title, (float)5, (float)80, paint);
		BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(markerBitmap);
		GoogleMap map;
		map = ((MapFragment) activity.getFragmentManager().findFragmentById(R.id.map)).getMap();
		
		GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
	     .image(image)
	     .anchor((float)1.0, (float)1.0)
	     .position(point, (float)300.0)
	     .transparency(0)
	     .zIndex((float)1));
		groundOverlay.setVisible(true);
		overlay = groundOverlay;
	}
	
	public void updateMarker(double density) {
		//delete old overlay and recreate with updated text
		overlay.remove();
		
		double pixelDouble = raster.pixelValueFromLatLng(location);
		String title;
		
		if (pixelDouble == 0.0) {
			title = "Not in field!";
		}
		else {
			String elevation = new DecimalFormat("#.#").format(pixelDouble);
			title = "Elevation: " + elevation + "m";
		}
		Bitmap markerBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.tile_riser);			
		markerBitmap = markerBitmap.copy(markerBitmap.getConfig(), true);
		Canvas c = new Canvas(markerBitmap);
		Paint paint = new Paint();
		
		paint.setTextSize((int)(25.0*density));
		c.drawText(title, (float)5, (float)80, paint);
		BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(markerBitmap);
		GoogleMap map;
		map = ((MapFragment) activity.getFragmentManager().findFragmentById(R.id.map)).getMap();
		
		GroundOverlay groundOverlay = map.addGroundOverlay(new GroundOverlayOptions()
	     .image(image)
	     .anchor((float)1.0, (float)1.0)
	     .position(location, (float)300.0)
	     .transparency(0)
	     .zIndex((float)1));
		groundOverlay.setVisible(true);
		overlay = groundOverlay;
	}
	
	public void removeMarker() {
		overlay.remove();
	}
	
	public static void setMap(GoogleMap newMap) {
		map = newMap;
	}
	
	public static void setRaster(RasterLayer newRaster) {
		raster = newRaster;
	}
	
	public static int[] getPixelCoords(LatLng point) {
		int[] xy = raster.pixelCoordsFromLatLng(point);
		return xy;
	}
	
	public static void setActivity(Activity mActivity) {
		activity = mActivity;
	}
	
	public boolean inBounds(LatLng point) {
		return overlay.getBounds().contains(point);
	}
}

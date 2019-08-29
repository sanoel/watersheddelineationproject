package org.waterapps.lib;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.openatk.openatklib.atkmap.ATKMap;
import com.openatk.openatklib.atkmap.models.ATKPoint;
import com.openatk.openatklib.atkmap.models.ATKPolygon;
import com.openatk.openatklib.atkmap.views.ATKPointView;
import com.openatk.openatklib.atkmap.views.ATKPolygonView;

public class DemFile {
    private LatLngBounds bounds;
    private String filePath;
    private String timestamp;
    private ATKPolygonView outlinePolygon;

    public DemFile(LatLngBounds bounds, String filePath, String timestamp,
			ATKPolygonView outlinePolygon) {
		super();
		this.bounds = bounds;
		this.filePath = filePath;
		this.timestamp = timestamp;
		this.outlinePolygon = outlinePolygon;
	}

    public DemFile(File file, LatLngBounds bounds, ATKMap map) {
    	DateFormat df = DateFormat.getDateInstance();
        this.bounds = bounds;
        this.filePath = file.getPath();
        this.timestamp = df.format(file.lastModified());
        
        // Draw the boundary on the map
		List<LatLng> list = new ArrayList<LatLng>();
		list.add(bounds.northeast);
		list.add(new LatLng(bounds.southwest.latitude, bounds.northeast.longitude));
        list.add(bounds.southwest);
        list.add(new LatLng(bounds.northeast.latitude, bounds.southwest.longitude));
        ATKPolygon poly = new ATKPolygon(this.getFilePath(), list);
        poly.viewOptions.setFillColor(Color.argb(64, 255, 0, 0));
        poly.viewOptions.setStrokeColor(Color.RED);
        
        setOutlinePolygon(map.addPolygon(poly));
        outlinePolygon.setLabel("Tap here to load");
        
        //Add the tappable marker to the map
//        ATKPoint point = new ATKPoint(this.filePath, new GeoRectangle(this.getBounds()).center());
//        setTapPoint(map.addPoint(point));
//        getTapPoint().setIcon(textToBitmap("Tap here to load"));
    }

    public DemFile(DemFile demFile) {
    	this(demFile.getBounds(), demFile.getFilePath(), demFile.getTimestamp(), demFile.getOutlinePolygon());
    }
    
    static int dpToPx(float density, int dp) {
        return (int) (dp * density + 0.5f);
    }
    
    public void setPolygon(ATKPolygonView polygon) {
    	if (this.getOutlinePolygon() != null) {
    		this.getOutlinePolygon().remove();
    	}
    	this.setOutlinePolygon(polygon);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilename(String filename) {
        this.filePath = filename;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public LatLngBounds getBounds() {
        return bounds;
    }
    
    public void setBounds(LatLngBounds bounds) {
        this.bounds = bounds;
    }

//	public ATKPointView getTapPoint() {
//		return tapPoint;
//	}

//	public void setTapPoint(ATKPointView tapPoint) {
//		this.tapPoint = tapPoint;
//	}

	public ATKPolygonView getOutlinePolygon() {
		return outlinePolygon;
	}

	public void setOutlinePolygon(ATKPolygonView outlinePolygon) {
		this.outlinePolygon = outlinePolygon;
	}

	public DemData readDemData() {
		return new DemData(this);	
	}

}
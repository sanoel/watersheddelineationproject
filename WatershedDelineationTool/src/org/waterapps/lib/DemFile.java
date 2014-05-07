package org.waterapps.lib;

import java.net.URI;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class DemFile {
    int id;
    LatLng sw;
    LatLng ne;
//    float sw_lat;
//    float sw_long;
//    float ne_lat;
//    float ne_long;
    String filename;
    String timestamp;
    URI fileUri;

    public URI getFileUri() {
        return fileUri;
    }

    public void setFileUri(URI fileUri) {
        this.fileUri = fileUri;
    }

    public DemFile(int id, LatLng sw, LatLng ne, String filename, String timestamp) {
        this.id = id;
        this.sw = sw;
        this.ne = ne;
        this.filename = filename;
        this.timestamp = timestamp;
    }

    public DemFile(LatLng sw, LatLng ne, String filename, String timestamp, URI fileUri) {
    	this.sw = sw;
        this.ne = ne;
        this.filename = filename;
        this.timestamp = timestamp;
        this.fileUri = fileUri;
    }

    public DemFile() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LatLng getNe() {
        return ne;
    }
    
    public LatLng getSw() {
        return sw;
    }
    

//    public void setNe_lat(float ne_lat) {
//        this.ne_lat = ne_lat;
//    }
//
//    public float getNe_long() {
//        return ne_long;
//    }
//
//    public void setNe_long(float ne_long) {
//        this.ne_long = ne_long;
//    }
    
    public void setNe(LatLng nE){ 
    	this.ne = nE;
    }
    
    public void setSw(LatLng sW){ 
    	this.sw = sW;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public LatLngBounds getBounds() {
        return new LatLngBounds(sw, ne);
    }

}
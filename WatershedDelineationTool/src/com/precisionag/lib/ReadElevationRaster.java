package com.precisionag.lib;

import java.net.URI;

public interface ReadElevationRaster {
	public ElevationRaster readFromFile(URI fileUri);
}

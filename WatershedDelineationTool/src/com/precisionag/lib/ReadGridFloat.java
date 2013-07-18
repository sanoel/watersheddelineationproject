package com.precisionag.lib;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import com.google.android.gms.maps.model.LatLng;

public class ReadGridFloat implements ReadElevationRaster {

	public ElevationRaster readFromFile(URI fileUri) {
		ElevationRaster raster = new ElevationRaster();
		//gridfloat splits the DEM into a header file and data file which consists of packed 32 bit floats
		try {
			//read header file (.hdr)
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileUri)));
			String line = null;
			String[] data;
			//lower left and upper right x and y
			float llx=0, lly=0, nodata=-9999, cellsize=0;
			
			while((line = reader.readLine()) != null) {
				data = line.split("[ \t]+");
				/*
				System.out.println(line);
				for(int i=0;i<data.length;i++) {
					System.out.println(i);
					System.out.println(data[i]);
				}
				*/
				if (data[0].equals("ncols")) raster.setNcols(Integer.parseInt(data[1]));
				if (data[0].equals("nrows")) raster.setNrows(Integer.parseInt(data[1]));
				if (data[0].equals("xllcorner")) llx = Float.parseFloat(data[1]);
				if (data[0].equals("yllcorner")) lly = Float.parseFloat(data[1]);
				if (data[0].equals("cellsize")) cellsize = Float.parseFloat(data[1]);
				if (data[0].equals("NODATA_value")) nodata = Float.parseFloat(data[1]);
			}
			
			raster.setLowerLeft(new LatLng(lly, llx));
			raster.setUpperRight(new LatLng(lly+(raster.getNrows()*cellsize), llx+(raster.getNcols()*cellsize)));
			System.out.println(raster.getUpperRight().latitude);
			
			//now read data file (.flt)
			String dataFilename = fileUri.toString().replace("hdr", "flt"); //fileUri.getPath().replace("hdr", "flt");
			System.out.println(dataFilename);
			URI dataUri = URI.create(dataFilename);
			System.out.println(dataUri.getPath());
			//reader = new BufferedReader(new FileReader(new File(dataUri)));
			//reader.read
			
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(dataUri)));
			byte[] bytes = new byte[raster.getNcols()*raster.getNrows()*4];
			bis.read(bytes, 0, raster.getNcols()*raster.getNrows()*4);
			
			int sampleWidth = 6;
			
			raster.setElevationData(new float[raster.getNcols()/sampleWidth][raster.getNrows()/sampleWidth]);
			raster.setNcols(raster.getNcols()/sampleWidth);
			raster.setNrows(raster.getNrows()/sampleWidth);
			
			for(int i = 0; i<raster.getNcols(); i++) {
				for(int j = 0; j<raster.getNrows(); j++) {
					raster.elevationData[j][i] = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 4*((i*sampleWidth)+(raster.getNcols()*(j*sampleWidth))), 4*((i*sampleWidth)+(raster.getNcols()*(j*sampleWidth)))+4)).order(ByteOrder.BIG_ENDIAN).getFloat();
					if (raster.getElevationData()[j][i] != nodata) {
						if (raster.getElevationData()[j][i] < raster.getMaxElevation()) raster.setMinElevation(raster.getElevationData()[i][j]);
						if (raster.getElevationData()[j][i] > raster.getMaxElevation()) raster.setMaxElevation(raster.getElevationData()[i][j]);
					}
				}
			}
			System.out.println(raster.getMinElevation());
			System.out.println(raster.getMaxElevation());
		}
		
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return raster;
	}


}

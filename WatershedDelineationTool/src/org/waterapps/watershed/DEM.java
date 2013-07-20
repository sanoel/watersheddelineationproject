package org.waterapps.watershed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.res.AssetManager;

public class DEM {
	double[][] DEM;
	double cellSize;
	public DEM(String DEMName) {
//		AssetManager assetManager = getAssets();
//		double[][] DEM = null;
//		try {
//			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(DEMName + ".asc")));
//			String line = bufferedReader.readLine();
//			String[] lineArray = line.split("\\s+");
//			int numcols = Integer.parseInt(lineArray[1]);
//			line = bufferedReader.readLine();
//			lineArray = line.split("\\s+");
//			int numrows = Integer.parseInt(lineArray[1]);
//			line = bufferedReader.readLine();
//			lineArray = line.split("\\s+");
//			double xLLCorner = Double.parseDouble(lineArray[1]);
//			line = bufferedReader.readLine();
//			lineArray = line.split("\\s+");
//			double yLLCorner = Double.parseDouble(lineArray[1]);
//			line = bufferedReader.readLine();
//			lineArray = line.split("\\s+");
//			double cellSize = Double.parseDouble(lineArray[1]);
//			line = bufferedReader.readLine();
//			lineArray = line.split("\\s+");
//			double NaNValue = Double.parseDouble(lineArray[1]);
//			
//			DEM = new double[numrows][numcols]; 
//			int r = 0;
//			while (( line = bufferedReader.readLine()) != null){
//				lineArray = line.split("\\s+");
//				for (int c = 0; c < numcols; c++) {
//				    DEM[r][c] = Double.parseDouble(lineArray[c]);
//				}
//			r++;
//			}
//		} catch (NumberFormatException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {}
	}
}
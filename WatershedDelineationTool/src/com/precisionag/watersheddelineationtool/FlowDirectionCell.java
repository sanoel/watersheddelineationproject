package com.precisionag.watersheddelineationtool;

import java.util.ArrayList;

import android.graphics.Point;

public class FlowDirectionCell {
	Point childPoint;
	ArrayList<Point> parentList = null;

//	// Constructor method	
	public FlowDirectionCell(Point inputChildPoint) {
		childPoint = inputChildPoint;
	}
	
	public void setParentList(ArrayList<Point> inputParentList) {
		parentList = inputParentList;
	}
	
}

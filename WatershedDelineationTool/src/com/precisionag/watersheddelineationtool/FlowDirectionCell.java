package com.precisionag.watersheddelineationtool;

import java.util.ArrayList;

import android.graphics.Point;

public class FlowDirectionCell {
//	int currentCellRow;
//	int currentCellColumn;
	Point childPoint;
	ArrayList<Point> parentList = null;

//	// Constructor method
//	public FlowDirectionCell(int inputCurrentCellRow, int inputCurrentCellColumn, int inputChildRow, int inputChildColumn) {
//		childRow = inputChildRow;
//		childColumn = inputChildColumn;
//		currentCellRow = inputCurrentCellRow;
//		currentCellColumn = inputCurrentCellColumn;
//	}
	
	public FlowDirectionCell(Point inputChildPoint) {
		childPoint = inputChildPoint;
	}
	
	public void setParentList(ArrayList<Point> inputParentList) {
		parentList = inputParentList;
	}
	
}

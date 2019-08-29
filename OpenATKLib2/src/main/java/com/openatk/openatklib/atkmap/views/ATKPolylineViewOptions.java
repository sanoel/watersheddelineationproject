package com.openatk.openatklib.atkmap.views;

import android.graphics.Color;
import androidx.core.view.ViewCompat;

public class ATKPolylineViewOptions {
    private boolean blnLabelSelected = false;
    private int fillColor = Color.argb(200, 200, 200, 200);
    private int labelColor = ViewCompat.MEASURED_STATE_MASK;
    private int labelSelectedColor = -1;
    private int strokeColor = Color.argb(150, 150, 150, 150);
    private float strokeWidth = 3.0f;
    private boolean visible = true;
    private float zindex = 1.0f;

    public int getStrokeColor() {
        return this.strokeColor;
    }

    public void setStrokeColor(int strokeColor2) {
        this.strokeColor = strokeColor2;
    }

    public int getFillColor() {
        return this.fillColor;
    }

    public void setFillColor(int fillColor2) {
        this.fillColor = fillColor2;
    }

    public float getStrokeWidth() {
        return this.strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth2) {
        this.strokeWidth = strokeWidth2;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible2) {
        this.visible = visible2;
    }

    public float getZindex() {
        return this.zindex;
    }

    public void setZindex(float zindex2) {
        this.zindex = zindex2;
    }

    public boolean isBlnLabelSelected() {
        return this.blnLabelSelected;
    }

    public void setBlnLabelSelected(boolean blnLabelSelected2) {
        this.blnLabelSelected = blnLabelSelected2;
    }

    public int getLabelColor() {
        return this.labelColor;
    }

    public void setLabelColor(int labelColor2) {
        this.labelColor = labelColor2;
    }

    public int getLabelSelectedColor() {
        return this.labelSelectedColor;
    }

    public void setLabelSelectedColor(int labelSelectedColor2) {
        this.labelSelectedColor = labelSelectedColor2;
    }
}

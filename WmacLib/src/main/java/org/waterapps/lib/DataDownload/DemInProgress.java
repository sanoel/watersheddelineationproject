package org.waterapps.lib.DataDownload;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.openatk.openatklib.atkmap.ATKMap;
import com.openatk.openatklib.atkmap.models.ATKPoint;
import com.openatk.openatklib.atkmap.models.ATKPolygon;
import com.openatk.openatklib.atkmap.views.ATKPointView;
import com.openatk.openatklib.atkmap.views.ATKPolygonView;
import java.util.ArrayList;
import java.util.List;
import org.waterapps.lib.R;
import org.waterapps.lib.GeoRectangle;

public class DemInProgress extends GeoRectangle {
    float density = 3.0f;
    ATKPolygonView fill = null;
    ATKMap map = null;
    ATKPointView mark;
    ATKPolygonView outline = null;
    setProgress updater = null;

    private class setProgress extends AsyncTask<Long, Integer, Long> {
        private setProgress() {
        }

        /* synthetic */ setProgress(DemInProgress demInProgress, setProgress setprogress) {
            this();
        }

        /* access modifiers changed from: protected */
        public Long doInBackground(Long... urls) {
            for (int i = 0; i < 100; i++) {
                publishProgress(new Integer[]{Integer.valueOf(i)});
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isCancelled()) {
                    break;
                }
            }
            return Long.valueOf(Long.parseLong("1"));
        }

        /* access modifiers changed from: protected */
        public void onProgressUpdate(Integer... progress) {
            DemInProgress.this.updateProgress(((double) progress[0].intValue()) / 150.0d);
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Long result) {
        }
    }

    public DemInProgress(LatLngBounds extent, ATKMap map2, ATKPolygonView polygon) {
        super(extent);
        this.map = map2;
        this.outline = polygon;
        this.outline.setStrokeColor(-16776961);
        this.outline.update();
        BitmapDescriptor b = BitmapDescriptorFactory.fromResource(R.drawable.notification);
        this.mark = map2.addPoint(new ATKPoint("User", center()));
        this.mark.setAnchor(0.5f, 0.5f);
        this.mark.setIcon(b, 100, 200);
        this.mark.update();
        updateProgress(0.0d);
        this.updater = new setProgress(this, null);
        this.updater.execute(new Long[]{Long.valueOf(Long.parseLong("42"))});
    }

    public void cancelUpdate() {
        this.updater.cancel(true);
    }

    public void updateProgress(double progress) {
        if (this.fill != null) {
            this.fill.remove();
        }
        double top = this.south + ((this.north - this.south) * progress);
        List<LatLng> list = new ArrayList<>();
        list.add(getSW());
        list.add(getSE());
        list.add(new LatLng(top, this.east));
        list.add(new LatLng(top, this.west));
        this.fill = this.map.addPolygon(new ATKPolygon((Object) "prog", list));
        this.fill.setFillColor(Color.argb(128, 0, 0, 128));
        this.fill.setStrokeColor(0);
        this.fill.update();
    }

    public void remove() {
        this.updater.cancel(true);
        if (this.outline != null) {
            this.outline.remove();
        }
        if (this.fill != null) {
            this.fill.remove();
        }
        if (this.mark != null) {
            this.mark.remove();
        }
    }

    /* access modifiers changed from: 0000 */
    public Bitmap textToBitmap(String text) {
        Bitmap bitmap = Bitmap.createBitmap(dpToPx(180), dpToPx(80), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, null);
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(16.0f * this.density);
        textPaint.setColor(-1);
        StaticLayout sl = new StaticLayout(text, textPaint, bitmap.getWidth() - 8, Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        canvas.translate((float) ((int) (3.0f * this.density)), (float) ((int) (10.0f * this.density)));
        sl.draw(canvas);
        return bitmap;
    }

    /* access modifiers changed from: 0000 */
    public int dpToPx(int dp) {
        return (int) ((((float) dp) * this.density) + 0.5f);
    }
}

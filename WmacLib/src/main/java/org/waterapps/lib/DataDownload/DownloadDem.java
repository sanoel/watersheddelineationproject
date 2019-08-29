package org.waterapps.lib.DataDownload;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import androidx.core.app.NotificationCompat.Builder;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLngBounds;
import com.openatk.openatklib.atkmap.ATKMap;
import com.openatk.openatklib.atkmap.views.ATKPolygonView;
import java.io.File;

import org.waterapps.lib.R;

public class DownloadDem {
    static boolean busy;
    static boolean downloading;
    Context context;
    DemInProgress dlArea;

    /* renamed from: dm */
    DownloadManager f67dm;
    long enqueue;
    LatLngBounds extent;
    int notificationID = ((int) System.currentTimeMillis());
    BroadcastReceiver receiver;
    WebView webView;

    private class runWeb {
        private static final String errorString = "AsV2gZ2pxd9PcC8pJLkm";
        private static final String magicString = "25az225MAGICee4587da";
        Context context;
        LatLngBounds extent;

        private class PageHandler extends WebChromeClient {
            private PageHandler() {
            }

            /* synthetic */ PageHandler(runWeb runweb, PageHandler pageHandler) {
                this();
            }

            public boolean onConsoleMessage(ConsoleMessage cmsg) {
                if (cmsg.message().startsWith(runWeb.magicString)) {
                    String categoryMsg = cmsg.message().substring(runWeb.magicString.length());
                    Log.d("magic:", runWeb.magicString);
                    Log.d("Link:", categoryMsg);
                    DownloadDem.this.downloadFile(categoryMsg);
                    DownloadDem.this.webView.stopLoading();
                    DownloadDem.this.webView.destroy();
                    DownloadDem.downloading = false;
                    return true;
                } else if (cmsg.message().startsWith(runWeb.errorString)) {
                    Log.d("onConsoleMessage", "no dem data available");
                    Toast.makeText(runWeb.this.context, "No DEM data available for this region", Toast.LENGTH_LONG).show();
                    ((NotificationManager) runWeb.this.context.getSystemService("notification")).cancel(DownloadDem.this.notificationID);
                    DownloadDem.this.dlArea.remove();
                    DownloadDem.this.webView.stopLoading();
                    DownloadDem.this.webView.destroy();
                    DownloadDem.downloading = false;
                    return true;
                } else {
                    Log.d("javascript log: ", cmsg.message());
                    return false;
                }
            }
        }

        public runWeb(LatLngBounds extent2, Context con) {
            this.extent = extent2;
            this.context = con;
        }

        /* access modifiers changed from: private */
        public void run() {
            final double minX = this.extent.southwest.longitude < this.extent.northeast.longitude ? this.extent.southwest.longitude : this.extent.northeast.longitude;
            final double minY = this.extent.southwest.latitude < this.extent.northeast.latitude ? this.extent.southwest.latitude : this.extent.northeast.latitude;
            final double maxX = this.extent.southwest.longitude > this.extent.northeast.longitude ? this.extent.southwest.longitude : this.extent.northeast.longitude;
            final double maxY = this.extent.southwest.latitude > this.extent.northeast.latitude ? this.extent.southwest.latitude : this.extent.northeast.latitude;
            DownloadDem.this.webView = new WebView(this.context);
            DownloadDem.this.webView.getSettings().setJavaScriptEnabled(true);
            DownloadDem.this.webView.setWebChromeClient(new PageHandler(this, null));
            DownloadDem.this.webView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    Log.d("onPageFinished", "url:" + url);
                    if (url.contains("datasets")) {
                        return;
                    }
                    if (url.contains("http://opentopo.sdsc.edu/gridsphere/gridsphere?cid=geonlidarframeportlet&gs_action=lidarDataset&opentopoID=OTLAS.062012.4326.1")) {
                        int i = VERSION.SDK_INT;
                        Log.d("onPageFinished", "Submitting form");
                        String strFunction = "javascript:setInterval(function(){\tif(document.getElementsByName('selectForm').length == 0) {\t/* keep waiting, page isn't finished loading */\t} else {\t\t/* fill in the form with our desired values */\t\tdocument.getElementById('select_coordinates').checked = 'checked';\t\tdocument.getElementById('minX').value = '" + Double.toString(minX) + "';" + "\t\tdocument.getElementById('minY').value = '" + Double.toString(minY) + "';" + "\t\tdocument.getElementById('maxX').value = '" + Double.toString(maxX) + "';" + "\t\tdocument.getElementById('maxY').value = '" + Double.toString(maxY) + "';" + "\t\tdocument.getElementById('auto_xmin').value = '" + Double.toString(minX) + "';" + "\t\tdocument.getElementById('auto_ymin').value = '" + Double.toString(minY) + "';" + "\t\tdocument.getElementById('auto_xmax').value = '" + Double.toString(maxX) + "';" + "\t\tdocument.getElementById('auto_ymax').value = '" + Double.toString(maxY) + "';" + "\t\tdocument.getElementById('classes_Unclassified').checked = 'unchecked';" + "\t\tdocument.getElementById('idwView').checked = 'checked';" + "\t\tdocument.getElementById('resolution').value = '" + Double.toString(3.0d) + "';" + "\t\tdocument.getElementById('radius').value = '" + Double.toString(3.0d) + "';" + "\t\tdocument.getElementById('format').value = 'GTiff';" + "\t\tdocument.getElementById('nullFill').value = '7';" + "\t\tdocument.getElementById('tin').checked = 'unchecked';" + "\t\tdocument.getElementById('derivativeSelect').checked = 'unchecked';" + "\t\tdocument.getElementById('visualization').checked = 'unchecked';" + "\t\tdocument.getElementById('email').value = 'name@somewhere.com';" + "\t\t/* submit it */" + "\t\tdocument.getElementsByName('selectForm')[0].submit();" + "    clearInterval();" + "\t}" + "}, 5000);";
                        DownloadDem.this.webView.loadUrl(strFunction);
                        return;
                    }
                    Log.d("onPageFinished", "Searching for DEM");
                    Log.d("onPageFinished", "URL:" + url);
                    String strFunction2 = "javascript:setInterval(function(){\tvar els = document.getElementsByTagName('a');\tfor (var i = 0, l = els.length; i < l; i++) {\t\t\tvar el = els[i];\t\t\tif (el.innerHTML.indexOf('dems.tar.gz') != -1) {\t\t\t\t\tjavascript:console.log('25az225MAGICee4587da'+ el.href);\t\t\t\t\tclearInterval();\t\t\t}\t\t}}, 5000);";
                    Log.d("url:", strFunction2);
                    DownloadDem.this.webView.loadUrl(strFunction2);
                }
            });
            DownloadDem.this.webView.loadUrl("http://opentopo.sdsc.edu/gridsphere/gridsphere?cid=geonlidarframeportlet&gs_action=lidarDataset&opentopoID=OTLAS.062012.4326.1");
        }
    }

    public DownloadDem(LatLngBounds extent2, final String directory, ATKMap map, Context con, ATKPolygonView polygon) {
        downloading = true;
        this.extent = extent2;
        this.context = con;
        this.receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.DOWNLOAD_COMPLETE".equals(intent.getAction())) {
                    long longExtra = intent.getLongExtra("extra_download_id", 0);
                    Query query = new Query();
                    query.setFilterById(new long[]{DownloadDem.this.enqueue});
                    Cursor c = DownloadDem.this.f67dm.query(query);
                    if (c.moveToFirst() && 8 == c.getInt(c.getColumnIndex("status"))) {
                        File in = new File(c.getString(c.getColumnIndex("local_filename")));
                        gzip.extractGzip(in, new File(directory));
                        in.delete();
                        ((NotificationManager) context.getSystemService("notification")).notify(DownloadDem.this.notificationID, new Builder(context).setSmallIcon(R.drawable.done).setContentTitle("DEM download").setContentText("Download complete").setProgress(0, 0, false).build());
                        DownloadDem.this.dlArea.remove();
                    }
                }
                context.unregisterReceiver(DownloadDem.this.receiver);
            }
        };
        this.context.registerReceiver(this.receiver, new IntentFilter("android.intent.action.DOWNLOAD_COMPLETE"));
        this.f67dm = (DownloadManager) this.context.getSystemService("download");
        ((NotificationManager) this.context.getSystemService("notification")).notify(this.notificationID, new Builder(this.context).setSmallIcon(R.drawable.notification).setContentTitle("DEM download").setContentText("Download in progress").setProgress(0, 0, true).build());
        this.dlArea = new DemInProgress(extent2, map, polygon);
        new runWeb(extent2, this.context).run();
    }

    /* access modifiers changed from: private */
    public void downloadFile(String url) {
        Log.d("downloadFile", url);
        this.enqueue = this.f67dm.enqueue(new Request(Uri.parse(url)));
        new Thread(new Runnable() {
            public void run() {
                boolean downloading = true;
                while (downloading) {
                    Query q = new Query();
                    q.setFilterById(new long[]{DownloadDem.this.enqueue});
                    Cursor cursor = DownloadDem.this.f67dm.query(q);
                    cursor.moveToFirst();
                    int bytes_downloaded = cursor.getInt(cursor.getColumnIndex("bytes_so_far"));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex("total_size"));
                    if (cursor.getInt(cursor.getColumnIndex("status")) == 8) {
                        downloading = false;
                    }
                    System.out.println("Downloading: " + (((double) bytes_downloaded) / ((double) bytes_total)));
                    cursor.close();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private int getProgressPercentage() {
        int DOWNLOADED_BYTES_SO_FAR_INT = 0;
        int TOTAL_BYTES_INT = 0;
        try {
            Cursor c = this.f67dm.query(new Query().setFilterById(new long[]{this.enqueue}));
            if (c.moveToFirst()) {
                DOWNLOADED_BYTES_SO_FAR_INT = (int) c.getLong(c.getColumnIndex("bytes_so_far"));
                TOTAL_BYTES_INT = (int) c.getLong(c.getColumnIndex("total_size"));
            }
            System.out.println("PERCEN ------" + DOWNLOADED_BYTES_SO_FAR_INT + " ------ " + TOTAL_BYTES_INT + "****" + 0);
            int PERCENTAGE = (DOWNLOADED_BYTES_SO_FAR_INT * 100) / TOTAL_BYTES_INT;
            System.out.println("PERCENTAGE % " + PERCENTAGE);
            return PERCENTAGE;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}

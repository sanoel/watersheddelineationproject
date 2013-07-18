package org.waterapps.watersheddelineation;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

/**
 * Created by steve on 7/15/13.
 */
public class HelpActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webview = new WebView(this);
        setContentView(webview);
        //String helpdoc = "<html><head><title>How to load DEMs</title><style type=\"text/css\">ol{margin:0;padding:0}.c7{max-width:468pt;background-color:#ffffff;padding:72pt 72pt 72pt 72pt}.c1{color:#1155cc;text-decoration:underline}.c2{color:inherit;text-decoration:inherit}.c6{font-style:italic}.c3{height:11pt}.c0{direction:ltr}.c4{vertical-align:super}.c5{color:#ff0000}.title{padding-top:0pt;line-height:1.15;text-align:left;color:#000000;font-size:21pt;font-family:\"Trebuchet MS\";padding-bottom:0pt}.subtitle{padding-top:0pt;line-height:1.15;text-align:left;color:#666666;font-style:italic;font-size:13pt;font-family:\"Trebuchet MS\";padding-bottom:10pt}li{color:#000000;font-size:11pt;font-family:\"Arial\"}p{color:#000000;font-size:11pt;margin:0;font-family:\"Arial\"}h1{padding-top:10pt;line-height:1.15;text-align:left;color:#000000;font-size:16pt;font-family:\"Trebuchet MS\";padding-bottom:0pt}h2{padding-top:10pt;line-height:1.15;text-align:left;color:#000000;font-size:13pt;font-family:\"Trebuchet MS\";font-weight:bold;padding-bottom:0pt}h3{padding-top:8pt;line-height:1.15;text-align:left;color:#666666;font-size:12pt;font-family:\"Trebuchet MS\";font-weight:bold;padding-bottom:0pt}h4{padding-top:8pt;line-height:1.15;text-align:left;color:#666666;font-size:11pt;text-decoration:underline;font-family:\"Trebuchet MS\";padding-bottom:0pt}h5{padding-top:8pt;line-height:1.15;text-align:left;color:#666666;font-size:11pt;font-family:\"Trebuchet MS\";padding-bottom:0pt}h6{padding-top:8pt;line-height:1.15;text-align:left;color:#666666;font-style:italic;font-size:11pt;font-family:\"Trebuchet MS\";padding-bottom:0pt}</style></head><body class=\"c7\"><p class=\"c0\"><span>1. Go to </span><span class=\"c1\"><a class=\"c2\" href=\"http://www.opentopography.org/\">http://www.opentopography.org/</a></span><span>, and click on the &lsquo;data&rsquo; tab</span></p><p class=\"c0\"><img height=\"144\" src=\"images/image05.png\" width=\"519\"></p><p class=\"c3 c0\"><span></span></p><p class=\"c0\"><span>2. Zoom in on your area of interest and click &lsquo;SELECT A REGION&rsquo;, then click and drag to select the region to download.</span></p><p class=\"c0\"><img height=\"312\" src=\"images/image04.png\" width=\"503\"></p><p class=\"c0\"><span>3. Scroll down below the map and click &lsquo;Get Data&rsquo;</span></p><p class=\"c0\"><img height=\"275\" src=\"images/image03.png\" width=\"566\"></p><p class=\"c0\"><span>4. Scroll down to the &lsquo;DEM Generation (Local Gridding)&rsquo; section of the page. </span></p><p class=\"c0\"><span>Here, set Grid Resolution appropriately. Directly below the map is a box with the text &lsquo;The selection area contains approximately </span><span class=\"c6\">N</span><span>&nbsp;points.&rsquo; An appropriate grid resolution is one such that N/Resolution</span><span class=\"c4\">2</span><span>&lt;1,000,000. </span><span class=\"c5\">TODO: figure out actual number</span></p><p class=\"c0\"><span>Also, set &lsquo;Grid Format&rsquo; to GeoTiff.</span></p><p class=\"c0\"><img height=\"155\" src=\"images/image02.png\" width=\"642\"></p><p class=\"c3 c0\"><span></span></p><p class=\"c0\"><span>5. Scroll to the bottom of the page. Enter your email address in the email field (results will be sent to this address), and then click Submit. </span></p><p class=\"c0\"><img height=\"111\" src=\"images/image01.png\" width=\"708\"></p><p class=\"c0\"><span>6. Wait. Depending on the size of the region you selected and server load, it can take anywhere from a few seconds to a few minutes to complete.</span></p><p class=\"c3 c0\"><span></span></p><p class=\"c0\"><span>7. Once processing completes, there will be a page section titled DEM Results. Scroll here and click on dems.tar.gz to download the DEM file.</span></p><p class=\"c3 c0\"><span></span></p><p class=\"c0\"><img height=\"84\" src=\"images/image00.png\" width=\"464\"></p><p class=\"c3 c0\"><span></span></p><p class=\"c0\"><span>8. Optional: Rename dems.tar.gz to something more descriptive.</span></p><p class=\"c3 c0\"><span></span></p><p class=\"c0\"><span>9. Repeat steps 1-8 for any additional areas of interest.</span></p><p class=\"c3 c0\"><span></span></p><p class=\"c0\"><span>10. Connect your Android device to your computer. Create a folder on the device called dem and then copy all of your .tar.gz files into that folder.</span></p><p class=\"c3 c0\"><span></span></p><p class=\"c3 c0\"><span></span></p><p class=\"c0\"><span>11. Download the app from the Play Store: </span><span class=\"c1\"><a class=\"c2\" href=\"https://play.google.com/store/apps/details?id=com.precisionag.waterplane\">https://play.google.com/store/apps/details?id=com.precisionag.waterplane</a></span></p><p class=\"c0 c3\"><span></span></p><p class=\"c0\"><span>12. Open the app and it will now be able to use your DEMs.</span></p></body></html>";
        //webview.loadData(helpdoc, "text/html", null);
        webview.loadUrl("file:///android_asset/Help.html");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }
}

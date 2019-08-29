package org.waterapps.lib.DataDownload;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

public class gzip {
    public static void extractGzip(File in, File outDir) {
        InputStream is = null;
        try {
            is = new FileInputStream(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            TarInputStream tis = new TarInputStream(new GZIPInputStream(new BufferedInputStream(is)));
            while (true) {
                TarEntry entry = tis.getNextEntry();
                if (entry == null) {
                    tis.close();
                    return;
                }
                byte[] data = new byte[2048];
                new Random();
                FileOutputStream fos = new FileOutputStream(outDir.getPath() + "/" + new Date().toString().replaceAll("\\s+", "").replaceAll("\\:+", "") + ".tif");
                Log.d("inputfilename", entry.getName());
                Log.d("outputfilename", outDir.getPath() + "/" + entry.getName());
                BufferedOutputStream dest = new BufferedOutputStream(fos);
                while (true) {
                    int count = tis.read(data);
                    if (count == -1) {
                        break;
                    }
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    /* access modifiers changed from: 0000 */
    public void extractTar(String in, String out) {
    }
}

package org.waterapps.watershed;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by steve on 7/17/13.
 */
public class LicenceDialog extends DialogPreference {
    public LicenceDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        CharSequence licence= "The MIT License (MIT)\n" +
                "\n" +
                "Copyright (c) 2013 Steven Ellis\n" +
                "\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
                "of this software and associated documentation files (the \"Software\"), to deal\n" +
                "in the Software without restriction, including without limitation the rights\n" +
                "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
                "copies of the Software, and to permit persons to whom the Software is\n" +
                "furnished to do so, subject to the following conditions:\n" +
                "\n" +
                "The above copyright notice and this permission notice shall be included in\n" +
                "all copies or substantial portions of the Software.\n" +
                "\n" +
                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
                "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
                "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN\n" +
                "THE SOFTWARE.\n";

        setDialogMessage(licence);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
        }
    }
}

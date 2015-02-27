package org.cm.podd.report.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FontUtil {
    private static final String TAG = "FontUtil";

    public static void overrideFonts(final Context context, final View v) {
        try {
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    overrideFonts(context, child);
                }
            } else if (v instanceof TextView) {
                Typeface typeface = ((TextView) v).getTypeface();
                Typeface desiredTypeface;
                if (typeface != null && typeface.isBold()) {
                    desiredTypeface = StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.BOLD);
                } else {
                    desiredTypeface = StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL);
                }
                ((TextView) v).setTypeface(desiredTypeface);

            }
        } catch (Exception e) {
            Log.e(TAG, "Something errors", e);
        }
    }
}

package org.cm.podd.report.util;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FontUtil {
    public static void overrideFonts(final Context context, final View v) {
        try {
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    overrideFonts(context, child);
                }
            } else if (v instanceof TextView) {
                Typeface face = StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL);
                ((TextView) v).setTypeface(face);
            }
        } catch (Exception e) {
        }
    }
}

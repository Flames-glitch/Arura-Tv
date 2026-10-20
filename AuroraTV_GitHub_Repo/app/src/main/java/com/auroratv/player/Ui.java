package com.auroratv.player;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** Lightweight appearance helpers. No AppCompat or min-API-21 calls. */
public final class Ui {
    public static final int WHITE = Color.rgb(242, 246, 255);
    public static final int MUTED = Color.rgb(153, 164, 191);
    public static final int ACCENT = Color.rgb(157, 183, 255);
    public static final int PANE = Color.rgb(21, 29, 48);
    public static final int CARD = Color.rgb(27, 39, 66);
    private Ui() { }

    public static int dp(Context context, float value) {
        return (int)(context.getResources().getDisplayMetrics().density * value + 0.5f);
    }
    public static Drawable rounded(Context context, int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radius));
        return shape;
    }
    public static Drawable outline(Context context, int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radius));
        shape.setStroke(dp(context, 2), ACCENT);
        return shape;
    }
    @SuppressWarnings("deprecation")
    public static void background(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 16) view.setBackground(drawable);
        else view.setBackgroundDrawable(drawable);
    }
    public static TextView text(Context context, String value, int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }
    public static void focusable(final View view, final int normal, final int radius) {
        view.setFocusable(true);
        view.setClickable(true);
        background(view, rounded(view.getContext(), normal, radius));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean focused) {
                background(v, focused ? outline(v.getContext(), Color.rgb(48, 65, 104), radius)
                        : rounded(v.getContext(), normal, radius));
                v.setScaleX(focused ? 1.025f : 1f);
                v.setScaleY(focused ? 1.025f : 1f);
            }
        });
    }
}

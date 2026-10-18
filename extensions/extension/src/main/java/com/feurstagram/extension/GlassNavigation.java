package com.feurstagram.extension;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/**
 * Applies the PrismGram floating-glass treatment to Instagram's existing
 * bottom navigation container without replacing or reordering its tab views.
 *
 * Android's public RenderEffect API blurs a view's own rendering, not arbitrary
 * content behind it. This first pass therefore uses a translucent surface,
 * hairline highlight and rounded inset shape; it deliberately does not claim
 * to provide true backdrop blur.
 */
public final class GlassNavigation implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final int GLASS = 0xD91A1A1A;
    private static final int HIGHLIGHT = 0x42FFFFFF;
    private static final int CORNER_DP = 30;
    private static final int SIDE_INSET_DP = 14;
    private static final int TOP_INSET_DP = 2;
    private static final int BOTTOM_INSET_DP = 7;

    private ViewGroup tabBar;

    private GlassNavigation(ViewGroup tabBar) {
        this.tabBar = tabBar;
    }

    public static void install(ViewGroup tabBar) {
        if (tabBar == null) return;
        tabBar.getViewTreeObserver().addOnGlobalLayoutListener(new GlassNavigation(tabBar));
    }

    @Override
    public void onGlobalLayout() {
        ViewGroup bar = tabBar;
        if (bar == null || bar.getWidth() <= 0 || bar.getHeight() <= 0) return;

        Context context = bar.getContext();
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(GLASS);
        pill.setCornerRadius(dp(context, CORNER_DP));
        pill.setStroke(dp(context, 1), HIGHLIGHT);

        bar.setBackground(new InsetDrawable(
                pill,
                dp(context, SIDE_INSET_DP),
                dp(context, TOP_INSET_DP),
                dp(context, SIDE_INSET_DP),
                dp(context, BOTTOM_INSET_DP)));
        bar.setClipToPadding(false);
        bar.setElevation(dp(context, 8));

        // Install only once for this tab-bar instance; avoid resetting Instagram's
        // layout or adding work to every subsequent frame.
        bar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        tabBar = null;
    }

    private static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}

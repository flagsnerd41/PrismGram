package com.feurstagram.extension;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * Visual chrome upgrades for PrismGram:
 * <ul>
 *   <li>Floating pill bottom tab bar (re-applied on every layout pass, because
 *       Instagram resets its tab bar styling)</li>
 *   <li>Soft gradient overlays at top and bottom of chats and Reels</li>
 * </ul>
 */
public final class FloatingChrome {

    private FloatingChrome() {}

    private static final int SIDE_DP = 14;
    private static final int BOTTOM_DP = 12;
    private static final int RADIUS_DP = 30;
    private static final int PILL_COLOR = 0xF01B1B1B;

    /** Marker so we can tell our background apart from Instagram's. */
    private static final class PillBackground extends GradientDrawable {}

    /** Install every floating treatment off the main tab-bar root. */
    public static void install(ViewGroup tabBar) {
        if (tabBar == null) return;
        ViewTreeObserver observer = tabBar.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new TabBarPillStyler(tabBar));
        observer.addOnGlobalLayoutListener(new OverlayInstaller(tabBar));
    }

    // -------------------------------------------------------------------------
    // Floating pill tab bar
    // -------------------------------------------------------------------------

    static final class TabBarPillStyler implements ViewTreeObserver.OnGlobalLayoutListener {
        private final ViewGroup root;

        TabBarPillStyler(ViewGroup root) {
            this.root = root;
        }

        @Override
        public void onGlobalLayout() {
            try {
                apply();
            } catch (Throwable ignored) {
                // Never let cosmetics crash Instagram.
            }
        }

        private void apply() {
            final Context context = root.getContext();
            if (context == null) return;

            // Prefer the real tab_bar container; fall back to the view we were given.
            View target = root;
            int tabBarId = Hiders.resolveId(context, "tab_bar");
            if (tabBarId != 0) {
                View found = root.getRootView().findViewById(tabBarId);
                if (found != null) target = found;
            }
            if (target.getWidth() == 0 || target.getHeight() == 0) return;

            // 1. Margins, so the bar floats clear of the screen edges.
            ViewGroup.LayoutParams lp = target.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                int side = dp(context, SIDE_DP);
                int bottom = dp(context, BOTTOM_DP);
                if (mlp.leftMargin != side || mlp.rightMargin != side
                        || mlp.bottomMargin != bottom) {
                    mlp.leftMargin = side;
                    mlp.rightMargin = side;
                    mlp.bottomMargin = bottom;
                    target.setLayoutParams(mlp);
                }
            }

            // 2. Rounded dark pill background (re-assert if Instagram replaced it).
            if (!(target.getBackground() instanceof PillBackground)) {
                PillBackground bg = new PillBackground();
                bg.setColor(PILL_COLOR);
                bg.setCornerRadius(dp(context, RADIUS_DP));
                target.setBackground(bg);
            }

            // 3. Clip the contents to the pill so square child backgrounds
            //    (and any top divider) can't poke out of the rounded corners.
            final float radius = dp(context, RADIUS_DP);
            target.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            target.setClipToOutline(true);
            target.setElevation(dp(context, 8));

            // 4. Let the shadow draw outside the parent.
            if (target.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) target.getParent();
                parent.setClipChildren(false);
                parent.setClipToPadding(false);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Gradient overlays (chats + Reels)
    // -------------------------------------------------------------------------

    static final class OverlayInstaller implements ViewTreeObserver.OnGlobalLayoutListener {
        private final ViewGroup root;

        OverlayInstaller(ViewGroup root) {
            this.root = root;
        }

        @Override
        public void onGlobalLayout() {
            try {
                install();
            } catch (Throwable ignored) {
                // Never let cosmetics crash Instagram.
            }
        }

        private void install() {
            Context context = root.getContext();
            if (context == null) return;
            View windowRoot = root.getRootView();
            if (!(windowRoot instanceof ViewGroup)) return;
            ViewGroup window = (ViewGroup) windowRoot;

            // Bottom of Reels
            ensureGradient(window, context, "clips_viewer_view_pager",
                    "prism_reels_bottom_blur", true, 120);

            // Chat thread: top and bottom
            ensureGradient(window, context, "thread_message_recycler_view",
                    "prism_chat_top_blur", false, 100);
            ensureGradient(window, context, "thread_message_recycler_view",
                    "prism_chat_bottom_blur", true, 120);

            // Inbox list: top fade
            ensureGradient(window, context, "inbox_refreshable_thread_list_recyclerview",
                    "prism_inbox_top_blur", false, 100);
        }

        private void ensureGradient(ViewGroup window, Context context, String anchorName,
                                    String tag, boolean darkAtBottom, int heightDp) {
            if (window.findViewWithTag(tag) != null) return;

            int anchorId = Hiders.resolveId(context, anchorName);
            if (anchorId == 0) return;
            View anchor = window.findViewById(anchorId);
            if (anchor == null || !(anchor.getParent() instanceof ViewGroup)) return;
            ViewGroup container = (ViewGroup) anchor.getParent();

            View gradient = makeVerticalGradient(context, darkAtBottom);
            gradient.setTag(tag);

            int height = dp(context, heightDp);
            int gravity = darkAtBottom ? Gravity.BOTTOM : Gravity.TOP;
            try {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, height);
                lp.gravity = gravity;
                container.addView(gradient, lp);
            } catch (Throwable t) {
                container.addView(gradient, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, height));
            }
        }

        /** darkAtBottom = true fades to dark at the bottom edge. */
        private View makeVerticalGradient(Context context, boolean darkAtBottom) {
            View v = new View(context);
            int[] colors = darkAtBottom
                    ? new int[]{Color.TRANSPARENT, 0xCC000000}
                    : new int[]{0xCC000000, Color.TRANSPARENT};
            v.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors));
            v.setClickable(false);
            v.setFocusable(false);
            return v;
        }
    }

    private static int dp(Context context, float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }
}

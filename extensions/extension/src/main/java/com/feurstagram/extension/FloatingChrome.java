package com.feurstagram.extension;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * Visual chrome upgrades for PrismGram:
 * <ul>
 *   <li>Floating pill bottom tab bar</li>
 *   <li>Soft gradient / blur-like overlays at top & bottom of chats</li>
 *   <li>Soft gradient at the bottom of Reels</li>
 * </ul>
 *
 * True live backdrop blur is limited inside a patched Instagram APK.
 * We apply RenderEffect blur where the platform allows it (Android 12+),
 * and fall back to high-quality dark gradients everywhere else.
 */
public final class FloatingChrome {

    private FloatingChrome() {}

    /** Install every floating / blur treatment off the main tab-bar root. */
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
        private boolean applied;

        TabBarPillStyler(ViewGroup root) {
            this.root = root;
        }

        @Override
        public void onGlobalLayout() {
            if (applied) return;
            Context context = root.getContext();
            if (context == null) return;

            // Prefer the real tab_bar id when present; otherwise style the root we were given.
            View target = root;
            int tabBarId = Hiders.resolveId(context, "tab_bar");
            if (tabBarId != 0) {
                View found = root.getRootView().findViewById(tabBarId);
                if (found != null) target = found;
            }

            // Margins so the bar floats above the system gesture / nav area
            int side = dp(context, 12);
            int bottom = dp(context, 10);
            ViewGroup.LayoutParams lp = target.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                mlp.leftMargin = side;
                mlp.rightMargin = side;
                mlp.bottomMargin = bottom;
                target.setLayoutParams(mlp);
            }

            // Rounded dark pill background
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xE61B1B1B); // semi-opaque dark
            bg.setCornerRadius(dp(context, 28));
            target.setBackground(bg);

            // Elevation for floating feel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                target.setElevation(dp(context, 8));
            }

            // Optional real blur on the bar itself (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    target.setRenderEffect(
                            RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP));
                } catch (Throwable ignored) {
                    // Some OEMs restrict RenderEffect on system views
                }
            }

            applied = true;
        }
    }

    // -------------------------------------------------------------------------
    // Gradient / blur overlays (chats + Reels)
    // -------------------------------------------------------------------------

    static final class OverlayInstaller implements ViewTreeObserver.OnGlobalLayoutListener {
        private final ViewGroup root;

        OverlayInstaller(ViewGroup root) {
            this.root = root;
        }

        @Override
        public void onGlobalLayout() {
            Context context = root.getContext();
            if (context == null) return;
            View windowRoot = root.getRootView();
            if (!(windowRoot instanceof ViewGroup)) return;
            ViewGroup window = (ViewGroup) windowRoot;

            // Bottom of Reels
            ensureBottomGradient(window, context, "clips_viewer_view_pager", "prism_reels_bottom_blur");

            // Chat thread – top & bottom
            ensureTopGradient(window, context, "thread_message_recycler_view", "prism_chat_top_blur");
            ensureBottomGradient(window, context, "thread_message_recycler_view", "prism_chat_bottom_blur");

            // Inbox list – top fade
            ensureTopGradient(window, context, "inbox_refreshable_thread_list_recyclerview", "prism_inbox_top_blur");
        }

        private void ensureBottomGradient(ViewGroup window, Context context, String anchorName, String tag) {
            if (window.findViewWithTag(tag) != null) return;

            int anchorId = Hiders.resolveId(context, anchorName);
            if (anchorId == 0) return;
            View anchor = window.findViewById(anchorId);
            if (anchor == null) return;

            View parent = (View) anchor.getParent();
            if (!(parent instanceof FrameLayout) && !(parent instanceof ViewGroup)) return;

            ViewGroup container = (ViewGroup) parent;

            View gradient = makeVerticalGradient(context, true);
            gradient.setTag(tag);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(context, 120)
            );
            lp.gravity = Gravity.BOTTOM;

            // If parent is not FrameLayout, wrap behaviour is best-effort
            try {
                container.addView(gradient, lp);
            } catch (Throwable t) {
                // Parent may not accept FrameLayout params; try generic
                container.addView(gradient,
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 120)));
            }
        }

        private void ensureTopGradient(ViewGroup window, Context context, String anchorName, String tag) {
            if (window.findViewWithTag(tag) != null) return;

            int anchorId = Hiders.resolveId(context, anchorName);
            if (anchorId == 0) return;
            View anchor = window.findViewById(anchorId);
            if (anchor == null) return;

            View parent = (View) anchor.getParent();
            if (!(parent instanceof ViewGroup)) return;
            ViewGroup container = (ViewGroup) parent;

            View gradient = makeVerticalGradient(context, false);
            gradient.setTag(tag);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(context, 100)
            );
            lp.gravity = Gravity.TOP;

            try {
                container.addView(gradient, lp);
            } catch (Throwable t) {
                container.addView(gradient,
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 100)));
            }
        }

        /** topToBottom = true → dark at bottom (for Reels / chat bottom) */
        private View makeVerticalGradient(Context context, boolean darkAtBottom) {
            View v = new View(context);
            int[] colors = darkAtBottom
                    ? new int[]{Color.TRANSPARENT, 0xCC000000}
                    : new int[]{0xCC000000, Color.TRANSPARENT};
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
            v.setBackground(gd);
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

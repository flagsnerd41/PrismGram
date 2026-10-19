package com.feurstagram.extension;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

/**
 * Runtime configuration backed by SharedPreferences ("feurstagram_prefs").
 *
 * Independent block toggles (feed, explore, reels, stories, instants, notes,
 * suggested, ads) plus the permanent "hardcore" lock. The application Context
 * is resolved reflectively so no patched class needs to hand us one.
 */
public final class Config {

    private static final String PREFS = "feurstagram_prefs";

    private static boolean sNeedsRestart;

    private static HashMap<String, Boolean> sBaseline;

    private Config() {}

    public static Context getAppContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object app = activityThread.getMethod("currentApplication").invoke(null);
            return (Context) app;
        } catch (Throwable t) {
            return null;
        }
    }

    private static SharedPreferences prefs() {
        Context context = getAppContext();
        return context == null ? null : context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean getBlocked(String key, boolean defaultValue) {
        SharedPreferences prefs = prefs();
        return prefs == null ? defaultValue : prefs.getBoolean(key, defaultValue);
    }

    public static void setBlocked(String key, boolean value) {
        if (isHardcoreMode()
                && isLockable(key)
                && !hidesSurface(key, value)
                && wasHiddenAtBaseline(key)) {
            return;
        }
        SharedPreferences prefs = prefs();
        if (prefs == null) return;
        prefs.edit().putBoolean(key, value).apply();
    }

    public static boolean isLockable(String key) {
        return key != null && (key.startsWith("block_") || key.startsWith("nav_show_"));
    }

    public static boolean hidesSurface(String key, boolean value) {
        if (key == null) return false;
        return key.startsWith("nav_show_") ? !value : value;
    }

    public static boolean isHardcoreMode() {
        return getBlocked("hardcore_mode", false);
    }

    public static void enableHardcoreMode() {
        SharedPreferences prefs = prefs();
        if (prefs == null) return;
        prefs.edit().putBoolean("hardcore_mode", true).apply();
    }

    public static boolean isAutoUpdateEnabled() {
        return getBlocked("auto_update", true);
    }

    public static boolean isForceSdr() { return getBlocked("force_sdr", true); }

    public static boolean isFollowingFeedOnly() {
        return getBlocked("limit_following_feed", false);
    }

    public static boolean isFeedBlocked()      { return getBlocked("block_feed", true); }
    public static boolean isExploreBlocked()   { return getBlocked("block_explore", true); }
    // PrismGram: Reels enabled by default so users can test bottom blur / UI polish
    public static boolean isReelsBlocked()     { return getBlocked("block_reels", false); }
    public static boolean isFriendsLaneBlocked() { return getBlocked("block_friends_lane", true); }
    public static boolean isStoriesBlocked()   { return getBlocked("block_stories", false); }
    public static boolean isInstantsBlocked()  { return getBlocked("block_instants", true); }
    public static boolean isNotesBlocked()     { return getBlocked("block_notes", true); }
    public static boolean isSuggestedBlocked() { return getBlocked("block_suggested", true); }
    public static boolean isAdsBlocked()       { return getBlocked("block_ads", true); }

    public static boolean isNotificationsButtonBlocked() { return getBlocked("block_notifications", false); }

    public static boolean arePopupsHidden() { return getBlocked("hide_toasts", true); }

    // PrismGram: Reels tab shown by default
    public static boolean isReelsTabShown() { return getBlocked("nav_show_reels", true); }

    public static boolean isOnboardingDone() { return getBlocked("onboarding_done", false); }

    public static void setOnboardingDone() { setBlocked("onboarding_done", true); }

    private static final String[] NAV_KEYS = {
            "nav_show_search", "nav_show_reels", "nav_show_create",
            "nav_show_direct", "nav_show_profile",
    };

    // All nav tabs shown by default, including Reels
    public static boolean navDefault(String key) {
        return true;
    }

    public static boolean isNavTabShown(String navKey) {
        return getBlocked(navKey, navDefault(navKey));
    }

    public static String navKeyForLanding(String landing) {
        if ("search".equals(landing)) return "nav_show_search";
        if ("direct".equals(landing)) return "nav_show_direct";
        if ("profile".equals(landing)) return "nav_show_profile";
        return null;
    }

    public static boolean isLandingAvailable(String landing) {
        String navKey = navKeyForLanding(landing);
        return navKey == null || isNavTabShown(navKey);
    }

    public static void captureBaseline() {
        HashMap<String, Boolean> baseline = new HashMap<>();
        baseline.put("block_feed", isFeedBlocked());
        baseline.put("block_explore", isExploreBlocked());
        baseline.put("block_reels", isReelsBlocked());
        baseline.put("block_friends_lane", isFriendsLaneBlocked());
        baseline.put("block_stories", isStoriesBlocked());
        baseline.put("block_instants", isInstantsBlocked());
        baseline.put("block_notes", isNotesBlocked());
        baseline.put("block_suggested", isSuggestedBlocked());
        baseline.put("block_ads", isAdsBlocked());
        for (String key : NAV_KEYS) {
            baseline.put(key, getBlocked(key, navDefault(key)));
        }
        sBaseline = baseline;
    }

    public static boolean wasHiddenAtBaseline(String key) {
        Boolean captured = sBaseline == null ? null : sBaseline.get(key);
        boolean value = captured != null ? captured : getBlocked(key, lockableDefault(key));
        return hidesSurface(key, value);
    }

    private static boolean lockableDefault(String key) {
        if (key == null) return false;
        if (key.startsWith("nav_show_")) return navDefault(key);
        return false;
    }

    public static String getLandingPage() {
        SharedPreferences prefs = prefs();
        String stored = prefs == null ? "home" : prefs.getString("landing_page", "home");
        return isLandingAvailable(stored) ? stored : "home";
    }

    public static void setLandingPage(String value) {
        if (!isLandingAvailable(value)) return;
        SharedPreferences prefs = prefs();
        if (prefs == null) return;
        prefs.edit().putString("landing_page", value).apply();
    }

    public static String getLastSeenVersion() {
        SharedPreferences prefs = prefs();
        return prefs == null ? null : prefs.getString("last_seen_version", null);
    }

    public static void setLastSeenVersion(String value) {
        SharedPreferences prefs = prefs();
        if (prefs == null || value == null) return;
        prefs.edit().putString("last_seen_version", value).apply();
    }

    public static long getFollowPromptShownFor() {
        SharedPreferences prefs = prefs();
        return prefs == null ? 0L : prefs.getLong("follow_prompt_update_time", 0L);
    }

    public static void setFollowPromptShownFor(long updateTime) {
        SharedPreferences prefs = prefs();
        if (prefs == null) return;
        prefs.edit().putLong("follow_prompt_update_time", updateTime).apply();
    }

    public static void setNeedsRestart() { sNeedsRestart = true; }

    public static boolean isRestartPending() { return sNeedsRestart; }
}

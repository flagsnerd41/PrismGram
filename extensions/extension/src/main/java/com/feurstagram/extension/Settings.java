package com.feurstagram.extension;

// TEMP: run restore from faa7492 - full content too large for this channel
// Use: git show faa7492:extensions/extension/src/main/java/com/feurstagram/extension/Settings.java
public final class Settings {
    private Settings() {}
    public static void installHomeTabWatcher(android.view.ViewGroup tabBar) {
        if (tabBar == null) return;
        tabBar.getViewTreeObserver().addOnGlobalLayoutListener(new HomeTabWatcher(tabBar));
        Hiders.installAll(tabBar);
        android.content.Context activity = getActivityContext(tabBar);
        FollowPrompt.maybeShow(activity, () -> UpdateChecker.checkWhatsNew(activity));
        UpdateChecker.check(activity);
    }
    public static android.content.Context getActivityContext(android.view.View view) {
        if (view == null) return null;
        android.content.Context context = view.getContext();
        android.content.Context cursor = context;
        while (cursor != null && !(cursor instanceof android.app.Activity) && cursor instanceof android.content.ContextWrapper) {
            cursor = ((android.content.ContextWrapper) cursor).getBaseContext();
        }
        return cursor != null ? cursor : context;
    }
    // Minimal stub so the project compiles until full Settings is restored from faa7492
    static final class HomeTabWatcher implements android.view.ViewTreeObserver.OnGlobalLayoutListener {
        private final android.view.ViewGroup root;
        HomeTabWatcher(android.view.ViewGroup root) { this.root = root; }
        public void onGlobalLayout() {}
    }
}

package org.sliit.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import org.sliit.FeedPreferenceActivity;
import org.sliit.R;

public final class SharedPreferencesHelper {
	
	private static final String LOG_TAG = "SharedPreferencesHelper";
	
	public static final int DIALOG_ABOUT = 0;
	public static final int DIALOG_NO_CONNECTION = 1;
	public static final int DIALOG_UPDATE_PROGRESS = 2;
	
	public static final int CHANNEL_SUBMENU_GROUP = 0;
	
	private static final String PREFS_FILE_NAME = "AppPreferences";
	
	private static final String PREF_TAB_FEED_KEY = "tabFeed";
	private static final String PREF_MAX_DOWNLOAD_KEY = "maxDownload";
	private static final String PREF_SPLASH_DURATION_KEY = "splashDuration";
	
	private static final int DEFAULT_MAX_DOWNLOAD_PER_FEED = Integer.parseInt(FeedPreferenceActivity.DEFAULT_MAX_ITEMS_PER_FEED);
	
    public static long getPrefTabFeedId(Context ctx) {
    	long default_tab_feed_id = Long.parseLong(ctx.getString(R.string.app_pref_default_tab_feed_id));
    	return ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getLong(PREF_TAB_FEED_KEY, default_tab_feed_id);
    }
    
    public static void setPrefTabFeedId(Context ctx, long feedId) {
    	SharedPreferences prefs = ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    	Editor editor = prefs.edit();
    	editor.putLong(PREF_TAB_FEED_KEY, feedId);
    	editor.commit();
    }
    
    public static int getPrefMaxDownload(Context ctx) {
    	return ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getInt(PREF_MAX_DOWNLOAD_KEY, DEFAULT_MAX_DOWNLOAD_PER_FEED);
    }
    
    public static int getPrefSplashDuration(Context ctx) {
    	int default_splash_screen_duration = Integer.parseInt(ctx.getString(R.string.app_pref_default_splash_screen_duration));
    	return ctx.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).getInt(PREF_SPLASH_DURATION_KEY, default_splash_screen_duration);
    }
    
    public static long getPrefStartChannel(Context ctx) {
    	return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPreferenceActivity.PREF_START_CHANNEL_KEY, FeedPreferenceActivity.DEFAULT_START_CHANNEL));
    }
    
    public static int getItemView(Context ctx) {
    	return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPreferenceActivity.PREF_ITEM_VIEW_KEY, FeedPreferenceActivity.DEFAULT_ITEM_VIEW));
    }
    
    public static int getPrefMaxItems(Context ctx) {
    	return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPreferenceActivity.PREF_MAX_ITEMS_KEY, FeedPreferenceActivity.DEFAULT_MAX_ITEMS_PER_FEED));
    }
    
    public static long getPrefMaxHours(Context ctx) {
    	return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPreferenceActivity.PREF_MAX_HOURS_KEY, FeedPreferenceActivity.DEFAULT_MAX_HOURS_PER_FEED));
    }
    
    public static long getPrefUpdatePeriod(Context ctx) {
    	return Long.parseLong(PreferenceManager.getDefaultSharedPreferences(ctx).getString(FeedPreferenceActivity.PREF_UPDATE_PERIOD_KEY, FeedPreferenceActivity.DEFAULT_UPDATE_PERIOD));
    }
    
    public static CharSequence getVersionName(Context ctx) {
		CharSequence version_name = "";
		try {
			PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			version_name = packageInfo.versionName;
		} catch (NameNotFoundException nnfe) {
			Log.e(LOG_TAG,"",nnfe);
		}
		return version_name;
    }
    
    public static boolean isOnline(Context ctx) {
    	ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo ni = cm.getActiveNetworkInfo();
    	if (ni != null)
    		return ni.isConnectedOrConnecting();
    	else return false;
    }
}

package org.sliit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.sliit.domain.Feed;
import org.sliit.domain.Item;
import org.sliit.service.DbFeedAdapter;
import org.sliit.service.DbSchema;
import org.sliit.service.SharedPreferencesHelper;


import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class FeedWebActivity extends Activity {
	
	private static final String LOG_TAG = "FeedWebActivity";
	
	private DbFeedAdapter mDbFeedAdapter;
	private WebView webView;
	private long mItemId = -1;
	
	private class ItemWebViewClient extends WebViewClient {

		@Override
		public void onPageStarted (WebView view, String url, Bitmap favicon) {
			setProgressBarIndeterminateVisibility(true);
		}
		
		@Override
		public void onPageFinished (WebView view, String url) {
			setProgressBarIndeterminateVisibility(false);
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	// Request progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	
        mDbFeedAdapter = new DbFeedAdapter(this);
        mDbFeedAdapter.open();
    	
        setContentView(R.layout.webview);
        registerForContextMenu(findViewById(R.id.webview));

        mItemId = savedInstanceState != null ? savedInstanceState.getLong(DbSchema.ItemSchema._ID) : -1;

		if (mItemId == -1) {
			Bundle extras = getIntent().getExtras();            
			mItemId = extras != null ? extras.getLong(DbSchema.ItemSchema._ID) : -1;
		}
    }
    
    private void displayWebView() {
    	if (!isOnline())
			showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
		else if (mItemId != -1) {
			URL link = mDbFeedAdapter.getItem(mItemId).getLink();
			
			webView = (WebView) findViewById(R.id.webview);
	        webView.getSettings().setJavaScriptEnabled(true);
	        webView.getSettings().setBuiltInZoomControls(true);
	        webView.setInitialScale(70);
	        webView.loadUrl(link.toString());
	        webView.setWebViewClient(new ItemWebViewClient());
	        
	        // set item as read (case when item is displayed from next/previous contextual menu or buttons)
            ContentValues values = new ContentValues();
	    	values.put(DbSchema.ItemSchema.COLUMN_READ, DbSchema.ON);
	    	mDbFeedAdapter.updateItem(mItemId, values, null);
		}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mDbFeedAdapter.close();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	displayWebView();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DbSchema.ItemSchema._ID, mItemId);
    }
    
    private boolean isOnline() {
    	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo ni = cm.getActiveNetworkInfo();
    	if (ni != null)
    		return ni.isConnectedOrConnecting();
    	else return false;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
        	webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opt_item_menu, menu);
        
        // Home menu item
        MenuItem menuItem = (MenuItem) menu.findItem(R.id.menu_opt_home);
        menuItem.setIntent(new Intent(this, FeedTabActivity.class));     
        
        // Channels menu item
        MenuItem channelsMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_channels);
        SubMenu subMenu = channelsMenuItem.getSubMenu();
        
        List<Feed> feeds = mDbFeedAdapter.getFeeds();
        Iterator<Feed> feedIterator = feeds.iterator();
        Feed feed = null;
        MenuItem channelSubMenuItem = null;
        Intent intent = null;
        int order = 0;
		while (feedIterator.hasNext()) {
			feed = feedIterator.next();
			channelSubMenuItem = subMenu.add(SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP, Menu.NONE, order, feed.getTitle());
			
			if (feed.getId() == SharedPreferencesHelper.getPrefTabFeedId(this))
				channelSubMenuItem.setChecked(true);
			
			intent = new Intent(this, FeedTabActivity.class);
	        intent.putExtra(DbSchema.FeedSchema._ID, feed.getId());
			channelSubMenuItem.setIntent(intent);
			
			order++;
		}

        subMenu.setGroupCheckable(SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP, true, true);
        
     	// Preferences menu item
        MenuItem preferencesMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_preferences);
        preferencesMenuItem.setIntent(new Intent(this,FeedPrefActivity.class));
       
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_opt_home:
	        	startActivity(item.getIntent());
		    	setResult(RESULT_OK);
		    	//finish();
	        	return true;
	        case R.id.menu_opt_channels:
	        	//do nothing
	            return true;
	        case R.id.menu_opt_preferences:
	        	startActivity(item.getIntent());
	            return true;
	        case R.id.menu_opt_about:
	        	showDialog(SharedPreferencesHelper.DIALOG_ABOUT);
	            return true;
	        default:
	        	if (item.getGroupId() == SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP) {
	        		startActivity(item.getIntent());
	        		setResult(RESULT_OK);
	        		//finish();
	        		return true;
	        	}
	    }
        return false;
    }
    
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
		if (v.getId() == R.id.webview) {
			menu.setHeaderTitle (R.string.ctx_menu_title);
			MenuInflater inflater = getMenuInflater();
			
			Item item = mDbFeedAdapter.getItem(mItemId);
			
			if (item != null) {
				long feedId = mDbFeedAdapter.getItemFeedId(mItemId);
				boolean isFirstItem = false;
				boolean isLastItem = false;
				if (mItemId == mDbFeedAdapter.getFirstItem(feedId).getId())
					isFirstItem = true;
				else if (mItemId == mDbFeedAdapter.getLastItem(feedId).getId())
					isLastItem = true;
				
				if (item.isFavorite()) {
					if (isFirstItem)
						inflater.inflate(R.menu.ctx_menu_item_online_notfav_next, menu);
					else if (isLastItem)
						inflater.inflate(R.menu.ctx_menu_item_online_notfav_prev, menu);
					else
						inflater.inflate(R.menu.ctx_menu_item_online_notfav_next_prev, menu);
				} else {
					if (isFirstItem)
						inflater.inflate(R.menu.ctx_menu_item_online_fav_next, menu);
					else if (isLastItem)
						inflater.inflate(R.menu.ctx_menu_item_online_fav_prev, menu);
					else
						inflater.inflate(R.menu.ctx_menu_item_online_fav_next_prev, menu);
				}
			}
		}
    }

    
    public boolean onContextItemSelected(MenuItem menuItem) {
    	Item item = mDbFeedAdapter.getItem(mItemId);
    	ContentValues values = null;
    	Intent intent = null;
    	long feedId = -1;
    	
    	switch (menuItem.getItemId()) {
    		case R.id.add_fav:
    			//item.favorite();
    			values = new ContentValues();
    	    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.ON);
    	    	mDbFeedAdapter.updateItem(mItemId, values, null);
    			Toast.makeText(this, R.string.add_fav_msg, Toast.LENGTH_SHORT).show();
    			return true;
    		case R.id.remove_fav:
    			//item.unfavorite();
    			Date now = new Date();
    			long diffTime = now.getTime() - item.getPubdate().getTime();
    			long maxTime = SharedPreferencesHelper.getPrefMaxHours(this) * 60 * 60 * 1000; // Max hours expressed in milliseconds
    			// test if item has expired
    			if (maxTime > 0 && diffTime > maxTime) {
	    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    			builder.setMessage(R.string.remove_fav_confirmation)
	    			       .setCancelable(false)
	    			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	    			           public void onClick(DialogInterface dialog, int id) {
									ContentValues values = new ContentValues();
									values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.OFF);
									mDbFeedAdapter.updateItem(mItemId, values, null);
									Toast.makeText(FeedWebActivity.this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
	    			           }
	    			       })
	    			       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	    			           public void onClick(DialogInterface dialog, int id) {
	    			                dialog.cancel();
	    			           }
	    			       });
	    			builder.create().show();
    			} else {
    				values = new ContentValues();
        	    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.OFF);
        	    	mDbFeedAdapter.updateItem(mItemId, values, null);
        			Toast.makeText(this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
    			}
    			return true;
    		case R.id.next:
    			feedId = mDbFeedAdapter.getItemFeedId(mItemId);
    			intent = new Intent(this, FeedWebActivity.class);
    	        intent.putExtra(DbSchema.ItemSchema._ID, mDbFeedAdapter.getNextItemId(feedId, mItemId));
    	        startActivity(intent);
    	        finish();
    			return true;
    		case R.id.previous:
    			feedId = mDbFeedAdapter.getItemFeedId(mItemId);
    			intent = new Intent(this, FeedWebActivity.class);
    	        intent.putExtra(DbSchema.ItemSchema._ID, mDbFeedAdapter.getPreviousItemId(feedId, mItemId));
    	        startActivity(intent);
    	        finish();
    			return true;
    		case R.id.share:
    			item = mDbFeedAdapter.getItem(mItemId);
    			if (item != null) {
	    			Intent shareIntent = new Intent(Intent.ACTION_SEND);
	                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
	                shareIntent.putExtra(Intent.EXTRA_TEXT, item.getTitle() + " " + item.getLink().toString());
	                shareIntent.setType("text/plain");
	                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    			}
    			return true;
    		default:
    			return super.onContextItemSelected(menuItem);
    	}
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog = null;
    	CharSequence title = null;
    	LayoutInflater inflater = null;
    	View dialogLayout = null;
    	AlertDialog.Builder builder = null;
        switch (id) {
        	case SharedPreferencesHelper.DIALOG_ABOUT:
        		//title = getResources().getText(R.string.app_name) + " - " + getResources().getText(R.string.version) + " " + SharedPreferencesHelper.getVersionName(this);
	        	title = getString(R.string.app_name) + " - " + getString(R.string.version) + " " + SharedPreferencesHelper.getVersionName(this);
	        	
	        	/*
	        	 * Without cancel button
	        	dialog = new Dialog(this);
	        	dialog.setContentView(R.layout.dialog_about);
	        	dialog.setTitle(title);
	        	*/
        		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        		dialogLayout = inflater.inflate(R.layout.dialog_about, null);
        		builder = new AlertDialog.Builder(this);
        		builder.setView(dialogLayout)
        			   .setTitle(title)
        			   .setIcon(R.drawable.icon)
        			   .setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		                dialog.cancel();
        		           }
        		       });
        		dialog = builder.create();
	        	break;
        	case SharedPreferencesHelper.DIALOG_NO_CONNECTION:
        		title = getString(R.string.error);
        		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        		dialogLayout = inflater.inflate(R.layout.dialog_no_connection, null);
        		builder = new AlertDialog.Builder(this);
        		builder.setView(dialogLayout)
        			   .setTitle(title)
        			   .setIcon(R.drawable.icon)
        			   .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
        		           public void onClick(DialogInterface dialog, int id) {
        		                dialog.cancel();
        		                finish();
        		           }
        		       });
        		dialog = builder.create();
        		break;
            default:
            	dialog = null;
        }
        return dialog;
    }
}
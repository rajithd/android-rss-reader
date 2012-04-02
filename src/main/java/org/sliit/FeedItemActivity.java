package org.sliit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.sliit.domain.Feed;
import org.sliit.domain.Item;
import org.sliit.service.RepositoryController;
import org.sliit.service.DbSchema;
import org.sliit.service.SharedPreferencesHelper;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class FeedItemActivity extends Activity {
	
	private static final String LOG_TAG = "FeedItemActivity";
	
	private RepositoryController mRepositoryController;
	private long mItemId = -1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
        mRepositoryController = new RepositoryController(this);
        mRepositoryController.open();

        mItemId = savedInstanceState != null ? savedInstanceState.getLong(DbSchema.ItemSchema._ID) : -1;

		if (mItemId == -1) {
			Bundle extras = getIntent().getExtras();            
			mItemId = extras != null ? extras.getLong(DbSchema.ItemSchema._ID) : -1;
		}
		
		Item item = mRepositoryController.getItem(mItemId);
		if (item.isFavorite())
			setContentView(R.layout.item_favorite);
		else
			setContentView(R.layout.item_notfavorite);
			
        TextView title = (TextView)findViewById(R.id.title);
        title.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		adjustLinkableTextColor (v);
        		startItemWebActivity();
            }
        });
        
        TextView channel = (TextView)findViewById(R.id.channel);
        channel.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		adjustLinkableTextColor (v);
        		if (SharedPreferencesHelper.isOnline(FeedItemActivity.this)) {
        			Feed feed = mRepositoryController.getFeed(mRepositoryController.getItemFeedId(mItemId));
        			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(feed.getHomePage().toString()));
        	        startActivity(intent);
            	} else
            		showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
            }
        });
        
        TextView read_online = (TextView)findViewById(R.id.read);
        read_online.setOnClickListener(new OnClickListener() {
        	@Override
            public void onClick(View v) {
        		adjustLinkableTextColor (v);
        		startItemWebActivity();
            }
        });
        
        registerForContextMenu(findViewById(R.id.item));
    }
    
    private void startItemWebActivity() {
    	if (SharedPreferencesHelper.isOnline(FeedItemActivity.this)) {
			Intent intent = new Intent(FeedItemActivity.this, FeedWebActivity.class);
	        intent.putExtra(DbSchema.ItemSchema._ID, mItemId);
	        startActivity(intent);
    	} else
    		showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
    }
    
    private void adjustLinkableTextColor (View v) {
    	TextView textView = (TextView) v;
    	textView.setTextColor(R.color.color2);
    	
    }
    
    private void displayItemView() {
    	if (mItemId != -1) {
    		Item item = mRepositoryController.getItem(mItemId);
    		TextView titleView = (TextView) findViewById(R.id.title);
            TextView channelView = (TextView) findViewById(R.id.channel);
            TextView pubdateView = (TextView) findViewById(R.id.pubdate);
            TextView contentView = (TextView) findViewById(R.id.content);
            if (titleView != null)
            	titleView.setText(item.getTitle());
            if (channelView != null) {
            	Feed feed = mRepositoryController.getFeed(mRepositoryController.getItemFeedId(mItemId));
            	if (feed != null)
            		channelView.setText(feed.getTitle());
            } 
            if (pubdateView != null) {
            	//DateFormat df = new SimpleDateFormat(getResources().getText(R.string.pubdate_format_pattern);
            	//pubdateView.setText(df.format(item.getPubdate()));
            	CharSequence formattedPubdate = DateFormat.format(getResources().getText(R.string.pubdate_format_pattern), item.getPubdate());
            	pubdateView.setText(formattedPubdate);
            }
            if (contentView != null) {
            	String content_description = item.getContent();
            	if (content_description == null)
            		content_description = item.getDescription();
            	if (content_description != null)
            		//contentView.setText(content_description,TextView.BufferType.SPANNABLE);
            		contentView.setText(content_description);
            }
            
            // set item as read (case when item is displayed from next/previous contextual menu or buttons)
            ContentValues values = new ContentValues();
	    	values.put(DbSchema.ItemSchema.COLUMN_READ, DbSchema.ON);
	    	mRepositoryController.updateItem(mItemId, values, null);
    	}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mRepositoryController.close();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	displayItemView();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DbSchema.ItemSchema._ID, mItemId);
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
        
        List<Feed> feeds = mRepositoryController.getFeeds();
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
        preferencesMenuItem.setIntent(new Intent(this,FeedPreferenceActivity.class));
       
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
    	
		if (v.getId() == R.id.item) {
			menu.setHeaderTitle (R.string.ctx_menu_title);
			MenuInflater inflater = getMenuInflater();
			
			Item item = mRepositoryController.getItem(mItemId);
			
			if (item != null) {
				long feedId = mRepositoryController.getItemFeedId(mItemId);
				boolean isFirstItem = false;
				boolean isLastItem = false;
				if (mItemId == mRepositoryController.getFirstItem(feedId).getId())
					isFirstItem = true;
				else if (mItemId == mRepositoryController.getLastItem(feedId).getId())
					isLastItem = true;
				
				if (item.isFavorite()) {
					if (isFirstItem)
						inflater.inflate(R.menu.ctx_menu_item_offline_notfav_next, menu);
					else if (isLastItem)
						inflater.inflate(R.menu.ctx_menu_item_offline_notfav_prev, menu);
					else
						inflater.inflate(R.menu.ctx_menu_item_offline_notfav_next_prev, menu);
				} else {
					if (isFirstItem)
						inflater.inflate(R.menu.ctx_menu_item_offline_fav_next, menu);
					else if (isLastItem)
						inflater.inflate(R.menu.ctx_menu_item_offline_fav_prev, menu);
					else
						inflater.inflate(R.menu.ctx_menu_item_offline_fav_next_prev, menu);
				}
			}
		}
    }

    
    public boolean onContextItemSelected(MenuItem menuItem) {
    	Item item = mRepositoryController.getItem(mItemId);
    	ImageView favView = (ImageView) findViewById(R.id.fav);
    	ContentValues values = null;
    	Intent intent = null;
    	long feedId = -1;
    	
    	switch (menuItem.getItemId()) {
    		case R.id.read_online:
    			startItemWebActivity();
    			return true;
    		case R.id.add_fav:
    			//item.favorite();
    			values = new ContentValues();
    	    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.ON);
    	    	mRepositoryController.updateItem(mItemId, values, null);
    	    	favView.setImageResource(R.drawable.fav);
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
									ImageView favView = (ImageView) findViewById(R.id.fav);
					    	    	favView.setImageResource(R.drawable.no_fav);
									mRepositoryController.updateItem(mItemId, values, null);
									Toast.makeText(FeedItemActivity.this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
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
        	    	mRepositoryController.updateItem(mItemId, values, null);
	    	    	favView.setImageResource(R.drawable.no_fav);
        			Toast.makeText(this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
    			}
    			return true;
    		case R.id.next:
    			feedId = mRepositoryController.getItemFeedId(mItemId);
    			intent = new Intent(this, FeedItemActivity.class);
    	        intent.putExtra(DbSchema.ItemSchema._ID, mRepositoryController.getNextItemId(feedId, mItemId));
    	        startActivity(intent);
    	        finish();
    			return true;
    		case R.id.previous:
    			feedId = mRepositoryController.getItemFeedId(mItemId);
    			intent = new Intent(this, FeedItemActivity.class);
    	        intent.putExtra(DbSchema.ItemSchema._ID, mRepositoryController.getPreviousItemId(feedId, mItemId));
    	        startActivity(intent);
    	        finish();
    			return true;
    		case R.id.share:
    			item = mRepositoryController.getItem(mItemId);
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
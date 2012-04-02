package org.sliit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TabHost.OnTabChangeListener;
import org.sliit.domain.Item;
import org.sliit.service.RepositoryController;
import org.sliit.service.DbSchema;
import org.sliit.service.SharedPreferencesHelper;
import org.xml.sax.SAXException;
import org.sliit.domain.Feed;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class FeedTabActivity extends TabActivity implements OnItemClickListener {
	private static final String LOG_TAG = "FeedTabActivity";
	
	private static final String TAB_CHANNEL_TAG = "tab_tag_channel";
	private static final String TAB_FAV_TAG = "tab_tag_favorite";
	
	private static final int WEB_ACTIVITY_CODE = 1;
	
	private RepositoryController mRepositoryController;
	
	private boolean mIsOnline = true;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mRepositoryController = new RepositoryController(this);
        mRepositoryController.open();
        
        setContentView(R.layout.main);
        
        long feedId = SharedPreferencesHelper.getPrefTabFeedId(this);
        
        Bundle extras = getIntent().getExtras();
		if (extras != null) {
			feedId = extras.getLong(DbSchema.FeedSchema._ID);
			SharedPreferencesHelper.setPrefTabFeedId(this, feedId);
		}
        
        Feed currentTabFeed = mRepositoryController.getFeed(feedId);
        setTabs(TAB_CHANNEL_TAG, currentTabFeed.getTitle());
        
        getTabHost().setOnTabChangedListener(new OnTabChangeListener(){
        	@Override
        	public void onTabChanged(String tabId) {
        	    if(tabId.equals(TAB_FAV_TAG)) {
        	    	List<Item> items = fillListData(R.id.favoritelist);
        	        if (items.isEmpty())
            			Toast.makeText(FeedTabActivity.this, R.string.no_fav_msg, Toast.LENGTH_LONG).show();
        		} else if(tabId.equals(TAB_CHANNEL_TAG)) {
        	    	Feed currentTabFeed = mRepositoryController.getFeed(SharedPreferencesHelper.getPrefTabFeedId(FeedTabActivity.this));
        	    	if (currentTabFeed != null && outOfDate(currentTabFeed.getId()))
	        	    	refreshFeed(currentTabFeed,false);
        	    	else
        	    		fillListData(R.id.feedlist);
        	    }
        	}
        });
        
        ListView feedListView = (ListView)findViewById(R.id.feedlist);	
        ListView favoriteListView = (ListView)findViewById(R.id.favoritelist);
        
        registerForContextMenu(feedListView);
        registerForContextMenu(favoriteListView);
        
        feedListView.setOnItemClickListener(this);
        favoriteListView.setOnItemClickListener(this);
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mRepositoryController.close();
    }
    
    @Override
    protected void onResume () {
    	super.onResume();
    	if (getTabHost().getCurrentTabTag().equals(TAB_CHANNEL_TAG)) {
	    	Feed currentTabFeed = mRepositoryController.getFeed(SharedPreferencesHelper.getPrefTabFeedId(FeedTabActivity.this));
	    	if (currentTabFeed != null && outOfDate(currentTabFeed.getId()))
		    	refreshFeed(currentTabFeed,false);
	    	else
	    		fillListData(R.id.feedlist);
    	} else
    		fillData();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
        
    @Override
    public void onConfigurationChanged(Configuration newConfig){        
        super.onConfigurationChanged(newConfig);
    }
    
    private boolean outOfDate(long feedId) {
    	Date now = new Date();
    	Feed feed = mRepositoryController.getFeed(feedId);
    	long diffTime = 0;
    	long updatePeriod = SharedPreferencesHelper.getPrefUpdatePeriod(this) * 60 * 1000;

    	if (feed == null || updatePeriod < 0)
    		return false;
    	
    	if (feed.getRefresh() != null)
    		diffTime = now.getTime() - feed.getRefresh().getTime();
    	else
    		return true;
    	
		if (diffTime > updatePeriod)
			return true;
		else
			return false;
    }
    
    private void setTabs(String activeTab, String title) {
    	getTabHost().addTab(getTabHost().newTabSpec(TAB_CHANNEL_TAG).setIndicator(title).setContent(R.id.feedlist));
    	getTabHost().addTab(getTabHost().newTabSpec(TAB_FAV_TAG).setIndicator(getResources().getText(R.string.favorites),getResources().getDrawable(R.drawable.fav)).setContent(R.id.favoritelist));
    	getTabHost().setCurrentTabByTag(activeTab);
    }
    
    private void refreshFeed(Feed feed, boolean alwaysDisplayOfflineDialog) {
    	if (SharedPreferencesHelper.isOnline(this)) {
    		mIsOnline = true;
    		new UpdateFeedTask().execute(feed);
    	} else {
    		if (mIsOnline || alwaysDisplayOfflineDialog)
    			showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
    		mIsOnline = false;
    		fillListData(R.id.feedlist);
    	}
    }

    private List<Item> fillData() {
    	if (getTabHost().getCurrentTabTag().equals(TAB_FAV_TAG))
    		return fillListData(R.id.favoritelist);
    	else
    		return fillListData(R.id.feedlist);
    }
    
    private List<Item> fillListData(int listResource) {
		ListView feedListView = (ListView)findViewById(listResource);
		
		List<Item> items = null;
		if (listResource == R.id.favoritelist)
			items = mRepositoryController.getFavoriteItems(0);
		else {
			Feed currentFeed = mRepositoryController.getFeed(SharedPreferencesHelper.getPrefTabFeedId(this));
			if (currentFeed != null && currentFeed.getRefresh() != null) {
				CharSequence formattedUpdate = DateFormat.format(getResources().getText(R.string.update_format_pattern), currentFeed.getRefresh());
				getWindow().setTitle(getString(R.string.app_name) + " - " + formattedUpdate);
			}
        	items = mRepositoryController.getItems(SharedPreferencesHelper.getPrefTabFeedId(this), 1, SharedPreferencesHelper.getPrefMaxItems(this));
		}

		FeedArrayAdapter arrayAdapter = new FeedArrayAdapter(this, R.id.title, items);
		feedListView.setAdapter(arrayAdapter);
		return items;
    }
    
    public void onItemClick (AdapterView<?> parent, View v, int position, long id) {
	    	ContentValues values = new ContentValues();
	    	values.put(DbSchema.ItemSchema.COLUMN_READ, DbSchema.ON);
	    	mRepositoryController.updateItem(id, values, null);
	    	Intent intent = null;
	    	if (SharedPreferencesHelper.getItemView(this) == 0) {
	    		Item item = mRepositoryController.getItem(id);
	    		if (item.getDescription() == null && item.getContent() == null)
	    			intent = new Intent(this, FeedWebActivity.class);
	    		else
	    			intent = new Intent(this, FeedItemActivity.class);
	    	} else
	    		intent = new Intent(this, FeedWebActivity.class);
	    	
	        intent.putExtra(DbSchema.ItemSchema._ID, id);
	        startActivityForResult(intent, WEB_ACTIVITY_CODE);
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);

    	switch(requestCode) {
	    	case WEB_ACTIVITY_CODE:
	    	    if (resultCode == RESULT_OK)
	    	    	finish();
	    	    break;
	    	}   
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opt_tab_menu, menu);
        
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
        
        MenuItem preferencesMenuItem = (MenuItem) menu.findItem(R.id.menu_opt_preferences);
        preferencesMenuItem.setIntent(new Intent(this,FeedPreferenceActivity.class));
        
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_opt_refresh:
	        	if (getTabHost().getCurrentTabTag().equals(TAB_FAV_TAG)) {
	        		fillListData(R.id.favoritelist);
	        		Toast.makeText(this, R.string.no_new_fav_item_msg, Toast.LENGTH_LONG).show();
	        	} else if (getTabHost().getCurrentTabTag().equals(TAB_CHANNEL_TAG)) {
	        		Feed currentTabFeed = mRepositoryController.getFeed(SharedPreferencesHelper.getPrefTabFeedId(this));
			    	if (currentTabFeed != null)
				    	refreshFeed(currentTabFeed,true);
	        	}
	            return true;
	        case R.id.menu_opt_channels:
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
	        		finish();
	        		return true;
	        	}
	    }
    	
        return false;
    }
    
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	
		if (v.getId() == R.id.feedlist || v.getId() == R.id.favoritelist) {
			menu.setHeaderTitle (R.string.ctx_menu_title);
			MenuInflater inflater = getMenuInflater();
			AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Item item = mRepositoryController.getItem(acmi.id);
			if (item != null) {
				if (item.isFavorite())
		    		inflater.inflate(R.menu.ctx_menu_notfav, menu);
		    	else
		    		inflater.inflate(R.menu.ctx_menu_fav, menu);
			}
		}
    }

    
    public boolean onContextItemSelected(MenuItem menuItem) {
    	final AdapterContextMenuInfo acmi = (AdapterContextMenuInfo) menuItem.getMenuInfo();
    	Item item = mRepositoryController.getItem(acmi.id);
    	ContentValues values = null;
    	switch (menuItem.getItemId()) {
    		case R.id.add_fav:
    			values = new ContentValues();
    	    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.ON);
    	    	mRepositoryController.updateItem(acmi.id, values, null);
    			fillData();
    			Toast.makeText(this, R.string.add_fav_msg, Toast.LENGTH_SHORT).show();
    			return true;
    		case R.id.remove_fav:
    			Date now = new Date();
    			long diffTime = now.getTime() - item.getPubdate().getTime();
    			long maxTime = SharedPreferencesHelper.getPrefMaxHours(this) * 60 * 60 * 1000; // Max hours expressed in milliseconds
    			if (maxTime > 0 && diffTime > maxTime) {
	    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    			builder.setMessage(R.string.remove_fav_confirmation)
	    			       .setCancelable(false)
	    			       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	    			           public void onClick(DialogInterface dialog, int id) {
									ContentValues values = new ContentValues();
									values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.OFF);
									mRepositoryController.updateItem(acmi.id, values, null);
									fillData();
									Toast.makeText(FeedTabActivity.this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
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
        	    	mRepositoryController.updateItem(acmi.id, values, null);
        			fillData();
        			Toast.makeText(this, R.string.remove_fav_msg, Toast.LENGTH_SHORT).show();
    			}
    			return true;
    		case R.id.share:
    			item = mRepositoryController.getItem(acmi.id);
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
        	case SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS:
	            dialog = new ProgressDialog(this);
	            ((ProgressDialog)dialog).setTitle(getResources().getText(R.string.updating));
	            ((ProgressDialog)dialog).setMessage(getResources().getText(R.string.downloading));
	            ((ProgressDialog)dialog).setIndeterminate(true);
	            dialog.setCancelable(false);
	            break;
        	case SharedPreferencesHelper.DIALOG_ABOUT:
	        	title = getString(R.string.app_name) + " - " + getString(R.string.version) + " " + SharedPreferencesHelper.getVersionName(this);
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
    
    private class FeedArrayAdapter extends ArrayAdapter<Item> {

    	public FeedArrayAdapter(Context context, int textViewResourceId, List<Item> objects) {
    		super(context, textViewResourceId, objects);
    	}
    	
    	@Override
    	public long getItemId(int position) {
    		return getItem(position).getId();
    	}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	int[] item_rows = {R.layout.channel_item_row_notselected_notfavorite, R.layout.channel_item_row_selected_notfavorite,R.layout.channel_item_row_notselected_favorite,R.layout.channel_item_row_selected_favorite,R.layout.fav_item_row_notselected_favorite,R.layout.fav_item_row_selected_favorite,};
        	int item_row = item_rows[0];
        	
        	Item item = getItem(position);
            
        	View view = convertView;
	            LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
	            if (item.isRead())
	            	if (item.isFavorite())
	            		if (getTabHost().getCurrentTabTag().equals(TAB_FAV_TAG))
	            			item_row = item_rows[5];
	            		else
	            			item_row = item_rows[3];
	            	else
	            		item_row = item_rows[1];
	            else
	            	if (item.isFavorite())
	            		if (getTabHost().getCurrentTabTag().equals(TAB_FAV_TAG))
	            			item_row = item_rows[4];
	            		else
	            			item_row = item_rows[2];
	            	else
	            		item_row = item_rows[0];
	            view = li.inflate(item_row, null);
            TextView titleView = (TextView) view.findViewById(R.id.title);
            TextView channelView = (TextView) view.findViewById(R.id.channel);
            TextView pubdateView = (TextView) view.findViewById(R.id.pubdate);
            if (titleView != null)
            	titleView.setText(item.getTitle());
            if (channelView != null) {
            	Feed feed = mRepositoryController.getFeed(mRepositoryController.getItemFeedId(item.getId()));
            	if (feed != null)
            		channelView.setText(feed.getTitle());
            } if (pubdateView != null) {
            	CharSequence formattedPubdate = DateFormat.format(getResources().getText(R.string.pubdate_format_pattern), item.getPubdate());
            	pubdateView.setText(formattedPubdate);
            }
            
            return view;
        }
    }
    
    private class UpdateFeedTask extends AsyncTask<Feed, Void, Boolean> {
    	private long feedId = -1;
    	private long lastItemIdBeforeUpdate = -1;
    	
    	public UpdateFeedTask() {
    		super();
    	}
    	
        protected Boolean doInBackground(Feed...params) {
        	feedId = params[0].getId();
        	Item lastItem = mRepositoryController.getLastItem(feedId);
        	if (lastItem != null)
        		lastItemIdBeforeUpdate = lastItem.getId();
        	
        	FeedController feedController = new FeedController(FeedTabActivity.this);
        	
        	try {
	        	Feed handledFeed = feedController.handleFeed(params[0].getURL());
	        	handledFeed.setId(feedId);
	        	
	        	mRepositoryController.updateFeed(handledFeed);
	        	mRepositoryController.cleanDbItems(feedId);
	
	        	SharedPreferencesHelper.setPrefTabFeedId(FeedTabActivity.this,feedId);
	        	
        	} catch (IOException ioe) {
        		Log.e(LOG_TAG,"",ioe);
        		return new Boolean(false);
            } catch (SAXException se) {
            	Log.e(LOG_TAG,"",se);
            	return new Boolean(false);
            } catch (ParserConfigurationException pce) {
            	Log.e(LOG_TAG,"",pce);
            	return new Boolean(false);
            }
            
            return new Boolean(true);
        }
        
        protected void onPreExecute() {
        	showDialog(SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS);
        }

        protected void onPostExecute(Boolean result) {
        	fillListData(R.id.feedlist);
        	dismissDialog(SharedPreferencesHelper.DIALOG_UPDATE_PROGRESS);
        	
        	long lastItemIdAfterUpdate = -1;
        	Item lastItem = mRepositoryController.getLastItem(feedId);
        	if (lastItem != null)
        		lastItemIdAfterUpdate = lastItem.getId();
        	if (lastItemIdAfterUpdate > lastItemIdBeforeUpdate)
        		Toast.makeText(FeedTabActivity.this, R.string.new_item_msg, Toast.LENGTH_LONG).show();
        	else
        		Toast.makeText(FeedTabActivity.this, R.string.no_new_item_msg, Toast.LENGTH_LONG).show();
        }
    }
}
package org.sliit.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import org.sliit.R;
import org.sliit.exception.FeedException;
import org.sliit.domain.Enclosure;
import org.sliit.domain.Feed;
import org.sliit.domain.Item;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public class RepositoryController {
	
	private static final String LOG_TAG = "RepositoryController";
	
	private final Context mCtx;
	private DbHelper mDbHelper;
	private SQLiteDatabase mDb;
    
	private static class DbHelper extends SQLiteOpenHelper {
		private static final String LOG_TAG = "DbHelper";
    	private RepositoryController mDbfa;
        
        DbHelper(RepositoryController dbfa) {
          super(dbfa.mCtx, DbSchema.DATABASE_NAME, null, DbSchema.DATABASE_VERSION);
          mDbfa = dbfa;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DbSchema.FeedSchema.CREATE_TABLE);
            try {
                List<Feed> resourceFeeds = getOPMLResourceFeeds();
                populateFeeds(db, resourceFeeds);
                List<Feed> userFeeds = getOPMLUserFeeds();
                populateFeeds(db, userFeeds);
            } catch(XmlPullParserException xppe) {
                Log.e(LOG_TAG,"",xppe);
            } catch (MalformedURLException mue) {
                Log.e(LOG_TAG,"",mue);
            } catch (IOException ioe) {
                Log.e(LOG_TAG,"",ioe);
            }
            db.execSQL(DbSchema.ItemSchema.CREATE_TABLE);
            db.execSQL(DbSchema.EnclosureSchema.CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion <= 7) {
            	Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
            	String alter_table = "ALTER TABLE " + DbSchema.FeedSchema.TABLE_NAME + " ADD " + DbSchema.FeedSchema.COLUMN_HOMEPAGE + " TEXT;";
            	db.execSQL(alter_table);
            	alter_table = "ALTER TABLE " + DbSchema.ItemSchema.TABLE_NAME + " ADD " + DbSchema.ItemSchema.COLUMN_CONTENT + " TEXT;";
            	db.execSQL(alter_table);
            	db.execSQL(DbSchema.EnclosureSchema.CREATE_TABLE);
            	
            	long feedId = -1;
            	Cursor feedCursor = null;
            	long itemId = -1;
            	String itemDescription = null;
            	String itemContent = null;
            	Cursor itemCursor = null;
            	ContentValues values = null;
            	
            	feedCursor = db.query(DbSchema.FeedSchema.TABLE_NAME, new String[]{DbSchema.FeedSchema._ID}, null, null, null, null, DbSchema.FeedSchema._ID + DbSchema.SORT_ASC);
            	feedCursor.moveToFirst();
        		while (!feedCursor.isAfterLast()) {
        			feedId = feedCursor.getLong(feedCursor.getColumnIndex(DbSchema.FeedSchema._ID));
        			itemCursor = db.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?",new String[]{Long.toString(feedId)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_ASC);
        			itemCursor.moveToFirst();
        			
        			while (!itemCursor.isAfterLast()) {
        				itemId = itemCursor.getLong(itemCursor.getColumnIndex(DbSchema.ItemSchema._ID));
        				if (!itemCursor.isNull(itemCursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_DESCRIPTION)))
        					itemDescription = itemCursor.getString(itemCursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_DESCRIPTION));
        				
        				if (itemDescription != null) {
		                	SpannableStringBuilder spannedStr = (SpannableStringBuilder)Html.fromHtml(itemDescription.toString().trim());
			        		Object[] spannedObjects = spannedStr.getSpans(0,spannedStr.length(),Object.class);
			        		for (int i = 0; i < spannedObjects.length; i++) {
			        			if (spannedObjects[i] instanceof ImageSpan)
			        				spannedStr.replace(spannedStr.getSpanStart(spannedObjects[i]), spannedStr.getSpanEnd(spannedObjects[i]), "");
			        		}
			        		
			        		itemDescription = spannedStr.toString().trim() + System.getProperty("line.separator");
			        		itemContent = spannedStr.toString().trim() + System.getProperty("line.separator");
			        		
			        		values = new ContentValues();
			                values.put(DbSchema.ItemSchema.COLUMN_DESCRIPTION, itemDescription);
			                values.put(DbSchema.ItemSchema.COLUMN_CONTENT, itemContent);
			                
			        		db.update(DbSchema.ItemSchema.TABLE_NAME, values, DbSchema.ItemSchema._ID + "=?", new String[]{Long.toString(itemId)});
			        		
			        		itemCursor.moveToNext();
        				}
        			}
        			
        			if (itemCursor != null)
            			itemCursor.close();
        			
        			feedCursor.moveToNext();
        		}
        		
        		if (feedCursor != null)
        			feedCursor.close();
            }

            try {
                List<Feed> resourceFeeds = getOPMLResourceFeeds();
                populateFeeds(db, resourceFeeds);
            } catch(XmlPullParserException xppe) {
                Log.e(LOG_TAG,"",xppe);
            } catch (MalformedURLException mue) {
                Log.e(LOG_TAG,"",mue);
            } catch (IOException ioe) {
                Log.e(LOG_TAG,"",ioe);
            }
        }
        
        private List<Feed> getOPMLResourceFeeds() throws XmlPullParserException, MalformedURLException, IOException {
        	List<Feed> feeds = new ArrayList<Feed>();
        	Feed feed;

            XmlResourceParser parser = mDbfa.mCtx.getResources().getXml(R.xml.feeds);
            
            int eventType = -1;
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if(eventType == XmlResourceParser.START_TAG) {
                    String tagName = parser.getName();
                    if (tagName.equals("outline") && parser.getAttributeCount() >= 3) {
                        feed = new Feed();
                        feed.setTitle(parser.getAttributeValue(null,"title"));
                        feed.setURL(new URL(parser.getAttributeValue(null,"xmlUrl")));
                        feed.setHomePage(new URL(parser.getAttributeValue(null,"htmlUrl")));
                        feed.setType(parser.getAttributeValue(null,"type"));
                        feed.setEnabled(DbSchema.ON);
                        feeds.add(feed);
                    }
                }
                eventType = parser.next();
            }
            parser.close();
            return feeds;
        }
        
        private List<Feed> getOPMLUserFeeds() {
            // Not yet implemented
            return null;
        }
        
        private void populateFeeds(SQLiteDatabase db, List<Feed> feeds) {
            if (feeds != null) {
                Iterator<Feed> feedsIterator = feeds.iterator();
                Feed feed = null;
                long feedId = -1;
                boolean populated = false;
                while (feedsIterator.hasNext()) {
	                feed = feedsIterator.next();
	                feedId = hasFeed(db,feed);
	                
	                if (feedId == -1)
	                	populated = insertFeed(db, feed);               
	                else {
                        feed.setId(feedId);
                        populated = updateFeed(db, feed);
	                }
	                
	                if (!populated)
	                    Log.e(LOG_TAG, "Feed with title '"+feed.getTitle()+"' cannot be populated into the database. Feed URL: " + feed.getURL().toString());
                }
            }
        }
        
        private boolean insertFeed(SQLiteDatabase db, Feed feed) {
            boolean inserted = false;
            ContentValues values = mDbfa.getContentValues(feed);
            inserted = (db.insert(DbSchema.FeedSchema.TABLE_NAME, null, values) != -1);     
            return inserted;
        }
        
        private boolean updateFeed(SQLiteDatabase db, Feed feed) {
            boolean updated = false;
            ContentValues values = mDbfa.getContentValues(feed);
            updated = (db.update(DbSchema.FeedSchema.TABLE_NAME, values, DbSchema.FeedSchema._ID + "=?", new String[]{Long.toString(feed.getId())}) > 0);
            return updated;
        }
        
        // check if feed URL already exists in the DB
        // if exists, returns feed id
        // if does not exist, returns -1
        private long hasFeed(SQLiteDatabase db, Feed feed) {
            long feedId = -1;
            Cursor cursor = db.query(DbSchema.FeedSchema.TABLE_NAME, null, DbSchema.FeedSchema.COLUMN_URL + "=?", new String[]{feed.getURL().toString()}, null, null, null);
            if (cursor.moveToFirst())
                    feedId = cursor.getLong(cursor.getColumnIndex(DbSchema.FeedSchema._ID));
            
            if (cursor != null)
                    cursor.close();
            
            return feedId;
        }
    }
    
	public RepositoryController(Context ctx) {
	    this.mCtx = ctx;
	}

    public RepositoryController open() {
        mDbHelper = new DbHelper(this);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    	    
    public void close() {
        mDbHelper.close();
    }
    
    public void cleanDbItems(long feedId) {
    	List<Item> items;
    	ListIterator<Item> itemListIterator;
    	int cleanNbr = 0;

    	// Remove from db old items (not favorite) in order to limit the number of not favorite items to max download preference
		items = getNotFavoriteItems(feedId, 0);
		cleanNbr = items.size() - SharedPreferencesHelper.getPrefMaxDownload(mCtx);
		if (cleanNbr > 0) {
			itemListIterator = items.listIterator();
			while (itemListIterator.hasNext() && (itemListIterator.nextIndex() < cleanNbr)) {
				removeItem(itemListIterator.next().getId());
			}
		}
		
		// Remove from db expired items (not favorite) older than max hours
		items = getNotFavoriteItems(feedId, 0);
		//Item mostRecentItem = getLastItem(feedId);
		itemListIterator = items.listIterator();
		Item nextItem = null;
		long diffTime = 0;
		long maxTime = 0;
		while (itemListIterator.hasNext()) {
			nextItem = itemListIterator.next();
			//diffTime = mostRecentItem.getPubdate().getTime() - nextItem.getPubdate().getTime();
			Date now = new Date();
			diffTime = now.getTime() - nextItem.getPubdate().getTime();
			maxTime = SharedPreferencesHelper.getPrefMaxHours(mCtx) * 60 * 60 * 1000; // Max hours expressed in milliseconds
			// check if item is out of date
			if (maxTime > 0 && diffTime > maxTime)
				removeItem(nextItem.getId());
		}
    }
    
    public List<Feed> getFeeds() {
		List<Feed> feeds = new ArrayList<Feed>();
		Cursor cursor = mDb.query(DbSchema.FeedSchema.TABLE_NAME, new String[]{DbSchema.FeedSchema._ID}, null, null, null, null, DbSchema.FeedSchema._ID + DbSchema.SORT_ASC);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Feed feed = getFeed(cursor.getLong(cursor.getColumnIndex(DbSchema.FeedSchema._ID)));
			if (feed != null)
				feeds.add(feed);
			cursor.moveToNext();
		}
		
		if (cursor != null)
			cursor.close();
		
		return feeds;
    }
  
	public Feed getFeed(long id) {
		Feed feed = null;
		Cursor cursor = null;
		try {
			cursor = mDb.query(DbSchema.FeedSchema.TABLE_NAME, null, DbSchema.FeedSchema._ID + "=?", new String[]{Long.toString(id)}, null, null, null);
			if (cursor.moveToFirst()) {
				feed = new Feed();
				while (!cursor.isAfterLast()) {
					feed.setId(cursor.getLong(cursor.getColumnIndex(DbSchema.FeedSchema._ID)));
					feed.setURL(new URL(cursor.getString(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_URL))));
					feed.setHomePage(new URL(cursor.getString(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_HOMEPAGE))));
					feed.setTitle(cursor.getString(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_TITLE)));
					if (!cursor.isNull(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_TYPE)))
						feed.setType(cursor.getString(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_TYPE)));
					if (!cursor.isNull(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_REFRESH)))
						feed.setRefresh(new Date(cursor.getLong(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_REFRESH))));
					//Calendar.getInstance().setTimeInMillis(cursor.getInt(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_REFRESH)));
					//feed.setRefresh(Calendar.getInstance().getTime();
					feed.setEnabled(cursor.getInt(cursor.getColumnIndex(DbSchema.FeedSchema.COLUMN_ENABLE)));
					feed.setItems(getItems(id, 1, -1));
					cursor.moveToNext();
				}
			} else {
				throw new FeedException("Feed with id " + id + " not found in the database.");
			}
		} catch (FeedException fe) {
			Log.e(LOG_TAG,"",fe);
    	} catch (MalformedURLException mue) {
			Log.e(LOG_TAG,"",mue);
		}
		
		if (cursor != null)
			cursor.close();
		
		return feed;
	}
	
    public ContentValues getContentValues(Feed feed) {
    	ContentValues values = new ContentValues();
        values.put(DbSchema.FeedSchema.COLUMN_URL, feed.getURL().toString());
        if (feed.getHomePage() == null)
        	values.putNull(DbSchema.FeedSchema.COLUMN_HOMEPAGE);
        else
        	values.put(DbSchema.FeedSchema.COLUMN_HOMEPAGE, feed.getHomePage().toString());
        if (feed.getTitle() == null)
        	values.putNull(DbSchema.FeedSchema.COLUMN_TITLE);
        else
        	values.put(DbSchema.FeedSchema.COLUMN_TITLE, feed.getTitle());
        if (feed.getType() == null)
        	values.putNull(DbSchema.FeedSchema.COLUMN_TYPE);
        else
        	values.put(DbSchema.FeedSchema.COLUMN_TYPE, feed.getType());
		if (feed.getRefresh() == null)
    		values.putNull(DbSchema.FeedSchema.COLUMN_REFRESH);
    	else
    		values.put(DbSchema.FeedSchema.COLUMN_REFRESH, feed.getRefresh().getTime());  
    	int state = DbSchema.ON;
    	if (!feed.isEnabled())
    		state = DbSchema.OFF;
    	values.put(DbSchema.FeedSchema.COLUMN_ENABLE, state);
    	
    	return values;
    }
    
    public long addFeed(Feed feed) {
        return addFeed(getContentValues(feed),feed.getItems());
    }
    
    public long addFeed(ContentValues values, List<Item> items) {
    	long feedId = mDb.insert(DbSchema.FeedSchema.TABLE_NAME, null, values);
        if (feedId == -1)
        	Log.e(LOG_TAG, "Feed '" + values.getAsString(DbSchema.ItemSchema.COLUMN_TITLE) + "' could not be inserted into the database. Feed values: " + values.toString());
        
        if (items != null && feedId != -1) {
    		Iterator<Item> iterator = items.iterator();
    		while (iterator.hasNext()) {
    			addItem(feedId,iterator.next());
    		}
    	}
        
        return feedId;
    }
    
    public ContentValues getUpdateContentValues(Feed feed) {
    	ContentValues values = new ContentValues();

		if (feed.getRefresh() == null)
    		values.putNull(DbSchema.FeedSchema.COLUMN_REFRESH);
    	else
    		values.put(DbSchema.FeedSchema.COLUMN_REFRESH, feed.getRefresh().getTime());  	
    	return values;
    }
    
    public boolean updateFeed(Feed feed) {  	
    	return updateFeed(feed.getId(), getUpdateContentValues(feed), feed.getItems());
    }
    
    public boolean updateFeed(long id, ContentValues values, List<Item> items) {
    	boolean feedUpdated = (mDb.update(DbSchema.FeedSchema.TABLE_NAME, values, DbSchema.FeedSchema._ID + "=?", new String[]{Long.toString(id)}) > 0);
    	
    	if (feedUpdated && items != null) {
    		Item firstDbItem = getFirstItem(id);
    		Iterator<Item> iterator = items.listIterator();
    		Item item = null;
    		while (iterator.hasNext()) {
    			item = iterator.next();
    			if (!hasItem(id,item)) {
    				if (firstDbItem == null)
    					addItem(id, item); // Db is empty
    				else {
    					if (item.getPubdate().after(firstDbItem.getPubdate()))
    						addItem(id, item);
    				}
    			}
    		}
    	}
    	
    	return feedUpdated;
    }
    
    public boolean hasItem(long feedId, Item item) {
    	Cursor cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=? AND (" + DbSchema.ItemSchema.COLUMN_LINK + "=? OR " + DbSchema.ItemSchema.COLUMN_GUID + "=? OR " + DbSchema.ItemSchema.COLUMN_TITLE + "=?)", new String[]{Long.toString(feedId),item.getLink().toString(), item.getGuid(), item.getTitle()}, null, null, null);
    	boolean hasItem = cursor.moveToFirst();
    	
    	if (cursor != null)
			cursor.close();
    	
    	return hasItem;
    }
    
    
    public boolean removeFeed(Feed feed) {
    	return removeFeed(feed.getId());
    }
    
    public boolean removeFeed(long id) {
    	removeItems(id);
    	return (mDb.delete(DbSchema.FeedSchema.TABLE_NAME, DbSchema.FeedSchema._ID + "=?", new String[]{Long.toString(id)}) > 0);
    }
    
	public long getItemFeedId(long itemId) {
		long feedId = -1;
		Cursor cursor = null;
		try {
			cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema._ID + "=?", new String[]{Long.toString(itemId)}, null, null, null);
			if (cursor.moveToFirst()) {
				while (!cursor.isAfterLast()) {
					feedId = cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_FEED_ID));
					cursor.moveToNext();
				}
			} else {
				throw new FeedException("Feed id for item id " + itemId + " not found in the database.");
			}
		} catch (FeedException fe) {
			Log.e(LOG_TAG,"",fe);
    	}
    	
    	if (cursor != null)
    		cursor.close();
    	
		return feedId;
	}
    
    public Item getItem(long id) {
		Item item = null;
		Cursor cursor = null;
		try {
			cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema._ID + "=?", new String[]{Long.toString(id)}, null, null, null);
			if (cursor.moveToFirst()) {
				item = new Item();
				while (!cursor.isAfterLast()) {
					item.setId(cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID)));
					item.setLink(new URL(cursor.getString(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_LINK))));
					item.setGuid(cursor.getString(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_GUID)));
					item.setTitle(cursor.getString(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_TITLE)));
					if (!cursor.isNull(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_DESCRIPTION)))
						item.setDescription(cursor.getString(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_DESCRIPTION)));
					if (!cursor.isNull(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_CONTENT)))
						item.setContent(cursor.getString(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_CONTENT)));
					if (!cursor.isNull(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_IMAGE)))
						item.setImage(new URL(cursor.getString(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_IMAGE))));
					item.setPubdate(new Date(cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_PUBDATE))));
					item.setFavorite(cursor.getInt(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_FAVORITE)));
					item.setRead(cursor.getInt(cursor.getColumnIndex(DbSchema.ItemSchema.COLUMN_READ)));
					item.setEnclosures(getEnclosures(id, 1, -1));
					cursor.moveToNext();
				}
			} else {
				throw new FeedException("Item with id " + id + " not found in the database.");
			}
		} catch (FeedException fe) {
			Log.e(LOG_TAG,"",fe);
    	} catch (MalformedURLException mue) {
			Log.e(LOG_TAG,"",mue);
		}
    	
    	if (cursor != null)
    		cursor.close();
    	
		return item;
	}
	
    public Item getFirstItem(long feedId) {
        Cursor cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?",new String[]{Long.toString(feedId)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_ASC, "1");
        boolean hasItem = cursor.moveToFirst();
        Item firstItem = null;
        if (hasItem)
        	firstItem = getItem(cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID)));
        
        if (cursor != null)
        	cursor.close();
        
		return firstItem;
    }
    
    public Item getLastItem(long feedId) {
        Cursor cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?",new String[]{Long.toString(feedId)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_DESC, "1");
        boolean hasItem = cursor.moveToFirst();
        Item lastItem = null;
        if (hasItem)
        	lastItem = getItem(cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID)));
        
        if (cursor != null)
        	cursor.close();
        
		return lastItem;
    }
    
    public long getNextItemId(long feedId, long currentItemId) {
    	long itemId = -1;
    	boolean isCurrentItem = false;
    	boolean nextItemFound = false;
    	long nextItemId = -1;
    	Cursor cursor = null;
    	
    	try {
    		cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?",new String[]{Long.toString(feedId)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_ASC);
			if (cursor.moveToFirst()) {
				while (!cursor.isAfterLast() && !nextItemFound) {
					itemId = cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID));
					
					if (isCurrentItem) {
						nextItemId = itemId;
						nextItemFound = true;
					}
					
					if (itemId == currentItemId)
						isCurrentItem = true;
					else
						isCurrentItem = false;

					cursor.moveToNext();
				}
			} else {
				throw new FeedException("Feed id " + feedId + " not found in the database.");
			}
		} catch (FeedException fe) {
			Log.e(LOG_TAG,"",fe);
    	}
    	
    	if (cursor != null)
    		cursor.close();

    	return nextItemId;
    }
    
    public long getPreviousItemId(long feedId, long currentItemId) {
    	long itemId = -1;
    	boolean isCurrentItem = false;
    	boolean previousItemFound = false;
    	long previousItemId = -1;
    	Cursor cursor = null;
    	
    	try {
    		cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?",new String[]{Long.toString(feedId)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_ASC);
			if (cursor.moveToLast()) {
				while (!cursor.isBeforeFirst() && !previousItemFound) {
					itemId = cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID));
					
					if (isCurrentItem) {
						previousItemId = itemId;
						previousItemFound = true;
					}
					
					if (itemId == currentItemId)
						isCurrentItem = true;
					else
						isCurrentItem = false;

					cursor.moveToPrevious();
				}
			} else {
				throw new FeedException("Feed id " + feedId + " not found in the database.");
			}
		} catch (FeedException fe) {
			Log.e(LOG_TAG,"",fe);
    	}
    	
    	if (cursor != null)
    		cursor.close();

    	return previousItemId;
    }

    public List<Item> getItems(long feedId, int sort, int maxItems) {
    	List<Item> items = new ArrayList<Item>();

    	Cursor cursor = null;
    	if (maxItems > 0)
        	cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?",new String[]{Long.toString(feedId)}, null, null, DbSchema.ItemSchema._ID + DbSchema.ORDERS[sort], Integer.toString(maxItems));
    	else
    		cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?",new String[]{Long.toString(feedId)}, null, null, DbSchema.ItemSchema._ID + DbSchema.ORDERS[sort]);
        
        cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Item item = getItem(cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID)));
			if (item != null)
				items.add(item);
			cursor.moveToNext();
		}
		
		if (cursor != null)
			cursor.close();
		
		return items;
    }


    public List<Item> getNotFavoriteItems(long feedId, int maxItems) {
    	List<Item> items = new ArrayList<Item>();
    	
        Cursor cursor = null;
        if (maxItems > 0)
        	cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=? AND " + DbSchema.ItemSchema.COLUMN_FAVORITE + " =?",new String[]{Long.toString(feedId), Integer.toString(DbSchema.OFF)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_ASC, Integer.toString(maxItems));
        else
        	cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=? AND " + DbSchema.ItemSchema.COLUMN_FAVORITE + " =?",new String[]{Long.toString(feedId), Integer.toString(DbSchema.OFF)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_ASC);
        
        cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Item item = getItem(cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID)));
			if (item != null)
				items.add(item);
			cursor.moveToNext();
		}
		
		if (cursor != null)
			cursor.close();
		
		return items;
    }

    
    public List<Item> getFavoriteItems(int maxItems) {
    	List<Item> items = new ArrayList<Item>();
    	
        Cursor cursor = null;
        if (maxItems > 0)
        	cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FAVORITE + " =?",new String[]{Integer.toString(DbSchema.ON)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_DESC, Integer.toString(maxItems));
        else
        	cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FAVORITE + " =?",new String[]{Integer.toString(DbSchema.ON)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_DESC);
        
        cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Item item = getItem(cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID)));
			if (item != null)
				items.add(item);
			cursor.moveToNext();
		}
		
		if (cursor != null)
			cursor.close();
		
		return items;
    }
 
/*
    public List<Item> getFavoriteItems(long feedId, int maxItems) {
    	List<Item> items = new ArrayList<Item>();
    	
        Cursor cursor = null;
        if (maxItems > 0)
        	cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=? AND " + DbSchema.ItemSchema.COLUMN_FAVORITE + " =?",new String[]{Long.toString(feedId),Integer.toString(DbSchema.ON)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_DESC, Integer.toString(maxItems));
        else
        	cursor = mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=? AND " + DbSchema.ItemSchema.COLUMN_FAVORITE + " =?",new String[]{Long.toString(feedId),Integer.toString(DbSchema.ON)}, null, null, DbSchema.ItemSchema._ID + DbSchema.SORT_DESC);	
        
        cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Item item = getItem(cursor.getLong(cursor.getColumnIndex(DbSchema.ItemSchema._ID)));
			if (item != null)
				items.add(item);
			cursor.moveToNext();
		}
		
		if (cursor != null)
			cursor.close();
		
		return items;
    }
    

    public Cursor getItemsCursor(long feedId) {
    	SharedPreferences prefs = mCtx.getSharedPreferences(DefaultSharedPreferences.PREFS_FILE_NAME, Context.MODE_PRIVATE);
    	int limit = prefs.getInt("maxItemsPerFeed", DefaultSharedPreferences.DEFAULT_MAX_ITEMS_PER_FEED);
        return mDb.query(DbSchema.ItemSchema.TABLE_NAME, null, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?",new String[]{Long.toString(feedId)}, null, null, DbSchema.ItemSchema._ID + DbSchema.ORDER[1], Integer.toString(limit));
    }
*/
    
    private ContentValues getContentValues(long feedId, Item item) {
    	ContentValues values = new ContentValues();
        values.put(DbSchema.ItemSchema.COLUMN_FEED_ID, feedId);
        values.put(DbSchema.ItemSchema.COLUMN_LINK, item.getLink().toString());
        values.put(DbSchema.ItemSchema.COLUMN_GUID, item.getGuid());
        values.put(DbSchema.ItemSchema.COLUMN_TITLE, item.getTitle());
        if (item.getDescription() == null)
        	values.putNull(DbSchema.ItemSchema.COLUMN_DESCRIPTION);
        else
        	values.put(DbSchema.ItemSchema.COLUMN_DESCRIPTION, item.getDescription());
        if (item.getContent() == null)
        	values.putNull(DbSchema.ItemSchema.COLUMN_CONTENT);
        else
        	values.put(DbSchema.ItemSchema.COLUMN_CONTENT, item.getContent());
    	if (item.getImage() == null)
    		values.putNull(DbSchema.ItemSchema.COLUMN_IMAGE);
    	else
    		values.put(DbSchema.ItemSchema.COLUMN_IMAGE, item.getImage().toString()); 
    	values.put(DbSchema.ItemSchema.COLUMN_PUBDATE, item.getPubdate().getTime());
    	int state = DbSchema.ON;
    	if (!item.isFavorite())
    		state = DbSchema.OFF;
    	values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, state);
    	if (!item.isRead())
    		state = DbSchema.OFF;
    	else
    		state = DbSchema.ON;
    	values.put(DbSchema.ItemSchema.COLUMN_READ, state);
    	return values;
    }
    
    public long addItem(long feedId, Item item) {
    	return addItem(feedId,getContentValues(feedId, item), item.getEnclosures());
    }
    
    public long addItem(long feedId, ContentValues values, List<Enclosure> enclosures) {
    	long itemId = mDb.insert(DbSchema.ItemSchema.TABLE_NAME, null, values);
    	if (itemId == -1)
    		Log.e(LOG_TAG,"Item '" + values.getAsString(DbSchema.ItemSchema.COLUMN_TITLE) + "' for Feed id " + values.getAsLong(DbSchema.ItemSchema.COLUMN_FEED_ID) + " could not be inserted into the database. Item values: " + values.toString());
    	
    	 if (enclosures != null && itemId != -1) {
     		Iterator<Enclosure> iterator = enclosures.iterator();
     		while (iterator.hasNext()) {
     			addEnclosure(itemId,iterator.next());
     		}
     	}
    	
    	return itemId;
    }

    public boolean updateItem(long feedId, Item item) {
    	return updateItem(item.getId(), getContentValues(feedId, item), item.getEnclosures());
    }
    
    public boolean updateItem(long id, ContentValues values, List<Enclosure> enclosures) {
    	boolean itemUpdated = (mDb.update(DbSchema.ItemSchema.TABLE_NAME, values, DbSchema.ItemSchema._ID + "=?", new String[]{Long.toString(id)}) > 0);
    	
    	if (itemUpdated && enclosures != null) {
    		Iterator<Enclosure> iterator = enclosures.listIterator();
    		Enclosure enclosure = null;
    		while (iterator.hasNext()) {
    			enclosure = iterator.next();
    			updateEnclosure(values.getAsLong(DbSchema.ItemSchema._ID), enclosure);
    		}
    	}
    	
    	return itemUpdated;
    }
    
    public boolean removeItems(long feedId) {
    	List<Item> items = getItems(feedId, 1, -1);
    	return removeItems(items);
    	//return (mDb.delete(DbSchema.ItemSchema.TABLE_NAME, DbSchema.ItemSchema.COLUMN_FEED_ID + "=?", new String[]{Long.toString(feedId)}) > 0);
    }
    
    public boolean removeItems(List<Item> items) {
    	boolean allItemRemoved = true;
    	Item item;
    	Iterator<Item> iterator = items.iterator();
    	while (iterator.hasNext()) {
    		item = iterator.next();
    		if (!removeItem(item.getId()))
    			allItemRemoved = false;
    	}
    	return allItemRemoved;
    }
    
    public boolean removeItem(long id) {
    	removeEnclosures(id);
    	return (mDb.delete(DbSchema.ItemSchema.TABLE_NAME, DbSchema.ItemSchema._ID + "=?", new String[]{Long.toString(id)}) > 0);
    }
    
    public List<Enclosure> getEnclosures(long itemId, int sort, int maxItems) {
    	List<Enclosure> enclosures = new ArrayList<Enclosure>();

    	Cursor cursor = null;
    	if (maxItems > 0)
        	cursor = mDb.query(DbSchema.EnclosureSchema.TABLE_NAME, null, DbSchema.EnclosureSchema.COLUMN_ITEM_ID + "=?",new String[]{Long.toString(itemId)}, null, null, DbSchema.EnclosureSchema._ID + DbSchema.ORDERS[sort], Integer.toString(maxItems));
    	else
    		cursor = mDb.query(DbSchema.EnclosureSchema.TABLE_NAME, null, DbSchema.EnclosureSchema.COLUMN_ITEM_ID + "=?",new String[]{Long.toString(itemId)}, null, null, DbSchema.EnclosureSchema._ID + DbSchema.ORDERS[sort]);
        
        cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Enclosure enclosure = getEnclosure(cursor.getLong(cursor.getColumnIndex(DbSchema.EnclosureSchema._ID)));
			if (enclosure != null)
				enclosures.add(enclosure);
			cursor.moveToNext();
		}
		
		if (cursor != null)
			cursor.close();
		
		return enclosures;
    }
    
    public Enclosure getEnclosure(long id) {
    	Enclosure enclosure = null;
		Cursor cursor = null;
		try {
			cursor = mDb.query(DbSchema.EnclosureSchema.TABLE_NAME, null, DbSchema.EnclosureSchema._ID + "=?", new String[]{Long.toString(id)}, null, null, null);
			if (cursor.moveToFirst()) {
				enclosure = new Enclosure();
				while (!cursor.isAfterLast()) {
					enclosure.setId(cursor.getLong(cursor.getColumnIndex(DbSchema.EnclosureSchema._ID)));
					enclosure.setMime(cursor.getString(cursor.getColumnIndex(DbSchema.EnclosureSchema.COLUMN_MIME)));
					enclosure.setURL(new URL(cursor.getString(cursor.getColumnIndex(DbSchema.EnclosureSchema.COLUMN_URL))));
					cursor.moveToNext();
				}
			} else {
				throw new FeedException("Enclosure with id " + id + " not found in the database.");
			}
		} catch (FeedException fe) {
			Log.e(LOG_TAG,"",fe);
    	} catch (MalformedURLException mue) {
			Log.e(LOG_TAG,"",mue);
		}
    	
    	if (cursor != null)
    		cursor.close();
    	
		return enclosure;
	}
    
    public ContentValues getContentValues(long itemId, Enclosure enclosure) {
    	ContentValues values = new ContentValues();    	
    	values.put(DbSchema.EnclosureSchema.COLUMN_ITEM_ID, itemId);
        values.put(DbSchema.EnclosureSchema.COLUMN_MIME, enclosure.getMime());
        values.put(DbSchema.EnclosureSchema.COLUMN_URL, enclosure.getURL().toString());
    	return values;
    }
    
    public long addEnclosure(long itemId, Enclosure enclosure) {
    	return addEnclosure(itemId,getContentValues(itemId, enclosure));
    }
    
    public long addEnclosure(long itemId, ContentValues values) {
    	long enclosureId = mDb.insert(DbSchema.EnclosureSchema.TABLE_NAME, null, values);
    	if (enclosureId == -1)
    		Log.e(LOG_TAG,"Enclosure '" + values.getAsString(DbSchema.EnclosureSchema.COLUMN_URL) + "' for Item id " + values.getAsLong(DbSchema.EnclosureSchema.COLUMN_ITEM_ID) + " could not be inserted into the database. Enclosure values: " + values.toString());
    	return enclosureId;
    }
    
    public boolean updateEnclosure(long itemId, Enclosure enclosure) {
    	return updateEnclosure(enclosure.getId(), getContentValues(itemId, enclosure));
    }
    
    public boolean updateEnclosure(long id, ContentValues values) {
    	return (mDb.update(DbSchema.EnclosureSchema.TABLE_NAME, values, DbSchema.EnclosureSchema._ID + "=?", new String[]{Long.toString(id)}) > 0);
    }
    
    public boolean removeEnclosures(long itemId) {
    	return (mDb.delete(DbSchema.EnclosureSchema.TABLE_NAME, DbSchema.EnclosureSchema.COLUMN_ITEM_ID + "=?", new String[]{Long.toString(itemId)}) > 0);
    }
    
    public boolean removeEnclosure(long id) {
    	return (mDb.delete(DbSchema.EnclosureSchema.TABLE_NAME, DbSchema.EnclosureSchema._ID + "=?", new String[]{Long.toString(id)}) > 0);
    }
}
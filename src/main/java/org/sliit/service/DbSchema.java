package org.sliit.service;

import android.provider.BaseColumns;

public final class DbSchema {
	
	public static final String DATABASE_NAME = "dbfeed";
	public static final int DATABASE_VERSION = 3;
	public static final String SORT_ASC = " ASC";
	public static final String SORT_DESC = " DESC";
	public static final String[] ORDERS = {SORT_ASC,SORT_DESC};
	public static final int OFF = 0;
	public static final int ON = 1;

	public static final class FeedSchema implements BaseColumns {
		public static final String TABLE_NAME = "feeds";
		public static final String COLUMN_URL = "url";
		public static final String COLUMN_HOMEPAGE = "homepage";
		public static final String COLUMN_TITLE = "title";
		public static final String COLUMN_TYPE = "type";
		public static final String COLUMN_REFRESH = "refresh";
		public static final String COLUMN_ENABLE = "enable";
		public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_URL + " TEXT NOT NULL," + COLUMN_HOMEPAGE + " TEXT NOT NULL," + COLUMN_TITLE + " TEXT NOT NULL," + COLUMN_TYPE + " TEXT," + COLUMN_REFRESH + " INTEGER," + COLUMN_ENABLE + " INTEGER NOT NULL);";
		public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
	}
	
	public static final class ItemSchema implements BaseColumns {
		public static final String TABLE_NAME = "items";
		public static final String COLUMN_FEED_ID = "feed_id";
		public static final String COLUMN_LINK = "link";
		public static final String COLUMN_GUID = "guid";
		public static final String COLUMN_TITLE = "title";
		public static final String COLUMN_DESCRIPTION = "description";
		public static final String COLUMN_CONTENT = "content";
		public static final String COLUMN_IMAGE = "image";
		public static final String COLUMN_PUBDATE = "pubdate";
		public static final String COLUMN_FAVORITE = "favorite";
		public static final String COLUMN_READ = "read";
		public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_FEED_ID + " INTEGER NOT NULL," + COLUMN_LINK + " TEXT NOT NULL," + COLUMN_GUID + " TEXT NOT NULL," + COLUMN_TITLE + " TEXT NOT NULL," + COLUMN_DESCRIPTION + " TEXT," + COLUMN_CONTENT + " TEXT," + COLUMN_IMAGE + " TEXT," + COLUMN_PUBDATE + " INTEGER NOT NULL," + COLUMN_FAVORITE + " INTEGER NOT NULL," + COLUMN_READ + " INTEGER NOT NULL);";
		public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
	}
	
	public static final class EnclosureSchema implements BaseColumns {
		public static final String TABLE_NAME = "enclosures";
		public static final String COLUMN_ITEM_ID = "item_id";
		public static final String COLUMN_MIME = "mime";
		public static final String COLUMN_URL = "URL";
		public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_ITEM_ID + " INTEGER NOT NULL," + COLUMN_MIME + " TEXT NOT NULL," + COLUMN_URL + " TEXT NOT NULL);";
		public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
	}
}

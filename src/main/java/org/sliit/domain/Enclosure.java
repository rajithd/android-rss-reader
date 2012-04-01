package org.sliit.domain;

import java.net.URL;

public class Enclosure {
	private static final String LOG_TAG = "Enclosure";
	
	private long mId = -1;
	private String mMime;
	private URL mURL;
	
	public Enclosure() {}
	
	public Enclosure(long id, String mime, URL url) {
		this.mId = id;
		this.mMime = mime;
		this.mURL = url;
	}
	
	public void setId(long id) {
		this.mId = id;
	}
	
	public long getId() {
		return mId;
	}
	
	public void setMime(String mime) {
		this.mMime = mime;
	}
	
	public String getMime() {
		return this.mMime;
	}
	
	public void setURL(URL url) {
		this.mURL = url;
	}

	public URL getURL() {
		return this.mURL;
	}
	
	public String toString() {
		return "{ID=" + this.mId + " mime=" + this.mMime + " URL=" + this.mURL.toString() + "}";
	}
}

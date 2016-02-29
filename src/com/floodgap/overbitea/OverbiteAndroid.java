package com.floodgap.overbitea;

/*
 * Overbite Android
 * A wonderful and exquisite gopher client for Android
 * Also washes dishes
 * Finds gorgeous MOTASes
 * Does windows, but not Windows
 * Requires Android SDK 1.5 and up.
 * BSD-license text follows.
  
Copyright (c) 2010, Cameron Kaiser
Copyright (c) 2010, Contributors to Overbite
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Overbite Project nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * 
 */

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.net.*;	

public class OverbiteAndroid extends Activity {
	Uri mCurrentUri = null;
	String mCurrentKey = null;
	WebView mWebView;
	String[][] mHistory = new String[128][5];
	String[][] mCache = new String[128][4];
	float[] mScrollPos = new float[128];
	int[] mZoomLevel = new int[128];
	int mHistoryEntries = -1;
	OverbiteDocShell mDocShell;
	TextView mTextView;
	Thread mLoader = null;
	OverbiteAndroid whoAmI = this;
	DisplayMetrics metrics = new DisplayMetrics();
	
	/* Currently this is for debugging only! */
	final static boolean useSOCKS = false;
	
	final static String SOCKShost = "stockholm.floodgap.com";
	final static int SOCKSport = 1080;
	
	// Generic helper to handle socks. Should run within a try().
	// If the Java compiler has any brains at all, it should be able to optimize the shet out of this.
	public final class SOCKSHelper {
		
		public final Socket OpenProxiedSocket(String hostname, int port) throws UnknownHostException, IOException {
    		Socket s;
    		if (useSOCKS) {
    			s = new Socket(SOCKShost, SOCKSport); 
    		} else {
    			s = new Socket(hostname, port);
    		}
   			return s;
		}
		public void EstablishProxiedLink(DataInputStream in, PrintStream out, String hostname, int port) throws IOException {
    		if (useSOCKS) {
    			byte[] b = new byte[2];
    			int count = 0;
    			
    			out.write(5); // HELLO SOCKS v5
    			out.write(1); // auth supported (1 method)
    			out.write(0); // ... which is "no support"
    			out.flush();
    			// read the server's response (two bytes)
    			while (count < 2)
    				count += in.read(b, 0, 2);
    			out.write(5); // CONNECT SOCKS v5
    			out.write(1); 
    			out.write(0);
    			out.write(3);
    			out.write((byte)hostname.length());
    			out.print(hostname);
    			int w = port / 256;
    			int x = port & 255;
    			out.write((byte)w);
    			out.write((byte)x);
    			out.flush();
    			
        		byte[] c = new byte[10];
        		count = 0;
        		// read the server's response (ten bytes)
        		while (count < 7)
        				count += in.read(c, 0, 10);
    		}			
		}
	}
	
	/* for base64 message passing */
    private static final String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";
    
    /* our content icons, encoded into Base64 for the WebView */
    private static final String[] inlineIcons = {
    	/* doc */
    	"biSJbmB6TqyrbuC5wgTNev7Nn6jnNsR+tpfkCYEEP0BY+V5MbGpDiH0GhkmvndrA8scmXYchnOnSqRFI/R4IM5xT67XWtF+R2HI9R17wXLN+Zn4RVoNdjUZkfXp/jkmAcZhSglucfIRTlBadhzZxb5JnfoKBoqGpNZineJaun5SugaK6NZYkuCO6IrwhviO0N7AowibEKcY3yrnLs6y9OqRzoa8AwanTotXW2tEynIvBveO/5bHkzNhFyUfrSu1C70/hgPW48zT3Vf69y9NLdvWD9/fPJlGUiwxTdVAY+dK9ZwWcRmE8VVJHfRXEZ0pNvUPUy2EWJHdwgTQjL4hZpJI9jAqVyJKcxHdiPlzYRX015OfDfp7eQXEuRPgUFpZpvUU99Qh0VxHvX4EuYUlH9Kwlyo7ahUhS0ZLpX4lWJYi2MxltV4luNTkk19rrXZVulbnXN5xj14N2VakXWB7hXal+jWZy4HzypsGFWdxYwbO34MObLkyZQrW76MObPmzZw7e/4MOrTo0aRLmz6NOrVqLgUAADs=",
    	/* folder */
    	"biSJbmiabqyrbuC8fyTNf2jef6zvf+DwwKh4Ci8YhMFocQpfNpZDKgVKV0kaxkr4gjBsk9gC/jcNlyZqa1XmBVU43HP3O4/P7k1O34fpu/JLWGFsX1l3GoVghotojoqBjIaJhIKCnhl6np1wX5sAkaCtV52TTIYyXm2XDqk1Y5BeuWKKvQ2rNWS1oqlLtqq4sKGxxwizv8uwtwdRpsLPzrTKzTqvu8U51cPJ1zW3tNbZ0MjmMsS14uzWuAbvOMrH0cv73eXnNdaX8/nc+dPs8Okj4a4Gj5u0Hu0MAZCR0dRHjwz0IZ6CQCDHdRVaCJXDHatXnoDqQXjjDsjcz4b52pVIJA0iP5YqATSiiVqfyxkGWkmytrQuTZEyg0oQ5EGT1KtCjSpZs2MH3aJ4zUqVSrWr2KNavWrVy7ev0KNqzYsWTLmj2LNq3aEAUAADs=",
    	/* error */
    	"biSJbmiabqyrbuC8fyTNf2jef6zvc+CQgKh8Ffh4hMFo0VpTPJjDyn0OiCClBgrYinxMt1WsRRpcZsRJ+rPfXGnWN/5Df6HKmze/Qy+XAs1IVXRyT4NzFokEizGMCn1dgYE/l4ACf5ggmXsOlYOIPpGdopGgj6eWVHWrrUd9jgtqr4ChNq+Ck7S+tie0slhZoZ7ICVBbzb0ss5hTicjPwA1mxaCx1dyeDMok1MNt0qbJ0qDaF8Yq7LfEwdzg5bbHwtnmKOlssaX+0OuZjLvf1v0yp057iROvhvhaZKAgkW3HXvUkKAwbBZ6ucQBcZ5a/zYZdS4Md8Xjx9BTsxgkVfKJiuflez2UsW9kTEVtnxXk2JMb2lmLrtZQx1Mn3GKpRPK5SI8pEl/Lj3Z9Om+puWYUr2KNavWrVy7ev0KNqzYsWTLmj2LNq3atWzbun0LN67cuXTr2r2L924BADs=",
    	/* search */
    	"bASJbhCZbqCqAupjbxSz/mweJkzSs3uzLcerWdEJgLGImu5UgWVD6Zp6UkZ6V2ppYgV7sZdmNfMKx8MWXNFPF5h2Zf1+mkvC31qFv3yYxjlxBo9hT31vKF5EYEx7fVeKS4yAP5ESUZxVSpV/kXSUeT+VjIZ/jp2LOXQjoHikKGCsjaGlvUWJuxCTH5Ams5G8HbtIdbJ+zj+ur7mOeXXDVj2oZYvOCZuqwh6nCNnX3WHHzcG10dLGWO0M14mc79fCodmgRvLR/vPt+er34f34eunL9xOgh6o+dqnT2D7IDgc+isHhVJBb1IVHgHU7Fts1D8EdKoKNw7j2xAYtolMaNJiABXrQTWsooghyRjyoIljaHNkUgYcNw5jSIynUDtBVz3s2jEnjOJKh3KsiK/pz6F9ptKtWrIrNCYcnXp9SunrWJHhS0bxiratGfXHorq9u2guGPa0g1q9y4tuHqXzu0rjixgv0kHd8xrWCvixFD/Ml64+LHUwpKv8q3c1DFmyxg3T64pubPn0aRLmz6NOrXq1axbu34NO7bs2bRr276N20ABADs=",
    	/* connect */
    	"biSJbmiabqyrbuC8fyTNf2/QD6zvc+j9v8hkRAULO7JI+YZcXJfOqU06gFOsFaI1pudZv9UrrgBjknLqON17Ta7Pay32vqnA6/h/V4xdnx1xfIMEhX6BfXd1DECKRI2Nj4CBlZNLlQyXhJmXko2Om4iQgaKmpA+mO6iNqjespaKgobuznr6Yqbq7vLixna2uv7RRscAGxMzOuzmqy7NMfW7BoozWL7/MonbAR9bWebnbjdDd4mDlhVPUpu1+6unvB7nuduHq6dx26/z/9xy1xPCj4k8ygFHFOw356DC0P8y8ZQwplEA5FVpKew4ShIdRgFRpQzzpKwj3UiZjKYEeRHMuIeWiRZ0hchdBdReoTZJOFGc+VAXKsZkpWIn99mDfVWFNUIojmVmhAqBFYJoxl6HpXaFKtDpAKtemDalapPrgjJfjW7R+tYr8Xaun0LN67cuXTr2r2LN6/evXz7+v0LOPCmAgA7",
    	/* download */
    	"biSJbmOQHqCqAuxq7vTMUqjUN2m/fL7gsigEIhseg7InPKJemWaAagzo5saMNSqxuW1muIca3gadZcHmfOQKm60t653ym5XUyH3fe8/IVv56cBeCa4RrhlqAeoSLbX6PEIGSk3CTJneXiVeZnI+QkaKnqCeDd6UGp6mhq4yopp+Qo7KVsoWos3ipsWurt569sH7HuKtlscLNwbjMzs6jwMDZr8y0ntGXsdLW2tvUy9XdtM/Hxcbh6Oq+s9zYsqrNqOHdZ3U/ldL/lenZlLb7tPWT+A7gICcyDwyzxIC6cwuNfu4RY+6dTh4zYQ3EVyIvKSVRRXLKTIkSRLmjyJMqXKlSxbunwJM6bMmTRr2ryZsgAAOw==",
    	/* HTML */
    	"biSJbmB6TqyrbuC5wgTNev7Nn6jnNsR+tpfkCYEEP0BY+V5MbGpDiH0GhkmvndrA8scmXYchnOnSqRFI/R4IM5xT67XWtF+R2HI9R17wXLN+Zn4RVoNdjUZkfXp/jkmAcZhSglucfIRTlBadhzZxb5JnfoKBoqGpNZineJaun5SugaK6NZYkuCO6IrwhviO0N7AowibEKcY3yrnLs6y9OqRzoa8AwanTotXW2tEynIvBveO/5bHkzNhFyUfrSu1C70/hgPW48zT3Vf69y9NLdvWD9/fPJlGUiwxTdVAY+dK9ZwWcRmE8VVrIMxo8aNyxw7evwIctO2aY2yMSyJcs3EkclMkoRwUaTLSSwXxJRw09xMmzXhocTkDajCViWNcEvorCicoZ+CJlWZTlFUhNEAFs1jtSpWrUdTxskKcCYkqT3l1Rw1lWU7sjvdnZWWVqxatGXt8cwWd5FcuHXxvcU7UtJaum3N7iS8lWjinOjugg0zeC/gwnb1huX6GDJfyn4PbzYpeO5mr4qPGi19WTPpxzUsL+7r0WDI2bRr276NO7fu3bx7+/4NPLjw4cSLGz+OPLny5cybgywAADs=",
    	/* image */
    	"biSJbmiabqyrbuA8SyjM02bd14qO9Sr6sAAaOhj2EEwpLFpM3gjOKksSb1iqVas9zubesNc8HispRsTg9J6rax5I4/4e9W3XRf5dlrylGohLLnEHTRJxi41GOYeHJI+PizqBK5cDSTM5lSqVDV+af4Rdlo6ZkwJ1m4SfpJRBTwCgvawEqniWR6OourinhbGosgGlHL99sKvNs67MgpbBrLHIraXHyAqUsNeYyGTesNJf3NzTOY7dO7nd4Nnowu7g4PwqSeKyv/bN3hVM+Lf69vAz9/wc7hG1iOXrJp2q4pnIfQIIRiET9E8UesVkUP7BcXMhS3cV/IhgaPheTQsWS7M/mcCRzZRuU/DTDVyGyXUGPMluQ4movDc6ZInTvDBRzKDajDnhApFgUo1Cc5pVBJsitolFmWpVGlriOa8qhFVtZYVlWWU5o5ronEetV68qwot0jhPmTbhy7Kto9yaZGL1ZaqPV2yWr2Kl5oXwzgFz6UXhnFjEXXkPGY6Vu/bw00xXw3c2bJNx6JHUy5t+TPqxadXu1Htemvr2Glg0/6b1pmYeJpTucXie11dzgQ987b3Irny5cybO38OPbr06dSrW7+OPbv27dy7e/8OPrz48eTLmz+PPr16CAUAADs=",
    	/* url */
    	"YQQALiCZbqyqJuxMay+tbIjOc2mvf67vEJf0DM8EgsTpDMmVLSjDqfDak1Rl1ct6vsjQsmeQPhsolqNj/TQq2YXCqyfdD4bp4cvWv4vP7s0uf3QBMoKHNhd3I4ZaEYwtil8fgRSQlYQdlhuQeHudS5yQmoCfq5MZoSisp5sAoT92pkmSBLGGubGVl7WidWmrjrmutQSGx6+JVh3OtIO7xsB0zxHCR9/CfoeVroadDF3EzNuK3QjVPuzZrcPbyHfi6O3Jf+9d7+rYh9y64Z769PXp18PbYNqeeOVKl4HN60SVgQXz2JEwXCIoiuGCKE/xgTurnX8Ncgc1g4VnwVLuSZjL7urfrXK+U6jJLGYZII8+PKfRoV1lzHLSDKaxYv0izakpfQmNes2eOZzedSnfmsSUXashFFGtN03ewajOVWXFj12OMBdmClTlBDOiv7pwrFWTvfQmr20yadcSIApmWAxOYippMCV1E2GG7Ug1T/0l3bpHFbr5AjKzXhOJpiwFKUErwzmTOTp59BhyY5urRJQ5tRH3HnUUnmng9Vz4bc0HWL0nNNt+aHb+fpyqxF/1Y1XPPxu7d9PV2eGDpJ0mOAS9d5/UVeoNl9J99dne52473D6+1svq/h9EDKs38PP778+fTr27+PP7/+/fz7+xX/D2CAAg5IYIEGHohgggouyCAHBQAAOw=="
    };
    private static final String inlinePrefix = "R0lGODlhZABkAIABAAAAAAAAACH5BAEAAAEALAAAAABkAGQAAAL/jI+py+0Po5y02ouz3rz7D4"; 
    
    // convenience method for l10n
    public String S(int resource) { return this.getString(resource); }
    
    /* Deliver the content to the web view. Uses tricks, wanted in several jurisdictions for abuse. */
    public void contentOut(WebView view, String data, String mimeType, String encoding) {
    	String buf = "";
    	
    	if (mimeType == null || mimeType == "")
    		return;
    	
    	if (mimeType.indexOf("image/") == 0) { // This is a kludge for 2.2: embed it in an HTML document
    		// Assumed that our encoding is always base64
    		StringBuilder hbuf = new StringBuilder();

    		hbuf.append("<html><body><img src=\"data:").append(mimeType).append(";").append(encoding).append(",")
    		    .append(data).append("\"></body></html>");
    		
    		try {
				buf = URLEncoder.encode(hbuf.toString(), "utf-8").replaceAll("\\+", " ");
			} catch (UnsupportedEncodingException e) {
				view.loadData("<html><body>bogus encoding</body></html", "text/html", "utf-8");
			}
    		view.loadData(buf, "text/html", "utf-8");
    		return;
    	}
    	
    	if (mimeType == "text/html" && encoding == "utf-8") {
    		try {
				buf = URLEncoder.encode(data, "utf-8").replaceAll("\\+", " ");
			} catch (UnsupportedEncodingException e) {
				view.loadData("<html><body>bogus encoding</body></html", "text/html", "utf-8");
			}
    		view.loadData(buf, "text/html", "utf-8");
    		return;
    	}
    	
    	// anything else. invariably, however, 2.2 always renders it as text/html.
    	// we compensate here by converting text/plain to text/html.
    	view.loadData(data, mimeType, encoding);
    }
    /* Keep the title bar in sync. */
    public void updateTitleBar() {
    	String[] thisHistory = mHistory[mHistoryEntries];
    	if (thisHistory[0] == "about:") {
    		mCurrentKey = null;
    		mCurrentUri = null;
    		whoAmI.setTitle(S(R.string.short_app_name) + " | "+S(R.string.about_app_name));
    		return;
    	}
    	mCurrentKey = thisHistory[0]+"/"+thisHistory[2];
    	whoAmI.setTitle(
    			((useSOCKS) ? "SOCKS:" : "") +
    			(S(R.string.short_app_name) + " | "+thisHistory[0]+"/"+thisHistory[3]+thisHistory[2]));
    	whoAmI.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);
    }
    
    public void reloadContentFromCache() {
    	stopGopherLoad();
    	String[] thisCache = mCache[mHistoryEntries];
    	updateTitleBar();
    	mTextView.setText(R.string.cache_hit);
    	mDocShell.setPendingScrollTo(mScrollPos[mHistoryEntries]); // docshell will catch up after page load   
    	mWebView.setInitialScale(mZoomLevel[mHistoryEntries]);
    	contentOut(mWebView, thisCache[0], thisCache[1], thisCache[2]);
    }
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK && mHistoryEntries > 0) {
	    	// free up memory
	    	mHistory[mHistoryEntries] = new String[] { "", "", "", "", "" };
	    	mCache[mHistoryEntries] = new String[] { "<html><body><b>"+S(R.string.cache_bad)+"</b></body></html>", "text/html", "utf-8" };	    		
	    	mHistoryEntries--;
	    	String[] thisHistory = mHistory[mHistoryEntries];
	    	mCurrentUri = Uri.parse(gopherToUri(thisHistory[0], java.lang.Integer.parseInt(thisHistory[1], 10),
												thisHistory[2], thisHistory[3].charAt(0), thisHistory[4]));
	    	if (mLoader != null) { // We have an optimization. No need to fetch from cache.
	    		stopGopherLoad();
	    		mTextView.setText(R.string.canceled);
	    		updateTitleBar();
	    		return true;
	    	}
	    	reloadContentFromCache();
	    	return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	/* OverbiteDocShell acts as the docshell for our gopher client.
	 * The only missing part is the back button (above). */
	private class OverbiteDocShell extends WebViewClient {
		Handler mySink = null;
		float pendingScrollTo = 0.0f;
		public void setMySink(Handler sink) {
			mySink = sink;
		}
		public void setPendingScrollTo(float y) {
			pendingScrollTo = y;
		}
		
		// Mostly to handle cached loads we want to scroll back in.
		// This doesn't work in 2.x and I don't really care.
		@Override
		public void onPageFinished(WebView view, String url) {
			if (pendingScrollTo > 0.0f) {
				int contentHeight = view.getContentHeight();
				int y = (int) ((float) contentHeight * pendingScrollTo); // matters not if zero.
				view.scrollTo(0, y);
			}
			pendingScrollTo = 0.0f;
			//super.onPageFinished(view, url);
		}
		
	    @Override
	    // We have a new URL to load, invoked by user action.
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	if (mySink == null)
	    		return false;
	    	Uri newUri = Uri.parse(url);
	    	if (newUri == null)
	    		return false;
	    	String scheme = newUri.getScheme();
	    	if (scheme == null)
	    		return false;
	    	try {
				alert(URLDecoder.decode(url, "utf-8"));
			} catch (UnsupportedEncodingException e) {
				alert(url);
			}
	    	return startGopherLoadFromUri(newUri, mySink);
	    	// Other protocols are loaded into an intent and we let the Launcher
	    	// sort it out. (For example, web -> Browser)
	        //view.loadUrl(url); // if someday we wanted to embed the browser. right now, we don't.
	    }
	}

    
    /* this messageSink is our event handler for network status and the docshell. */
    private Handler messageSink = new Handler() {
        	@Override
        	public void handleMessage(Message what) {
        		// Update the process bar. This can be combined with any message.
        		if (what.arg1 > 0)
        			whoAmI.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, what.arg1);
        		switch(what.what) {
        		case 1: // status message
        			String statusMessage = (String)what.obj;
        			mTextView.setText(statusMessage);
        			break;
        		case 928: // end of content load (may not have been successful)
        			String[] data = (String [])what.obj;
        			mWebView.setInitialScale(mZoomLevel[mHistoryEntries]);
        			contentOut(mWebView, data[0], data[1], data[2]);
        			mCache[mHistoryEntries] = new String[] { data[0], data[1], data[2] };
        			stopGopherLoad();
        			if (data[3].length() > 0)
        				mTextView.setText(R.string.error);
        			else
        				mTextView.setText(S(R.string.done) + ((data[1] == "text/html") ? "" : " ("+data[1]+")"));
         			whoAmI.updateTitleBar();
         			break;
        		}
        	}
    };

    /* Start the application (or restore from frozen state or change in orientation).
     * This may also get called if the application is unexpectedly quit by a task manager,
     * in which case we must be prepared for failure.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Try to resume with a warm start first
        if (savedInstanceState != null) {
        	try {
        		mHistoryEntries = savedInstanceState.getInt("mHistoryEntries");
        		String s = savedInstanceState.getString("mCurrentUri");
        		if (s.length() > 0)
        			mCurrentUri = Uri.parse(s);
        		else
        			mCurrentUri = null;
        		mCache = (String[][]) savedInstanceState.getSerializable("mCache"); // triggers exception
        		mHistory = (String[][]) savedInstanceState.getSerializable("mHistory"); // triggers exception
        		mScrollPos = savedInstanceState.getFloatArray("mScrollPos");
        		mZoomLevel = savedInstanceState.getIntArray("mZoomLevel");
        		mCurrentKey = savedInstanceState.getString("mCurrentKey");
        	} catch(ClassCastException cc) { 
        		// this failed. Start up cold.
        		savedInstanceState = null;
        	}
        }
        
        // Set up the window
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        setTitle(R.string.app_name);
        
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(mDocShell = new OverbiteDocShell());
        mDocShell.setMySink(messageSink);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);

        mTextView = (TextView) findViewById(R.id.statusbar);
        
        whoAmI = this;
        
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        // Cold start if warm start failed, or we don't have a startup bundle
        if (savedInstanceState == null) {
        	// Reset everything just in case the warm start was incomplete
        	mHistoryEntries = -1;
        	mCurrentUri = null;
        	mCurrentKey = null;
        	
        	mTextView.setText(R.string.easter_egg);        
        	// See if we were passed a URL from something (Browser?)
        	if (!startGopherLoadFromIntent(getIntent(), messageSink))
        		// Couldn't start from the intent; fall back on our default home page.
        		startGopherLoadFromUri(Uri.parse(S(R.string.startup_url)), messageSink);
        	return;
        }
        
        // Otherwise reload the cache from warm start and re-enter main loop
        reloadContentFromCache();
    }
    
    /* Handle freezing state or switching orientation */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	// Remember the Alamoscroll!
        if (mHistoryEntries > -1 && mWebView != null) {
        	int contentHeight = mWebView.getContentHeight();
        	if (contentHeight > 0) {
        		int scrollY = mWebView.getScrollY();
        		mScrollPos[mHistoryEntries] = ((float) scrollY / ((float) contentHeight));
        	}
        }
    	savedInstanceState.putInt("mHistoryEntries", mHistoryEntries);
    	if (mCurrentUri != null)
    		savedInstanceState.putString("mCurrentUri", mCurrentUri.toString());
    	else
    		savedInstanceState.putString("mCurrentUri", "");
    	savedInstanceState.putSerializable("mCache", mCache);
    	savedInstanceState.putSerializable("mHistory", mHistory);
    	savedInstanceState.putFloatArray("mScrollPos", mScrollPos);
    	savedInstanceState.putIntArray("mZoomLevel", mZoomLevel);
    	savedInstanceState.putString("mCurrentKey", mCurrentKey);
    	
    	super.onSaveInstanceState(savedInstanceState);
    }
    
    /* Handle death */
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	stopGopherLoad();
    }
    
    /* Handle gopher:// URLs passed to us by Browser */
    @Override
    public void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	setIntent(intent);
    	startGopherLoadFromIntent(intent, messageSink); // no worries if nothing happens
    }
    
    /* define menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, 0, 0, R.string.about_app_name);
    	menu.add(0, 1, 0, R.string.enter_url);
    	menu.add(0, 2, 0, R.string.reload_this);
    	menu.add(0, 3, 0, R.string.add_to_home);
    	menu.add(0, 4, 0, R.string.server_root);
    	return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
    	String[] thisHistory;
    	switch(item.getItemId()) {
    		case 0: // About
    			alert(S(R.string.about_app_name)+"...");
    			startGopherLoad("about:", -1, "overbite", 'h', "", messageSink);
    			return true;
    		case 1:
    	    	final EditText input = new EditText(whoAmI);
    	    	inputBox(S(R.string.enter_url_1), S(R.string.enter_url_2), input, new DialogInterface.OnClickListener() {
    	    	public void onClick(DialogInterface dialog, int whichButton) {
    	    	  String value = input.getText().toString().trim();
    	    	  if (value == "" || value == null) {
    	    		  alert(S(R.string.enter_void));
    	    		  return;
    	    	  }
    	    	  if(value.indexOf("://") == -1)
    	    		  value = "gopher://" + value;
    	    	  Uri newUri = Uri.parse(value);
    	    	  if (newUri == null) {
    	    		  alert(S(R.string.enter_bad_url));
    	    		  return;
    	    	  }
    	    	  String scheme = newUri.getScheme();
    	    	  String hostname = newUri.getHost();
    	    	  if (scheme == null || scheme == "" || hostname == null || hostname == "") {
    	    		  alert(S(R.string.enter_no_host));
    	    		  return;
    	    	  }
    	    	  if (!scheme.matches("^gopher$")) {
    	    		  alert(S(R.string.enter_not_gopher));
    	    		  return;
    	    	  }
    	    	  startGopherLoadFromUri(newUri, messageSink);
    	    	  }
    	    	}, null);
    			return true;
    		case 2: // Reload
    			alert(S(R.string.reloading_this)+" ...");
    			thisHistory = mHistory[mHistoryEntries];
	    		mHistoryEntries--;
	    		startGopherLoad(thisHistory[0], java.lang.Integer.parseInt(thisHistory[1], 10),
	    						thisHistory[2], thisHistory[3].charAt(0), thisHistory[4],
	    						messageSink);
    			return true;
    		case 3: // Shortcut
    			if (mCurrentKey == null) // occurs for about: and on start up
    				return true; // politely ignore
    			if (mCurrentUri == null)
    				return true;
    			Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
    			shortcutIntent.setData(mCurrentUri);
    			Intent intent = new Intent();
    			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mCurrentKey);
    			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.mark));
    			intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
    			sendBroadcast(intent);
    			alert(S(R.string.added_to_home));
    			return true;
    		case 4: // Root
    			if (mCurrentUri == null) // politely ignore
    				return true;
    		
    			thisHistory = mHistory[mHistoryEntries];
    			if (thisHistory[2] == "" || thisHistory[2].matches("^/$")) {
    				alert(S(R.string.server_root_already));
    				return true;
    			}
    			alert(S(R.string.server_rooting)+" "+thisHistory[0]+" ...");
    			startGopherLoad(thisHistory[0], java.lang.Integer.parseInt(thisHistory[1], 10),
    							"", '1', "", messageSink);
    			return true;
    	}
    	return false;
    }
    
    /* Utility interface functions */
    public void sendHandler(Handler sink, int code, int arg1, int arg2, Object obj) { // Sends the sink a status code.
    	sink.sendMessage(Message.obtain(sink, code, arg1, arg2, obj));
    }
    public void alert(String what) { // Pops up a toast alert.
    	Toast.makeText(getApplicationContext(), what, Toast.LENGTH_SHORT).show();
    }
    public void inputBox(String title, String message, EditText boundInput, // defined in the scope of the click handlers!
    		final OnClickListener ok, OnClickListener cancel) { // Pops up an input box.
    	AlertDialog.Builder requester = new AlertDialog.Builder(this);
    	if (cancel == null) // convenience method, but ok must always be specified.
    		cancel = new DialogInterface.OnClickListener() {
    	    	  public void onClick(DialogInterface dialog, int whichButton) {
    	      	    // Canceled.
    	      	  }
    		};
    	      
    	requester.setTitle(title);
    	requester.setMessage(message);
    	boundInput.setOnKeyListener(new OnKeyListener() {
 			public boolean onKey(View v, int keyCode, KeyEvent event) {
 				// USE THE BUTTONS DAMMIT
				if (keyCode == KeyEvent.KEYCODE_ENTER) { // otherwise it distorts our input box.
					//ok.onClick(null, 0); // This doesn't actually work.
					return true;
				}
				return false;
			}
    	});

    	// Set the already created EditText's view to get user input.
    	requester.setView(boundInput);
    	// Set our handlers.
    	requester.setPositiveButton(R.string.ok, ok);
    	requester.setNegativeButton(R.string.cancel, cancel);
    	requester.show();
    }
    
    /* "necko" */
    /* Start a Gopher URL from an intent, if we have one. */
    public boolean startGopherLoadFromIntent(Intent intent, Handler mySink) {
    	// We'll take pretty much ANY action (we really are that kind of girl).
    	// If the target isn't actually gopher://, startGopherLoadFromUri() will
    	// sort it out.
    	if (intent != null) {
    		Uri newUri = intent.getData();
    		return startGopherLoadFromUri(newUri, messageSink);
    	}
    	return false; // no intent to load from
    }
    
    /* Used to start a Gopher load from a Uri (typically passed from an Intent,
     * but also from the docshell). */
    public boolean startGopherLoadFromUri(Uri newUri, Handler mySink) {
    	mCurrentUri = newUri;
    	if (newUri == null || mySink == null) // paranoia. we do get null URIs!
    		return false;
    	String scheme = newUri.getScheme();
    	if (scheme == null)
    		return false;
    	// Handle kicking out non-gopher:// URLs.
    	if (!(scheme.matches("^gopher$"))) { // let's pass it on to someone who cares.
	    	Intent myIntent = new Intent(Intent.ACTION_VIEW, newUri);
	    	startActivity(myIntent);
	    	return true;
    	}

    	String hostname = newUri.getHost();
		if (hostname.length() < 1 || hostname == null)
			return false;
		int port = newUri.getPort();
		// oh, don't let them use us for naughtiness, master!
		if (port != 43 && port != 70 && port != 71 && port != 79 && port != 80 && port != 2347 && port != 3070 && port != 7070 && port != 7071 && port != 27070)
			port = 70;
		String sel = newUri.getPath();
		char itype = '9';
		if (sel.length() < 1 || sel == null || (sel.length() == 1 && sel.charAt(0) == '/')) {
			itype = '1';
			sel = "";
		} else {
			if (sel.charAt(0) == '/')
				sel = sel.substring(1);
			itype = sel.charAt(0);
			sel = (sel.length() == 1) ? "" : sel.substring(1);
		}
		String query = newUri.getQuery();
		if (query == null)
			query = "";
		startGopherLoad(hostname, port, sel, itype, query, mySink);
		return true;    	
    }
    /* Start a Gopher load. This pops a requester, if needed, and then starts the actual worker thread function. */
    public void startGopherLoad(final String hostname, final int port, final String sel, final char itype, final String query,
			final Handler sink) {
    	if (itype == '4' || itype == '5' || itype == '6' || itype == '9' || itype == 's' || itype == 'd' || itype == ';') {
    		//_startGopherDownload(hostname, port, sel, itype, query, sink); // NOT YET! SIGH.
    		alert(S(R.string.dl_yet_to_come));
    		return;
    	}
    	if (itype == '3' || itype == 'i') {
    		alert(S(R.string.itype_no_load));
    		return;
    	}
    	if (itype != '7' && itype != 'I' && itype != 'p' && itype != 'g' && itype != '0' && itype != '1' && itype != 'h') {
    		alert(S(R.string.itype_no_do));
    		return;
    	}
    	if (itype != '7' || query.length() > 0) {
    		_startGopherLoad(hostname, port, sel, itype, query, sink);
    		return;
    	}
    	final EditText input = new EditText(whoAmI);
    	inputBox(S(R.string.search_box_1), S(R.string.search_box_2), input, new DialogInterface.OnClickListener() {
    	public void onClick(DialogInterface dialog, int whichButton) {
    	  String value = input.getText().toString().trim();
    	  _startGopherLoad(hostname, port, sel, itype, value, sink);
    	  }
    	}, null);
    }
    public void _startGopherDownload(final String hostname, final int port, final String sel, final char itype, final String query,
			final Handler sink) {
    	
    	// Ask for the filename in an intent and call us back with it (new function goes here)
    	
    	// Make a new thread. It will kill itself off. America eats its young, but not its threads.
    	new Thread() {
    		public void run() {
    			ThreadServiceConnection con = new ThreadServiceConnection();
    			con.setParent(this);
    			bindService(new Intent(getApplicationContext(), GopherDownloadService.class), con,
    						Context.BIND_AUTO_CREATE);
    			while(interrupted() == false) { // wait until the download is done. need to unbusywait this.
    				yield();
    			}
    		}
    	}.start();
    }
    public void _startGopherLoad(final String hostname, final int port, final String sel, final char itype, final String query,
    							final Handler sink) {
        if (mLoader != null) {
        	stopGopherLoad();
        	mHistoryEntries--;
        	updateTitleBar();
        }
        if (mHistoryEntries > -1 && mWebView != null) {
        	int contentHeight = mWebView.getContentHeight();
        	if (contentHeight > 0) {
        		int scrollY = mWebView.getScrollY();
        		mScrollPos[mHistoryEntries] = ((float) scrollY / ((float) contentHeight));
        	}
        	mZoomLevel[mHistoryEntries] = (int)(mWebView.getScale() * 100);
        }
        mHistoryEntries++;
        mHistory[mHistoryEntries] = new String[] { hostname, ""+port, sel, ""+itype, query };
        updateTitleBar();
        if (itype == '7') // need to update for the query string
        	mCurrentUri = Uri.parse(gopherToUri(hostname, port, sel, itype, query));
        mCache[mHistoryEntries] = new String[] { "<html><body><b>"+S(R.string.cache_bad)+"</b></body></html>",
        		"text/html", "utf-8" };
        mScrollPos[mHistoryEntries] = 0.0f;
        mZoomLevel[mHistoryEntries] = (int)(mWebView.getScale() * 100);

    	mLoader = new Thread() {
        	public void run() {
        		System.gc(); // assume this task is expensive, so let's houseclean first
				String[] result = _loadViewFromGopher(hostname, port, sel, itype, query, sink);
				if (mLoader != null)
					sendHandler(messageSink, 928, 10000, 0, result);
				else
					sendHandler(messageSink, 1, 10000, 0, S(R.string.canceled));
        	}
        };
        mLoader.start();
    }
    public void stopGopherLoad() {
    	if (mLoader != null) {
    		Thread diediedie = mLoader;
    		mLoader = null;
    		diediedie.interrupt();
    	}
    }
    
    /* Find the next specified character in a byte array. used by bytesToURLandDS */
    public int findNextByte(byte[] what, byte whatFor, int startFrom) {
    	int i;
    	int k = what.length;
    	if (startFrom >= k)
    		return -1;
    	for (i=startFrom; i<k; i++) {
    		if (what[i] == whatFor)
    			return i;
    	}
    	return -1;
    }
    /* Sanitize a string with ampersand encoding, used by bytesToURLandDS */
    public String entityFixBytes(String st) {
    	String s = st;
    	s = s.replaceAll("&", "&amp;");
    	s = s.replaceAll("<", "&lt;");
    	s = s.replaceAll(">", "&gt;");
    	s = s.replaceAll("  ", "&nbsp;&nbsp;");
    	s = s.replaceAll(" &nbsp;", "&nbsp;&nbsp;");
    	s = s.replaceAll("&nbsp; ", "&nbsp;&nbsp;");
    	if(s.charAt(0) == ' ')
    		s = "&nbsp;" + ((s.length() > 1) ? s.substring(1) : "");
    	
    	return s;
    }
    /* Turn a gopher location into a standardized URL. */
    public String gopherToUri(String hostname, int port, String sel, char itype, String query) {
    	// If this is a hURL, it's easy because it's already ready to go. Just unwrap.
    	if (itype == 'h' && (sel.indexOf("URL:") == 0 || sel.indexOf("/URL:") == 0)) {
    		if (sel.charAt(0) == '/')
    			sel = sel.substring(1);
    		return sel.replaceFirst("URL:", "");
    	}
    	// The deprecated GET syntax for Web URLs is a little harder.
    	if (itype == 'h' && (sel.indexOf("GET ") == 0 || sel.indexOf("/GET ") == 0)) {
    		if (sel.charAt(0) == '/')
    			sel = sel.substring(1);
    		sel = sel.replaceFirst("GET ", "");
    		// Now use the Uri.Builder as a helper to create an http:// URL.
    		// This is a little tricky because of the URI encoding. Hope it
    		// doesn't break.
    		Uri.Builder helpMeObiWan = new Uri.Builder();
    		helpMeObiWan.scheme("http");
    		helpMeObiWan.path("/");
    		helpMeObiWan.appendEncodedPath("/"+hostname+":"+port);
    		helpMeObiWan.appendPath(sel);
    		if (query.length() > 0)
    			helpMeObiWan.query(query);
    		return helpMeObiWan.build().toString();
    	}
    	// If this is itype 8, turn into a Telnet URL.
    	if (itype == '8')
    		return "telnet://"+hostname+":"+port+"/";
    	
    	// It must be gopher, so use the Uri.Builder as a helper to create a gopher:// URL.
    	// This dance is to keep only the needed parts URI-encoded.
    	Uri.Builder helpMeObiWan = new Uri.Builder();
    	helpMeObiWan.scheme("gopher");
    	helpMeObiWan.path("/");
    	helpMeObiWan.appendEncodedPath("/"+hostname+":"+port+"/");
    	helpMeObiWan.appendPath((char)itype+sel);
		if (query.length() > 0)
			helpMeObiWan.query(query);
    	return helpMeObiWan.build().toString();
    }

    /*
     * given an array of bytes, emit a URL (as a string), display string and converted itype.
     * This assumes the array contains only one menu entry. If itype i or 3, the URL is "".
     * This parses Gopher menu entries, using gopherToUri to turn them into URLs.
     * Everything is encoded safely for display.
     */
    public String[] bytesToURLandDS(byte[] what) {
    	int endDS = -1;
    	int endsel = -1;
    	int endhost = -1;
    	int endport = -1;
    	int endline = what.length - 1;
    	int beginline = 0;
    	
    	// Trim any unexpected newlines and ignore the "." if the server sends it
    	while (endline > 0 && (what[endline] == 13 || what[endline] == 10))
    		endline--;
    	while (beginline < endline && (what[beginline] == 13 || what[beginline] == 10))
    		beginline++;
    	if (endline < 1 || beginline >= endline)
    		return new String[] { "", "", "i" };
    	byte itype = what[beginline];
    	if (itype == 46) // "."
    		return new String[] { "", "", "i" };
    	
    	endDS = findNextByte(what, (byte) 9, beginline);
    	if (endDS == -1)
    		endDS = endline;
    	if (endDS < beginline+2) // can't possibly have anything of relevance
    		return new String[] { "", "", "i" };
    	
    	// Got itype and display string so far.
    	String ds = entityFixBytes(new String(what, beginline+1, endDS - 1 - beginline));
    	// Return early for i and 3
    	if (itype == 51 || itype == 105)
    		return new String[] { "", ds, (itype == 51) ? "3" : "i" };
    	
    	// If this is an itype we don't support, don't bother parsing out into a URL.
    	// Right now, we only support 0, 1, 7, 8, g, h, p and I.
    	//if (itype != 48 && itype != 49 && itype != 55 && itype != 56 && itype != 103 && itype != 104 && itype != 112 && itype != 73)
    	//	return new String[] { "", ds, ""+(char)itype };
    	
    	String sel = "";
    	String hostname = "";
    	int port = -1;
    	
    	endsel = findNextByte(what, (byte)9, endDS+1);
    	if (endsel != -1) {
    		if (endsel == endDS+1) // possible
    			sel = "";
    		else
    			sel = new String(what, endDS + 1, endsel-(endDS+1));
    		endhost = findNextByte(what, (byte)9, endsel+1);
    		if (endhost != -1 && endhost != endsel+1) {
    			hostname = new String(what, endsel+1, endhost-(endsel+1));
    			// There might not be a tab after the port number (in fact, there probably isn't).
    			endport = findNextByte(what, (byte)9, endhost+1);
    			if (endport != endhost+1) {
    				try {
    					if (endport == -1) // there wasn't
    						port = java.lang.Integer.parseInt(new String(what, endhost+1, 1+endline-(endhost+1)));
    					else // there was (probably Gopher+)
    						port = java.lang.Integer.parseInt(new String(what, endhost+1, endport-(endhost+1)));
    				} catch (NumberFormatException e) {
    					port = -1;
    				}
    			}
    		}
    	}
    	
    	// Do we have enough components to make a URI?
    	if (hostname.length() > 0 && !hostname.matches("error.host") && port > 0) {
    		// Yup!
    		return new String[] { gopherToUri(hostname, port, sel, (char)itype, ""), ds, new String(new byte[] { itype }) };
    	}
    	
    	// Nope. Give up, this line is totally impossible to parse.
    	return new String[] { "", "", "3" }; // I got nothin', kid.
    }
    /*
     * load data from a gopher host/port/sel/itype set, and feed it back to the docshell.
     * SOCKSIFIED FOR AWESOME
     * returns data, MIME type, encoding and error string (the first three are "" if an error occurred).
     */
    private String[] _loadViewFromGopher(String hostname, int port, String sel, char itype, String query, Handler sink) {
    	StringBuilder buf = new StringBuilder();
    	final int bytes_per_cough = 1024;
    	
    	// Pseudo-handler for about:
    	if (hostname == "about:") {
    		buf.append("<!DOCTYPE html><html><body>\n");
    		// The selector tells us what.
    		if (sel == "overbite") {
    			buf.append("<center><a href=\"gopher://gopher.floodgap.com/1/overbite\"><h3>")
    			   .append(S(R.string.app_name)).append("</h3></a>\n")
    			   .append("<h4>").append(S(R.string.about_1)).append("</h4>")
    			   .append("<i>").append(S(R.string.about_long_version)).append("</i><br>\n")
    			   .append(S(R.string.about_blurb))
    			   .append("<p>\n")
    			   .append("&copy;2010, Cameron Kaiser<br>\n")
    			   .append("&copy;2010, Contributors to Overbite<br>\n")
    			   .append(S(R.string.all_rights_reserved))
    			   .append("<br>")
    			   .append(S(R.string.bsd_license))
    			   .append("<p>")
    			   .append("<b>").append(S(R.string.visit_overbite)).append("</b><br>\n")
    			   .append("<a href=\"gopher://gopher.floodgap.com/1/overbite\">Gopher</a>")
    			   .append(" | <a href=\"http://gopher.floodgap.com/overbite/\">HTTP</a>")
    			   .append("</center>")
    			   ;
    		}
    		buf.append("</body></html>");
    		return new String[] { buf.toString(), "text/html", "utf-8", "" };
    	}
    	
     	byte[] leftover = {}; // rolling buffer for conversion
    	int byteCount = 0;
    	int lineCount = 0;
    	
    	String myMIMEType = "";
    	if (itype == 'I') {
    		if (sel.indexOf(".png") != -1 || sel.indexOf(".PNG") != -1)
    			myMIMEType = "image/png";
    		else if (sel.indexOf(".gif") != -1 || sel.indexOf(".GIF") != -1)
    			myMIMEType = "image/gif";
    		else
    			myMIMEType = "image/jpeg";
    		// any others?
    	} else myMIMEType =
    				(itype == '1' || itype == 'h' || itype == '7' || itype == '0') ? "text/html"
    				: (itype == 'g') ? "image/gif"
    				: (itype == 'p') ? "image/png"
    				: "application/octet-stream";
    	String myEncoding =
    				(itype == '0' || itype == '1' || itype == '7') ? "utf-8" : "base64";
    	
    	try {
    		sendHandler(sink, 1, 1, 0, S(R.string.loader_back_off)+" | "+S(R.string.loader_connecting));
    		
    		SOCKSHelper helper = new SOCKSHelper();
    		Socket s = helper.OpenProxiedSocket(hostname, port);
    		PrintStream out = new PrintStream(s.getOutputStream(), true);
    		DataInputStream in = new DataInputStream(s.getInputStream());
    		helper.EstablishProxiedLink(in, out, hostname, port);
    		
    		if (query.length() > 0)
    			out.print(sel+"\t"+query+"\r\n");
    		else
    			out.print(sel + "\r\n");
    		out.flush(); // paranoia
    		sendHandler(sink, 1, 100, 0, S(R.string.loader_back_off)+" | "+S(R.string.loader_waiting));
    		
    		if (myMIMEType == "text/html" && myEncoding == "utf-8") {
    			int dimension = (int)(25.0 * metrics.density); // the icons are 100x100
    			buf.append("<!DOCTYPE html><html>"
    			    + "<head><style type=\"text/css\">"
    			    + "tt { font-size:0.76em; }\n"
    			    + "table { border: 0px; }\n"
    			    + "tr.l { vertical-align: middle; text-align: left; }\n"
    			    + "tr.r { vertical-align: middle; text-align: right; }\n"
    			    + "img.i { opacity:0.5; height:"+dimension+"px; width:"+dimension+"px; border:0px; }\n"
    			    + "</style>\n"
    		        + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"></head><body>\n");
    		}
    		
    		// Picked to give reasonable responsiveness to the user, but not be too inefficient.
    		byte[] b = new byte[bytes_per_cough];
    		int i;
    		int last_state = 100;
    		while ((i = in.read(b)) > 0) {
    			if (mLoader == null) // our worker thread has exited
    				return new String[] { S(R.string.loader_canceled), "text/html", "utf-8", "Canceled" };
    			
    			byteCount += i;
    			/*// We don't need this anymore, we handle the exception directly. Left here for legacy purposes.
    			if (byteCount > 1000000) { // We need to adjust this for various devices. This is a
    										// lowest common denominator figure. Other failures are
    										// intercepted by the exception handler.
    				sendHandler(sink, 1, 10000, 0, S(R.string.loader_bufboom));
    				System.gc();
    				return new String[] { S(R.string.loader_outofmemory_long), 
    						"text/html", "utf-8", "OutOfMemory" };
    			}
    			*/
    			int progBarBase = lineCount+7000;
    			if (progBarBase > 8000)
    				progBarBase = 8000;
    			
    			sendHandler(sink, 1, (last_state+progBarBase-i)/2, 0, S(R.string.loader_back_off)+" | "+byteCount+" "+S(R.string.loader_bytes_read));
    			last_state = progBarBase-i;

    			// create our "work" array. this consists of whatever we didn't process last
    			// time, plus what we got this time.
    			byte[] work = {};
    			if (leftover.length > 0) {
    				if (i == 0) { // ghost event? well, let's see if we still have work to do
    					work = leftover;
    				} else {
    					work = new byte[leftover.length + i];
    					System.arraycopy(leftover, 0, work, 0, leftover.length);
    					System.arraycopy(b, 0, work, leftover.length, i);
    				}
    			} else {
    				// must have processed it all, or this is first time through.
    				// don't just say work = b because then it will appear to be 1024 bytes long!
    				work = new byte[i]; 
    				System.arraycopy(b, 0, work, 0, i);
    			}
    			i = work.length;
    			
    			// now do some work, if we have work to do.
     			if (i > 0) {
     				int k = 0;
     				
     				switch(itype) {
     				
     				    // Process menus manually by cutting up the bytes into string fragments
     					// and turning them into HTML (or, for 0, text).
     					case '0':
     					case '7':
     					case '1': {
     						i--;
     						k = i;
     						// find our last newline and save any characters after it because it is an incomplete line.
     						while (k > -1 && work[k] != 10)
     							k--;

     						if (k == -1) {
     								leftover = work; // no newlines at all!! save all, wait for more.
     						} else {
     							// k is now where the newline is. hold everything after. if k is i, then
     							// we landed right on it and there is nothing to copy.
     							if (k < i) {
     								leftover = new byte[i - k];
     								System.arraycopy(work, 
     										k+1,
     										leftover, 0, leftover.length);
     							} else {
     								leftover = new byte[] {}; // nothing to copy; it's a complete line.
     							}
     							k--;
     						}
     						i = k;
     						k = 0;
     						// To avoid slinging a lot of strings into GC, we add to this smaller
     						// one, and then copy to the big buffer at the end.
     						StringBuilder newBuf = new StringBuilder(4096);
     						while (i > -1 && k > -1 && k < i) { // now we have some content we need to process.
     							// grab hunks of bytes, stopping on newlines, and hand them off for processing.
     							// it doesn't matter if we have some stuck newlines because bytesToURLandDS will compensate
     							// for itype 1 + 7, and we don't care for itype 0.
     							int new_k = findNextByte(work, (byte)10, k) + 1;
     							byte[] slice;

     							if (new_k == 0) {
     								new_k = i;
     								slice = new byte[i - k];
     							} else {
     								slice = new byte[new_k - k];
     							}
 								System.arraycopy(work, k, slice, 0, slice.length);
 								if (itype == '0') {
 									String lineOfText = new String(slice);
 									newBuf.append("<tt>").append(entityFixBytes(lineOfText)).append("</tt><br>\n");
 								} else {
 									String []result = bytesToURLandDS(slice);
 									char myitype = result[2].charAt(0);
 									if (myitype != 'i') {
 										int imageIndex =
 												(result[0].indexOf("gopher://") != 0) ? 8 :
 												(myitype == '1') ? 1 :
 												(myitype == '0') ? 0 :
 												(myitype == '7') ? 3 :
 												(myitype == '8') ? 4 :
 												(myitype == 'h') ? 6 :
 												(myitype == 'I' || myitype == 'g' || myitype == 'p') ? 7 :
 												(myitype == '3') ? 2 :
 												5;
 										if (result[0].length() > 0)		
 										newBuf
										      .append("<table><tr><td class=\"r\">")
									          .append("<a href=\"").append(result[0]).append("\"><img src=\"data:image/gif;base64,")
									          .append(inlinePrefix)
										      .append(inlineIcons[imageIndex])
										      .append("\" class=\"i\"></a></td><td class=\"l\">")
 											  .append("<a href=\"").append(result[0]).append("\"><tt>")
 										      .append(result[1]).append("</tt></a></td></tr></table>\n")
 										      ;
 										else
 										newBuf
 										      .append("<table><tr><td class=\"r\">")
 										      .append("<img src=\"data:image/gif;base64,")
									          .append(inlinePrefix)
										      .append(inlineIcons[0])
										      .append("\" class=\"i\"></td><td class=\"l\"><tt>")
 										      .append(result[1]).append("</tt></td></td></tr></table>\n")
 										      ;
 									} else {
 										if (result[1].matches("^[-=]+ *$") || result[1].matches("^_+ *$"))
 											newBuf.append("<hr noshade>\n");
 										else
 											newBuf.append("<tt>").append(result[1]).append("</tt><br>\n");
 									}
 								}
     							k = new_k;
     							lineCount++;
     						}
     						buf.append(newBuf);
     						break;
     					}
     					
     					// For other item types, translate to base64 to load into the WebView.
     					// This is a bit slower, but not appreciably so.
     					default: {
     						// make it end on a multiple of 3 for Base64, load the rest into leftover.
     						leftover = new byte[] {};
     						int loadRest = (i / 3) * 3;
     						if (loadRest < i) {
     							leftover = new byte[i - loadRest];
     							System.arraycopy(work, loadRest, leftover, 0, leftover.length);
     						}
     						
     						// To avoid slinging a lot of strings into GC, we add to this smaller
     						// buffer, and then copy to the big buffer at the end. This trick is even
     						// more critical here because we are concatenating a LOT!
     						StringBuilder sb = new StringBuilder(loadRest*4/3);
     						for(k=0; k<loadRest; k+=3) {
     				            int j = ((work[k] & 0xff) << 16) +
     			                	((work[k + 1] & 0xff) << 8) + 
     			                	(work[k + 2] & 0xff);
     				            sb.append(base64code.charAt((j >> 18) & 0x3f))
     				            	.append(base64code.charAt((j >> 12) & 0x3f))
     				            	.append(base64code.charAt((j >> 6) & 0x3f))
     				            	.append(base64code.charAt(j & 0x3f));
     						}
     						buf.append(sb);
     						//buf += " >EOL" + loadRest + "." + byteCount +"< ";
     					}
     				} // end switch
     				
     			}
    		}
    		sendHandler(sink, 1, 9500, 0, S(R.string.loader_finishing));
    		s.close();
    		// handle anything stuck in leftover[]. this only applies to converted data.
    		if (leftover.length > 0) {
    			if (myEncoding == "base64") { 
    				//buf += " >EOC< ";
    				// base64: pad out leftover. it sure had better be just one or two characters
    				int paddingCount = (3 - (leftover.length % 3)) % 3;
    				int j = ((leftover[0] & 0xff) << 16);
    				if (leftover.length > 1)
    					j += ((leftover[1] & 0xff) << 8);
    				buf.append(base64code.charAt((j >> 18) & 0x3f))
                	   .append(base64code.charAt((j >> 12) & 0x3f));
    				if (paddingCount == 0 || paddingCount == 1)
    					buf.append(base64code.charAt((j >> 6) & 0x3f));
    				if (paddingCount == 0) 
                		buf.append(base64code.charAt(j & 0x3f));
    				buf.append("==".substring(0, paddingCount));
    			} else {
    				if (itype == '0') {
    					String lastLine = new String(leftover);
    					buf.append("<tt>").append(entityFixBytes(lastLine)).append("</tt>\n");
    				}
    				if (itype == '1' || itype == '7') {
    					String[] result = bytesToURLandDS(leftover);
    					buf.append("<tt>").append(result[1]).append("</tt><br>");
    				}
    			}
    		}
    		if (myMIMEType == "text/html" && myEncoding == "utf-8") {
    			buf.append("\n</body></html>");
     		}
    		sendHandler(sink, 1, 9999, 0, S(R.string.loader_processing));
    		System.gc();
    		return new String[] { buf.toString(), myMIMEType, myEncoding, "" };
    	}
    	catch (SocketException e) {
    		sendHandler(sink, 1, 10000, 0, S(R.string.loader_bufboom));
    		return new String[] { S(R.string.loader_networkfail_long), "text/html", "utf-8", "SocketException" };   	
    	}
    	catch (UnknownHostException e) {
    		sendHandler(sink, 1, 10000, 0, S(R.string.loader_bufboom));
    		return new String[] { S(R.string.loader_dnsfail_long), "text/html", "utf-8", "HostName" };
    	}
    	catch (IOException e) {
    		sendHandler(sink, 1, 10000, 0, S(R.string.loader_bufboom));
    		return new String[] { S(R.string.loader_iofail_long), "text/html", "utf-8", "IOFail" };
    	}
    	catch (OutOfMemoryError e) {
    		sendHandler(sink, 1, 10000, 0, S(R.string.loader_bufboom));
    		System.gc(); // clean up
    		return new String[] { S(R.string.loader_outofmemory_long), "text/html", "utf-8", "OutOfMemory" };
    	}
    	catch (Exception e) {
    		sendHandler(sink, 1, 10000, 0, S(R.string.loader_bufboom));
    		return new String[] { "Unexpected exception "+e.toString(), "text/html", "utf-8", "WTF" };
    	}
    }

    /* The charges against you are severe. But they could be dismissed ... if you perform a "service." 
     * THIS DOESN'T WORK YET. IT IS DISABLED IN 0.2. 0.2 will be the last version to support Android 1.5. */
    public class GopherDownloadService extends Service {
    	public class LocalBinder extends Binder {
    		GopherDownloadService getService() {
    			return GopherDownloadService.this;
    		}
    	}
    	
    	private NotificationManager mNM;
    	@Override
    	public void onCreate() {
    		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    		
    		Notification notification = new Notification(R.drawable.dl, S(R.string.download_started), System.currentTimeMillis());
    		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, OverbiteAndroid.class), 0);
    		notification.setLatestEventInfo(this, S(R.string.download_service_label), S(R.string.download_service_long), contentIntent);
    		mNM.notify(99, notification);
    		Toast.makeText(this, "I'm down", Toast.LENGTH_SHORT).show();
    	}
    	/*
    	@Override
    	public int onStartCommand(Intent intent, int flags, int startId) {
    		return START_REDELIVER_INTENT;
    	}
    	*/
    	@Override
    	public void onDestroy() {
    		// Cancel any persistent notification.
    		// mNM.cancel(R.string.local_service_started);
    		// Send a toast to say we stopped.
    		// Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    		mNM.cancel(99);
    	}
		@Override
		public IBinder onBind(Intent arg0) {
			return mBinder;
		}
		private final IBinder mBinder = new LocalBinder();
		
		
    }
    // Utility class for threads using ServiceConnection so they can interrupt their parents
	public class ThreadServiceConnection implements ServiceConnection {
		Thread parent;
		public void onServiceConnected(ComponentName classname, IBinder svc) {
			GopherDownloadService service = ((GopherDownloadService.LocalBinder)svc).getService();
			//alert("download begins");
		}
		public void onServiceDisconnected(ComponentName classname) {
			//service = null;
		}
		public void setParent(Thread x) {
			parent = x;
		}
	}

}


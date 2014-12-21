package photogallery.android.zarkorunjevac.com.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zarkorunjevac on 15/12/14.
 */
public class ThumbnailDownloader<Token> extends HandlerThread {
    public static final String TAG = ThumbnailDownloader.class.getSimpleName();
    public static final int MESSAGE_DOWNLOAD = 0;

    private LruCache<Token, Bitmap> mBitmapCache;
    private final int mMaxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private final int mCacheSize = mMaxMemory / 8;
    Handler mHandler;
    Map<Token, String> requestMap;

    {
        requestMap = Collections.synchronizedMap(new HashMap<Token, String>());
    }


    Handler mResponseHandler;
    Listener<Token> mListener;

    public interface Listener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    public void setListener(Listener<Token> listener) {
        mListener = listener;
    }

   /* public ThumbnailDownloader(){
        super(TAG);
    }*/

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;

        mBitmapCache = new LruCache<Token, Bitmap>(mCacheSize) {


            @Override
            protected int sizeOf(Token key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    @SuppressLint("HanlerLeak")
    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new Handler() {


            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    Token token = (Token) msg.obj;
                    Log.i(TAG, "Got a request for url: " + requestMap.get(token));
                    handleRequest(token);
                }
            }
        };
    }

    public void queueThumbnail(Token token, String url) {
        Log.i(TAG, "Got an URL: " + url);
        requestMap.put(token, url);

        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token)
                .sendToTarget();
    }

    private void handleRequest(final Token token) {
        try {
            final String url = requestMap.get(token);
            final Bitmap bitmap;
            if (null == url) {
                return;
            }
            if (null == getBitmapFromMemoryCache(token)) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                addBitmapToMemoryCache(token, bitmap);
                Log.i(TAG, "Bitmap created");
            } else {
                bitmap = getBitmapFromMemoryCache(token);
                Log.i(TAG, "Bitmap from cache");
            }


            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (requestMap.get(token) != url) {
                        return;
                    }
                    requestMap.remove(token);
                    mListener.onThumbnailDownloaded(token, bitmap);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void clearQueue() {
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();

    }

    public void addBitmapToMemoryCache(Token token, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(token) == null) {
            mBitmapCache.put(token, bitmap);
        }
    }

    public Bitmap getBitmapFromMemoryCache(Token token) {
        return mBitmapCache.get(token);
    }
}

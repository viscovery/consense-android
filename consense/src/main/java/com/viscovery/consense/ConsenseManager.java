package com.viscovery.consense;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsLoader.AdsLoadedListener;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener;
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ConsenseManager implements
        AdErrorListener, AdsLoadedListener, AdEventListener, ContentProgressProvider {
    public interface ConsensePlayer {
        void setVideoPath(String path);
        void pause();
        void resume();
        int getCurrentPosition();
        int getDuration();
    }

    private class DownloadAsyncTask extends AsyncTask<String, Void, String> {
        private static final String URL_FORMAT =
                "https://vsp.viscovery.com/api/vmap?api_key=%s&video_url=%s&platform=mobile";

        @Override
        protected String doInBackground(String... params) {
            final byte[] data = params[0].getBytes();
            final String encoded = Base64.encodeToString(data, Base64.DEFAULT);
            final String url = String.format(URL_FORMAT, mApiKey, encoded);
            Log.d(TAG, url);
            try {
                final HttpURLConnection connection =
                        (HttpURLConnection) new URL(url).openConnection();
                final int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    switch (code) {
                        case HttpURLConnection.HTTP_UNAUTHORIZED:
                            Log.e(TAG, "Unauthorized");
                            break;
                        case HttpURLConnection.HTTP_NO_CONTENT:
                            Log.e(TAG, "Not found");
                            break;
                        default:
                            break;
                    }
                    return null;
                }

                final InputStream stream = connection.getInputStream();
                return new Scanner(stream).useDelimiter("\\A").next();
            } catch (IOException e) {
                final String message = e.getMessage();
                Log.e(TAG, message);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                return;
            }

            Log.d(TAG, result);
            try {
                final JSONObject object = new JSONObject(result);
                mContent = object.getString("context");
                if (mStarted) {
                    requestAds();
                }
            } catch (JSONException e) {
                final String message = e.getMessage();
                Log.e(TAG, message);
            }
        }
    }

    private static final String TAG = "ConsenseManager";

    private final ConsensePlayer mPlayer;
    private final String mApiKey;
    private final ImaSdkFactory mSdkFactory;
    private final AdsLoader mAdsLoader;
    private final AdDisplayContainer mAdDisplayContainer;

    private boolean mStarted;
    private String mContent;
    private AdsManager mAdsManager;
    private boolean mActive;

    public ConsenseManager(
            Context context, ViewGroup container, ConsensePlayer player, String apiKey) {
        mPlayer = player;
        mApiKey = apiKey;
        mSdkFactory = ImaSdkFactory.getInstance();
        mAdsLoader = mSdkFactory.createAdsLoader(context);
        mAdsLoader.addAdErrorListener(this);
        mAdsLoader.addAdsLoadedListener(this);
        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setAdContainer(container);
    }

    public void setVideoPath(String path) {
        if (mPlayer != null) {
            mPlayer.setVideoPath(path);
        }

        new DownloadAsyncTask().execute(path);
    }

    public void start() {
        if (mStarted) {
            return;
        }

        mStarted = true;
        requestAds();
    }

    public void pause() {
        if (mAdsManager != null && mActive) {
            mAdsManager.pause();
        } else if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void resume() {
        if (mAdsManager != null && mActive) {
            mAdsManager.resume();
        } else if (mPlayer != null) {
            mPlayer.resume();
        }
    }

    @Override
    public void onAdError(AdErrorEvent event) {
        Log.e(TAG, event.getError().getMessage());
    }

    @Override
    public void onAdsManagerLoaded(AdsManagerLoadedEvent event) {
        mAdsManager = event.getAdsManager();
        mAdsManager.addAdErrorListener(this);
        mAdsManager.addAdEventListener(this);
        mAdsManager.init();
    }

    @Override
    public void onAdEvent(AdEvent event) {
        final AdEventType type = event.getType();
        Log.d(TAG, type.toString());
        switch (type) {
            case LOG:
                Log.d(TAG, event.getAdData().toString());
                break;
            case LOADED:
                Log.d(TAG, event.getAd().toString());
                mAdsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                if (mPlayer != null) {
                    mPlayer.pause();
                }
                mActive = true;
                break;
            case CONTENT_RESUME_REQUESTED:
                if (mPlayer != null) {
                    mPlayer.resume();
                }
                mActive = false;
                break;
            case ALL_ADS_COMPLETED:
                destroy();
                break;
            default:
                break;
        }
    }

    @Override
    public VideoProgressUpdate getContentProgress() {
        if (mActive || mPlayer == null) {
            return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        final int currentPosition = mPlayer.getCurrentPosition();
        final int duration = mPlayer.getDuration();
        return new VideoProgressUpdate(currentPosition, duration);
    }

    private void destroy() {
        if (mAdsManager != null) {
            mAdsManager.destroy();
            mAdsManager = null;
        }
    }

    private void requestAds() {
        if (mContent == null) {
            return;
        }

        destroy();
        final AdsRequest request = mSdkFactory.createAdsRequest();
        request.setAdsResponse(mContent);
        request.setAdDisplayContainer(mAdDisplayContainer);
        request.setContentProgressProvider(this);
        mAdsLoader.requestAds(request);
    }
}

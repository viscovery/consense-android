package com.viscovery.ad;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

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
import com.squareup.picasso.Picasso;
import com.viscovery.ad.api.VmapResponse;
import com.viscovery.ad.api.VspService;
import com.viscovery.ad.vmap.Extension;
import com.viscovery.ad.vmap.OutstreamExtension;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdSdkManager implements
        AdErrorListener,
        AdEventListener,
        AdsLoadedListener,
        ContentProgressProvider,
        View.OnClickListener {
    public interface AdSdkPlayer {
        void setVideoPath(String path);
        void pause();
        void resume();
        int getCurrentPosition();
        int getDuration();
    }

    private class VastAsyncTask extends AsyncTask<String, Void, String> {
        private String mTimeOffset;
        private String mType;

        VastAsyncTask(String timeOffset, String type) {
            mTimeOffset = timeOffset;
            mType = type;
        }

        @Override
        protected String doInBackground(String... params) {
            final String url = params[0];
            Log.d(TAG, url);
            try {
                final HttpURLConnection connection =
                        (HttpURLConnection) new URL(url).openConnection();
                final int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
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

            try {
                final NonLinear nonLinear = VastParser.parse(result);
                if (nonLinear != null) {
                    nonLinear.setType(mType);
                    final Calendar calendar = Calendar.getInstance(Locale.US);
                    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                            "HH:mm:ss.SSS", Locale.US);
                    calendar.setTime(simpleDateFormat.parse(mTimeOffset));
                    final int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    final int minute = calendar.get(Calendar.MINUTE);
                    final int second = calendar.get(Calendar.SECOND);
                    final int millisecond = calendar.get(Calendar.MILLISECOND);
                    final double key = hour * 3600 + minute * 60 + second + millisecond / 1000.0;
                    mNonLinears.put(key, nonLinear);
                }
            } catch (IOException|ParseException|XmlPullParserException e) {
            }
        }
    }

    private static final String TAG = "AdSdkManager";

    private final Context mContext;
    private final AdSdkPlayer mPlayer;
    private final String mApiKey;
    private final ImaSdkFactory mSdkFactory;
    private final AdsLoader mAdsLoader;
    private final AdDisplayContainer mAdDisplayContainer;
    private final RelativeLayout mContainerLayout;
    private final ImageView mAdView;
    private final ImageView mCloseView;
    private final VspService mVspService;

    private boolean mStarted;
    private String mContent;
    private AdsManager mAdsManager;
    private boolean mActive;
    private HashMap<Double, NonLinear> mNonLinears = new HashMap<>();
    private String mClickThroughUrl;
    private String mClickTrackingUrl;
    private Picasso mPicasso;

    private ImageView mOutstreamView;
    private ImageView mCloseOutstreamView;

    public AdSdkManager(
            Context context, ViewGroup container, AdSdkPlayer player, String apiKey) {
        mContext = context;
        mPlayer = player;
        mApiKey = apiKey;
        mSdkFactory = ImaSdkFactory.getInstance();
        mAdsLoader = mSdkFactory.createAdsLoader(context);
        mAdsLoader.addAdErrorListener(this);
        mAdsLoader.addAdsLoadedListener(this);
        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setAdContainer(container);

        mContainerLayout = new RelativeLayout(context);
        final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(mContainerLayout, layoutParams);

        mAdView = new ImageView(context);
        mAdView.setId(R.id.ad);
        mAdView.setAdjustViewBounds(true);
        mAdView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mAdView.setOnClickListener(this);
        mCloseView = new ImageView(context);
        mCloseView.setId(R.id.close);
        mCloseView.setImageResource(R.drawable.btn_close);
        mCloseView.setOnClickListener(this);

        mPicasso = Picasso.with(context);
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://vsp.viscovery.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mVspService = retrofit.create(VspService.class);
    }

    public void setOutstreamContainer(ViewGroup container) {
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.outstream, null);
        container.addView(layout);

        mOutstreamView = (ImageView) layout.findViewById(R.id.outstream);
        mOutstreamView.setOnClickListener(this);
        mCloseOutstreamView = (ImageView) layout.findViewById(R.id.close);
        mCloseOutstreamView.setOnClickListener(this);
    }

    public void setVideoPath(String path) {
        if (mPlayer != null) {
            mPlayer.setVideoPath(path);
        }

        final String videoUrl = Base64.encodeToString(path.getBytes(), Base64.DEFAULT);
        final Call<VmapResponse> call = mVspService.getVmap(mApiKey, videoUrl);
        call.enqueue(new Callback<VmapResponse>() {
            @Override
            public void onResponse(Call<VmapResponse> call, Response<VmapResponse> response) {
                final VmapResponse vmapResponse = response.body();
                if (vmapResponse != null) {
                    mContent = vmapResponse.getContent();
                    try {
                        final List<AdBreak> adBreaks = VmapParser.parse(mContent);
                        for (AdBreak adBreak : adBreaks) {
                            final String timeOffset = adBreak.getTimeOffset();
                            final String breakType = adBreak.getBreakType();
                            final String url = adBreak.getAdSource().getAdTagUri().getUrl();
                            boolean outstream = false;
                            for (Extension extension : adBreak.getExtensions()) {
                                if (extension instanceof OutstreamExtension) {
                                    outstream = true;
                                }
                            }
                            if (breakType.equals(AdBreak.BREAK_TYPE_NONLINEAR)) {
                                new VastAsyncTask(timeOffset, outstream
                                        ? NonLinear.TYPE_OUTSTREAM
                                        : NonLinear.TYPE_INSTREAM).execute(url);
                            }
                        }
                    } catch (IOException|XmlPullParserException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    if (mStarted) {
                        requestAds();
                    }
                }
            }

            @Override
            public void onFailure(Call<VmapResponse> call, Throwable t) {
            }
        });
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
    public void onAdEvent(AdEvent event) {
        final AdEventType type = event.getType();
        Log.d(TAG, type.toString());
        switch (type) {
            case LOG:
                Log.d(TAG, event.getAdData().toString());
                break;
            case LOADED:
                Log.d(TAG, event.getAd().toString());
                closeAd();
                final double key = event.getAd().getAdPodInfo().getTimeOffset();
                if (mNonLinears.containsKey(key)) {
                    final NonLinear nonLinear = mNonLinears.get(key);
                    if (nonLinear.getType().equals(NonLinear.TYPE_OUTSTREAM)) {
                        if (mOutstreamView != null) {
                            mClickThroughUrl = nonLinear.getClickThroughUrl();
                            mClickTrackingUrl = nonLinear.getClickTrackingUrl();
                            final String path = nonLinear.getResourceUrl();
                            mPicasso.load(path).into(mOutstreamView);
                            mCloseOutstreamView.setVisibility(View.VISIBLE);
                        }
                        return;
                    }
                    final Map<String, String> parameters = parseParameters(
                            nonLinear.getAdParameters());

                    final int layoutWidth = mContainerLayout.getWidth();
                    final int layoutHeight = mContainerLayout.getHeight();
                    final int nonLinearWidth = nonLinear.getWidth();
                    final int nonLinearHeight = nonLinear.getHeight();
                    final int heightPercentage = Integer.parseInt(parameters.get("height"));
                    final int bottomPercentage = Integer.parseInt(parameters.get("pos_value"));
                    final int height = layoutHeight * heightPercentage / 100;
                    final int width = height * nonLinearWidth / nonLinearHeight;

                    final RelativeLayout.LayoutParams adLayoutParams =
                            new RelativeLayout.LayoutParams(width, height);
                    adLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    int left = 0;
                    int bottom = layoutHeight * bottomPercentage / 100;
                    if (parameters.get("align").equals("center")) {
                        adLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    } else {
                        adLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        final int leftPercentage = Integer.parseInt(parameters.get("align_value"));
                        left = layoutWidth * leftPercentage / 100;
                    }
                    adLayoutParams.setMargins(left, 0, 0, bottom);
                    mContainerLayout.addView(mAdView, adLayoutParams);

                    final RelativeLayout.LayoutParams closeLayoutParams =
                            new RelativeLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                    closeLayoutParams.setMargins(
                            0,
                            mContext.getResources().getDimensionPixelSize(R.dimen.btn_offset_vertical),
                            mContext.getResources().getDimensionPixelSize(R.dimen.btn_offset_horizontal),
                            0);
                    closeLayoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.ad);
                    closeLayoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.ad);
                    mContainerLayout.addView(mCloseView, closeLayoutParams);

                    mClickThroughUrl = nonLinear.getClickThroughUrl();
                    mClickTrackingUrl = nonLinear.getClickTrackingUrl();
                    final String path = nonLinear.getResourceUrl();
                    mPicasso.load(path).into(mAdView);
                } else {
                    mAdsManager.start();
                }
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
    public void onAdsManagerLoaded(AdsManagerLoadedEvent event) {
        mAdsManager = event.getAdsManager();
        mAdsManager.addAdErrorListener(this);
        mAdsManager.addAdEventListener(this);
        mAdsManager.init();
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

    @Override
    public void onClick(View v) {
        if (v == mAdView || v == mOutstreamView) {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mClickThroughUrl));
            mContext.startActivity(intent);
            closeAd();
        } else if (v == mCloseView || v == mCloseOutstreamView) {
            closeAd();
        }
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

    private void closeAd() {
        mContainerLayout.removeView(mAdView);
        mContainerLayout.removeView(mCloseView);
        if (mOutstreamView != null) {
            mOutstreamView.setImageDrawable(null);
            mCloseOutstreamView.setVisibility(View.GONE);
        }
    }

    private Map<String, String> parseParameters(String parameters) {
        HashMap<String, String> mapping = new HashMap<>();
        for (final String parameter : parameters.split(",")) {
            final String[] elements = parameter.split("=");
            mapping.put(elements[0], elements[1]);
        }
        return mapping;
    }
}

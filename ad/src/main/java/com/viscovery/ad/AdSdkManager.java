package com.viscovery.ad;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.viscovery.ad.api.MockService;
import com.viscovery.ad.api.VmapResponse;
import com.viscovery.ad.api.VspService;
import com.viscovery.ad.vast.NonLinear;
import com.viscovery.ad.vast.Vast;
import com.viscovery.ad.vast.VastService;
import com.viscovery.ad.vmap.Extension;
import com.viscovery.ad.vmap.AdBreak;
import com.viscovery.ad.vmap.Vmap;
import com.viscovery.ad.vmap.VmapTypeAdapter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

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

    private class VastCallback implements Callback<Vast> {
        private AdBreak mAdBreak;

        public VastCallback(AdBreak adBreak) {
            mAdBreak = adBreak;
        }

        @Override
        public void onResponse(Call<Vast> call, Response<Vast> response) {
            final Vast vast = response.body();
            if (vast != null) {
                final NonLinear nonLinear = vast.getAd().getInLine().getNonLinear();
                if (nonLinear != null) {
                    try {
                        final double key = parseTimeOffset(mAdBreak.getTimeOffset());
                        mNonLinears.put(key, nonLinear);
                    } catch (ParseException e) {
                    }
                }
            }
        }

        @Override
        public void onFailure(Call<Vast> call, Throwable t) {
        }
    }

    private static final String TAG = "AdSdkManager";

    private final Context mContext;
    private final AdSdkPlayer mPlayer;
    private final String mApiKey;
    private final boolean mMock;
    private final ImaSdkFactory mSdkFactory;
    private final AdsLoader mAdsLoader;
    private final AdDisplayContainer mAdDisplayContainer;
    private final ViewGroup mInstreamView;
    private final Guideline mInstreamLeftGuideline;
    private final Guideline mInstreamTopGuideline;
    private final Guideline mInstreamRightGuideline;
    private final Guideline mInstreamBottomGuideline;
    private final ImageView mInstreamAdView;
    private final ImageView mInstreamCloseView;
    private final VspService mVspService;
    private final MockService mMockService;
    private final Callback<VmapResponse> mVmapResponseCallback = new Callback<VmapResponse>() {
        @Override
        public void onResponse(Call<VmapResponse> call, Response<VmapResponse> response) {
            final VmapResponse vmapResponse = response.body();
            if (vmapResponse != null) {
                mContent = vmapResponse.getContent();
                final Serializer serializer = new Persister();
                try {
                    final Vmap vmap = serializer.read(Vmap.class, mContent);
                    for (AdBreak adBreak : vmap.getAdBreaks()) {
                        try {
                            final double key = parseTimeOffset(adBreak.getTimeOffset());
                            mAdBreaks.put(key, adBreak);
                        } catch (ParseException e) {
                            continue;
                        }

                        final Call<Vast> vastCall = mVastService.getDocument(
                                adBreak.getAdSource().getAdTagUri().getValue());
                        vastCall.enqueue(new VastCallback(adBreak));
                    }
                } catch (Exception e) {
                }

                if (mStarted) {
                    requestAds();
                }
            }
        }

        @Override
        public void onFailure(Call<VmapResponse> call, Throwable t) {
            Log.d(TAG, "onFailure");
            Log.d(TAG, t.getMessage());
        }
    };

    private boolean mStarted;
    private String mContent;
    private AdsManager mAdsManager;
    private boolean mActive;
    private HashMap<Double, AdBreak> mAdBreaks = new HashMap<>();
    private HashMap<Double, NonLinear> mNonLinears = new HashMap<>();
    private String mClickThroughUrl;
    private String mClickTrackingUrl;
    private Picasso mPicasso;

    private ImageView mOutstreamView;
    private ImageView mCloseOutstreamView;

    private VastService mVastService;

    public AdSdkManager(Context context, ViewGroup container, AdSdkPlayer player, String apiKey) {
        this(context, container, player, apiKey, false);
    }

    public AdSdkManager(
            Context context, ViewGroup container, AdSdkPlayer player, String apiKey, boolean mock) {
        mContext = context;
        mPlayer = player;
        mApiKey = apiKey;
        mMock = mock;
        mSdkFactory = ImaSdkFactory.getInstance();
        mAdsLoader = mSdkFactory.createAdsLoader(context);
        mAdsLoader.addAdErrorListener(this);
        mAdsLoader.addAdsLoadedListener(this);
        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setAdContainer(container);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.instream, container);
        mInstreamView = (ViewGroup) container.findViewById(R.id.instream);
        mInstreamLeftGuideline = (Guideline) mInstreamView.findViewById(R.id.left);
        mInstreamTopGuideline = (Guideline) mInstreamView.findViewById(R.id.top);
        mInstreamRightGuideline = (Guideline) mInstreamView.findViewById(R.id.right);
        mInstreamBottomGuideline = (Guideline) mInstreamView.findViewById(R.id.bottom);
        mInstreamAdView = (ImageView) mInstreamView.findViewById(R.id.ad);
        mInstreamAdView.setOnClickListener(this);
        mInstreamCloseView = (ImageView) mInstreamView.findViewById(R.id.close);
        mInstreamCloseView.setOnClickListener(this);

        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Vmap.class, new VmapTypeAdapter())
                .create();
        mPicasso = Picasso.with(context);
        if (mMock) {
            final Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://www.mocky.io/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            mVspService = null;
            mMockService = retrofit.create(MockService.class);
        } else {
            final Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://vsp.viscovery.com/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            mVspService = retrofit.create(VspService.class);
            mMockService = null;
        }
        mVastService = new Retrofit.Builder()
                .baseUrl("https://vsp.viscovery.com/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
                .create(VastService.class);
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

        final Call<VmapResponse> call;
        if (mMock) {
            call = mMockService.getVmap("http://www.mocky.io/v2/594256ef120000ed04ddc3fb");
        } else {
            final String videoUrl = Base64.encodeToString(path.getBytes(), Base64.DEFAULT);
            call = mVspService.getVmapByUrl(mApiKey, videoUrl);
        }
        call.enqueue(mVmapResponseCallback);
    }

    public void setVideoId(String id) {
        final Call<VmapResponse> call;
        if (mMock) {
            call = mMockService.getVmap("http://www.mocky.io/v2/594256ef120000ed04ddc3fb");
        } else {
            call = mVspService.getVmapById(mApiKey, id);
        }
        call.enqueue(mVmapResponseCallback);
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
                if (mAdBreaks.containsKey(key)) {
                    final AdBreak adBreak = mAdBreaks.get(key);
                    final NonLinear nonLinear = mNonLinears.get(key);
                    if (nonLinear == null) {
                        return;
                    }

                    final boolean isOutstream = adBreak.getExtensions().size() > 0
                            && adBreak.getExtensions().get(0).getType().equals(Extension.TYPE_OUTSTREAM);
                    if (isOutstream) {
                        if (mOutstreamView != null) {
                            mClickThroughUrl = nonLinear.getNonLinearClickThrough();
                            mClickTrackingUrl = nonLinear.getNonLinearClickTracking();
                            final String path = nonLinear.getStaticResource();
                            mPicasso.load(path).into(mOutstreamView);
                            mCloseOutstreamView.setVisibility(View.VISIBLE);
                        }
                        return;
                    }
                    final Map<String, String> parameters = parseParameters(
                            nonLinear.getAdParameters());

                    int heightPercentage;
                    try {
                        heightPercentage = Integer.parseInt(parameters.get("height"));
                    } catch (NumberFormatException e) {
                        heightPercentage = 100;
                    }
                    final int bottomPercentage = Integer.parseInt(parameters.get("pos_value"));


                    final ConstraintLayout.LayoutParams topLayoutParams =
                            (ConstraintLayout.LayoutParams) mInstreamTopGuideline.getLayoutParams();
                    topLayoutParams.guidePercent =
                            (float) ((100 - heightPercentage - bottomPercentage) / 100.0);
                    mInstreamTopGuideline.setLayoutParams(topLayoutParams);

                    final ConstraintLayout.LayoutParams bottomLayoutParams =
                            (ConstraintLayout.LayoutParams) mInstreamBottomGuideline.getLayoutParams();
                    bottomLayoutParams.guidePercent = (float) ((100 - bottomPercentage) / 100.0);
                    mInstreamBottomGuideline.setLayoutParams(bottomLayoutParams);

                    final ConstraintLayout.LayoutParams leftLayoutParams =
                            (ConstraintLayout.LayoutParams) mInstreamLeftGuideline.getLayoutParams();
                    final ConstraintLayout.LayoutParams rightLayoutParams =
                            (ConstraintLayout.LayoutParams) mInstreamRightGuideline.getLayoutParams();
                    final ConstraintLayout.LayoutParams adLayoutParams =
                            (ConstraintLayout.LayoutParams) mInstreamAdView.getLayoutParams();
                    if (parameters.get("align").equals("center")) {
                        mInstreamAdView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        leftLayoutParams.guidePercent = 0;
                        rightLayoutParams.guidePercent = 1;
                        adLayoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
                        adLayoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
                    } else {
                        mInstreamAdView.setScaleType(ImageView.ScaleType.FIT_START);
                        final int leftPercentage = Integer.parseInt(parameters.get("align_value"));
                        leftLayoutParams.guidePercent = (float) (leftPercentage / 100.0);
                        rightLayoutParams.guidePercent = 1 - leftLayoutParams.guidePercent;
                        adLayoutParams.leftToLeft = mInstreamLeftGuideline.getId();
                        adLayoutParams.rightToRight = ConstraintLayout.LayoutParams.UNSET;
                    }
                    mInstreamLeftGuideline.setLayoutParams(leftLayoutParams);
                    mInstreamRightGuideline.setLayoutParams(rightLayoutParams);
                    mInstreamAdView.setLayoutParams(adLayoutParams);

                    mClickThroughUrl = nonLinear.getNonLinearClickThrough();
                    mClickTrackingUrl = nonLinear.getNonLinearClickTracking();
                    final String path = nonLinear.getStaticResource();
                    mPicasso.load(path).into(mInstreamAdView);
                    mInstreamCloseView.setVisibility(View.VISIBLE);
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
        if (v == mInstreamAdView || v == mOutstreamView) {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mClickThroughUrl));
            mContext.startActivity(intent);
            closeAd();
        } else if (v == mInstreamCloseView || v == mCloseOutstreamView) {
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
        mInstreamAdView.setImageDrawable(null);
        mInstreamCloseView.setVisibility(View.GONE);
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

    private double parseTimeOffset(String timeOffset) throws ParseException {
        final Calendar calendar = Calendar.getInstance(Locale.US);
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                "HH:mm:ss.SSS", Locale.US);
        calendar.setTime(simpleDateFormat.parse(timeOffset));
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        final int minute = calendar.get(Calendar.MINUTE);
        final int second = calendar.get(Calendar.SECOND);
        final int millisecond = calendar.get(Calendar.MILLISECOND);
        return hour * 3600 + minute * 60 + second + millisecond / 1000.0;
    }
}

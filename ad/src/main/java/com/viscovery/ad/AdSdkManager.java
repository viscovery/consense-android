package com.viscovery.ad;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.viscovery.ad.api.MockService;
import com.viscovery.ad.api.VmapResponse;
import com.viscovery.ad.api.VspService;
import com.viscovery.ad.vast.InLine;
import com.viscovery.ad.vast.Linear;
import com.viscovery.ad.vast.MediaFile;
import com.viscovery.ad.vast.NonLinear;
import com.viscovery.ad.vast.Vast;
import com.viscovery.ad.vast.VastService;
import com.viscovery.ad.vmap.Extension;
import com.viscovery.ad.vmap.AdBreak;
import com.viscovery.ad.vmap.Horizontal;
import com.viscovery.ad.vmap.Placement;
import com.viscovery.ad.vmap.Size;
import com.viscovery.ad.vmap.Vertical;
import com.viscovery.ad.vmap.Vmap;
import com.viscovery.ad.vmap.VmapTypeAdapter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class AdSdkManager implements
        MediaPlayer.OnCompletionListener,
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
                try {
                    final int key = parseTimeOffset(mAdBreak.getTimeOffset());
                    final InLine inLine = vast.getAd().getInLine();
                    final Linear linear = inLine.getLinear();
                    if (linear != null) {
                        mAds.put(key, linear);
                    }
                    final NonLinear nonLinear = inLine.getNonLinear();
                    if (nonLinear != null) {
                        mAds.put(key, nonLinear);
                    }
                } catch (ParseException e) {
                }
            }
        }

        @Override
        public void onFailure(Call<Vast> call, Throwable t) {
        }
    }

    private static final String TAG = "AdSdkManager";
    private static final int POSITION_CHECK = 0;

    private final Context mContext;
    private final AdSdkPlayer mPlayer;
    private final String mApiKey;
    private final boolean mMock;
    private final ViewGroup mInstreamView;
    private final VideoView mInstreamLinearView;
    private final TextView mInstreamRemainingView;
    private final TextView mInstreamSkipView;
    private final TextView mInstreamAboutView;
    private final Guideline mInstreamLeftGuideline;
    private final Guideline mInstreamTopGuideline;
    private final Guideline mInstreamRightGuideline;
    private final Guideline mInstreamBottomGuideline;
    private final ImageView mInstreamNonLinearView;
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
                            final int key = parseTimeOffset(adBreak.getTimeOffset());
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
            }
        }

        @Override
        public void onFailure(Call<VmapResponse> call, Throwable t) {
        }
    };
    private final Callback<Void> mVastTrackingCallback = new Callback<Void>() {
        @Override
        public void onResponse(Call<Void> call, Response<Void> response) {
            Log.d(TAG, String.format("vast tracking succeeded: %d", response.code()));
        }

        @Override
        public void onFailure(Call<Void> call, Throwable t) {
            Log.e(TAG, String.format("vast tracking failed: %s", t.getMessage()));
        }
    };
    private final Timer mTimer = new Timer();
    private final TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            mPositionHandler.obtainMessage().sendToTarget();
        }
    };
    private final Handler mPositionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mActive) {
                final int position = mInstreamLinearView.getCurrentPosition();
                final int remaining = (mInstreamLinearView.getDuration() - position) / 1000;
                final String arg =
                        String.format(Locale.US, "%d:%02d", remaining / 60, remaining % 60);
                final String text = mContext.getString(R.string.remaining, arg);
                mInstreamRemainingView.setText(text);

                if (mSkipOffset > 0) {
                    if (position >= mSkipOffset) {
                        mInstreamSkipView.setText(R.string.skip);
                        mInstreamSkipView.setCompoundDrawablesWithIntrinsicBounds(
                                0, 0, R.drawable.ic_skip, 0);
                        mInstreamSkipView.setVisibility(View.VISIBLE);
                        mInstreamSkipView.setOnClickListener(AdSdkManager.this);
                    } else {
                        final int skipOffset = (mSkipOffset - position) / 1000;
                        final String skippable = mContext.getString(R.string.skippable, skipOffset);
                        mInstreamSkipView.setText(skippable);
                        mInstreamSkipView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        mInstreamSkipView.setVisibility(View.VISIBLE);
                        mInstreamSkipView.setOnClickListener(null);
                    }
                }
                return;
            }

            final int currentPosition = mPlayer.getCurrentPosition();
            final Integer[] keys = mAds.keySet().toArray(new Integer[0]);
            Arrays.sort(keys);
            for (int key : keys) {
                if ((mLastPosition < key && currentPosition >= key)
                        || (mLastPosition <= key && currentPosition > key)) {
                    final AdBreak adBreak = mAdBreaks.get(key);
                    final Object ad = mAds.get(key);
                    closeAds();
                    if (ad instanceof Linear) {
                        showInstreamLinear((Linear) ad);
                    } else if (ad instanceof NonLinear) {
                        Placement placement = null;
                        Horizontal horizontal = null;
                        Vertical vertical = null;
                        Size size = null;
                        for (Extension extension : adBreak.getExtensions()) {
                            switch (extension.getType()) {
                                case Extension.TYPE_POSITION:
                                    for (Object value : extension.getValues()) {
                                        if (value instanceof Placement) {
                                            placement = (Placement) value;
                                        } else if (value instanceof Horizontal) {
                                            horizontal = (Horizontal) value;
                                        } else if (value instanceof  Vertical) {
                                            vertical = (Vertical) value;
                                        }
                                    }
                                    break;
                                case Extension.TYPE_SIZE:
                                    for (Object value : extension.getValues()) {
                                        if (value instanceof Size) {
                                            size = (Size) value;
                                        }
                                    }
                                    break;
                            }
                        }
                        final NonLinear nonLinear = (NonLinear) ad;
                        if (placement != null
                                && placement.getType().equals(Placement.TYPE_OUTSTREAM)) {
                            showOutstreamNonLinear(nonLinear);
                        } else {
                            showInstreamNonLinear(nonLinear, horizontal, vertical, size);
                        }
                    }
                    break;
                }
            }
            mLastPosition = currentPosition;
        }
    };

    private boolean mStarted;
    private String mContent;
    private boolean mActive;
    private int mSkipOffset;
    private int mLastPosition;
    private HashMap<Integer, AdBreak> mAdBreaks = new HashMap<>();
    private HashMap<Integer, Object> mAds = new HashMap<>();
    private String mClickThroughUrl;
    private String mClickTrackingUrl;
    private Picasso mPicasso;

    private ImageView mOutstreamNonLinearView;
    private ImageView mOutstreamCloseView;

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

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.instream, container);
        mInstreamView = (ViewGroup) container.findViewById(R.id.instream);
        mInstreamLinearView = (VideoView) mInstreamView.findViewById(R.id.linear);
        mInstreamLinearView.setZOrderMediaOverlay(true);
        mInstreamLinearView.setOnCompletionListener(this);
        mInstreamLinearView.setOnClickListener(this);
        mInstreamRemainingView = (TextView) mInstreamView.findViewById(R.id.remaining);
        mInstreamSkipView = (TextView) mInstreamView.findViewById(R.id.skip);
        mInstreamAboutView = (TextView) mInstreamView.findViewById(R.id.about);
        mInstreamAboutView.setOnClickListener(this);
        mInstreamLeftGuideline = (Guideline) mInstreamView.findViewById(R.id.left);
        mInstreamTopGuideline = (Guideline) mInstreamView.findViewById(R.id.top);
        mInstreamRightGuideline = (Guideline) mInstreamView.findViewById(R.id.right);
        mInstreamBottomGuideline = (Guideline) mInstreamView.findViewById(R.id.bottom);
        mInstreamNonLinearView = (ImageView) mInstreamView.findViewById(R.id.nonLinear);
        mInstreamNonLinearView.setOnClickListener(this);
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

        mOutstreamNonLinearView = (ImageView) layout.findViewById(R.id.outstream);
        mOutstreamNonLinearView.setOnClickListener(this);
        mOutstreamCloseView = (ImageView) layout.findViewById(R.id.close);
        mOutstreamCloseView.setOnClickListener(this);
    }

    public void setVideoPath(String path) {
        if (mPlayer != null) {
            mPlayer.setVideoPath(path);
        }

        final Call<VmapResponse> call;
        if (mMock) {
            call = mMockService.getVmap("http://www.mocky.io/v2/593fae381000000f07cd101e");
        } else {
            final String videoUrl = Base64.encodeToString(path.getBytes(), Base64.DEFAULT);
            call = mVspService.getVmapByUrl(mApiKey, videoUrl);
        }
        call.enqueue(mVmapResponseCallback);
    }

    public void setVideoId(String id) {
        final Call<VmapResponse> call;
        if (mMock) {
            call = mMockService.getVmap("http://www.mocky.io/v2/593fae381000000f07cd101e");
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
        mTimer.schedule(mTimerTask, 0, 100);
    }

    public void pause() {
        if (mActive) {
            mInstreamLinearView.pause();
            mTimer.cancel();
        } else if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void resume() {
        if (mActive) {
            mInstreamLinearView.resume();
            mTimer.schedule(mTimerTask, 0, 100);
        } else if (mPlayer != null) {
            mPlayer.resume();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        skipInstreamLinear();
    }

    @Override
    public void onClick(View v) {
        if (v == mInstreamSkipView) {
            skipInstreamLinear();
        } else if (v == mInstreamCloseView) {
            closeInstreamNonLinear();
        } else if (v == mOutstreamCloseView) {
            closeOutstreamNonLinear();
        } else if (v == mInstreamLinearView) {
            if (mInstreamLinearView.isPlaying()) {
                mInstreamLinearView.pause();
            } else {
                mInstreamLinearView.resume();
            }
        } else if (v == mInstreamAboutView) {
            mInstreamLinearView.pause();
            final Call<Void> call = mVastService.trackEvent(mClickTrackingUrl);
            call.enqueue(mVastTrackingCallback);
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mClickThroughUrl));
            mContext.startActivity(intent);
        } else if (v == mInstreamNonLinearView || v == mOutstreamNonLinearView) {
            closeAds();
            final Call<Void> call = mVastService.trackEvent(mClickTrackingUrl);
            call.enqueue(mVastTrackingCallback);
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mClickThroughUrl));
            mContext.startActivity(intent);
        }
    }

    private void showInstreamLinear(Linear linear) {
        mActive = true;
        if (mPlayer != null) {
            mPlayer.pause();
        }

        final String offset = linear.getSkipOffset();
        if (offset != null) {
            try {
                mSkipOffset = parseTimeOffset(offset);
            } catch (ParseException e) {
            }
        }

        int width = 0;
        String path = null;
        for (MediaFile file : linear.getMediaFiles()) {
            if (file.getWidth() > width) {
                width = file.getWidth();
                path = file.getValue();
            }
        }
        mInstreamLinearView.setVideoPath(path);
        mInstreamLinearView.setVisibility(View.VISIBLE);
        mInstreamRemainingView.setVisibility(View.VISIBLE);
        mInstreamAboutView.setVisibility(View.VISIBLE);
        mInstreamLinearView.start();
        mClickThroughUrl = linear.getClickThrough();
        mClickTrackingUrl = linear.getClickTracking();
    }

    private void showInstreamNonLinear(
            NonLinear nonLinear, Horizontal horizontal, Vertical vertical, Size size) {
        int heightPercentage;
        try {
            final Pattern pattern = Pattern.compile("(\\d+)%");
            final Matcher matcher = pattern.matcher(size.getValue());
            matcher.find();
            heightPercentage = Integer.parseInt(matcher.group(1));
        } catch (NullPointerException|NumberFormatException e) {
            heightPercentage = 100;
        }
        int bottomPercentage;
        try {
            final Pattern pattern = Pattern.compile("(\\d+)%");
            final Matcher matcher = pattern.matcher(vertical.getValue());
            matcher.find();
            bottomPercentage = Integer.parseInt(matcher.group(1));
        } catch (NullPointerException|NumberFormatException e) {
            bottomPercentage = 0;
        }

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
                (ConstraintLayout.LayoutParams) mInstreamNonLinearView.getLayoutParams();
        if (horizontal == null || horizontal.getType().equals(Horizontal.TYPE_CENTER)) {
            mInstreamNonLinearView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            leftLayoutParams.guidePercent = 0;
            rightLayoutParams.guidePercent = 1;
            adLayoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
            adLayoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        } else if (horizontal.getType().equals(Horizontal.TYPE_LEFT)) {
            mInstreamNonLinearView.setScaleType(ImageView.ScaleType.FIT_START);
            int leftPercentage;
            try {
                final Pattern pattern = Pattern.compile("(\\d+)%");
                final Matcher matcher = pattern.matcher(horizontal.getValue());
                matcher.find();
                leftPercentage = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                leftPercentage = 0;
            }
            leftLayoutParams.guidePercent = (float) (leftPercentage / 100.0);
            rightLayoutParams.guidePercent = 1 - leftLayoutParams.guidePercent;
            adLayoutParams.leftToLeft = mInstreamLeftGuideline.getId();
            adLayoutParams.rightToRight = ConstraintLayout.LayoutParams.UNSET;
        } else {
            mInstreamNonLinearView.setScaleType(ImageView.ScaleType.FIT_END);
            int rightPercentage;
            try {
                final Pattern pattern = Pattern.compile("(\\d+)%");
                final Matcher matcher = pattern.matcher(horizontal.getValue());
                matcher.find();
                rightPercentage = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                rightPercentage = 0;
            }
            leftLayoutParams.guidePercent = (float) (rightPercentage / 100.0);
            rightLayoutParams.guidePercent = 1 - leftLayoutParams.guidePercent;
            adLayoutParams.leftToLeft = ConstraintLayout.LayoutParams.UNSET;
            adLayoutParams.rightToRight = mInstreamRightGuideline.getId();
        }
        mInstreamLeftGuideline.setLayoutParams(leftLayoutParams);
        mInstreamRightGuideline.setLayoutParams(rightLayoutParams);
        mInstreamNonLinearView.setLayoutParams(adLayoutParams);

        mClickThroughUrl = nonLinear.getNonLinearClickThrough();
        mClickTrackingUrl = nonLinear.getNonLinearClickTracking();
        final String path = nonLinear.getStaticResource();
        mPicasso.load(path).into(mInstreamNonLinearView);
        mInstreamCloseView.setVisibility(View.VISIBLE);
    }

    private void showOutstreamNonLinear(NonLinear nonLinear) {
        if (mOutstreamNonLinearView != null) {
            mPicasso.load(nonLinear.getStaticResource()).into(mOutstreamNonLinearView);
            mOutstreamCloseView.setVisibility(View.VISIBLE);
            mClickThroughUrl = nonLinear.getNonLinearClickThrough();
            mClickTrackingUrl = nonLinear.getNonLinearClickTracking();
        }
    }

    private void skipInstreamLinear() {
        mActive = false;
        mSkipOffset = 0;
        mInstreamLinearView.stopPlayback();
        mInstreamLinearView.setVisibility(View.GONE);
        mInstreamRemainingView.setVisibility(View.GONE);
        mInstreamSkipView.setVisibility(View.GONE);
        mInstreamAboutView.setVisibility(View.GONE);

        if (mPlayer != null) {
            mPlayer.resume();
        }
    }

    private void closeInstreamNonLinear() {
        mInstreamNonLinearView.setImageDrawable(null);
        mInstreamCloseView.setVisibility(View.GONE);
    }

    private void closeOutstreamNonLinear() {
        mOutstreamNonLinearView.setImageDrawable(null);
        mOutstreamCloseView.setVisibility(View.GONE);
    }

    private void closeAds() {
        skipInstreamLinear();
        closeInstreamNonLinear();
        closeOutstreamNonLinear();
    }

    private int parseTimeOffset(String timeOffset) throws ParseException {
        final Calendar calendar = Calendar.getInstance(Locale.US);
        try {
            final SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
            calendar.setTime(simpleDateFormat.parse(timeOffset));
        } catch (ParseException e) {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
            calendar.setTime(simpleDateFormat.parse(timeOffset));
        }
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        final int minute = calendar.get(Calendar.MINUTE);
        final int second = calendar.get(Calendar.SECOND);
        final int millisecond = calendar.get(Calendar.MILLISECOND);
        return hour * 3600000 + minute * 60000 + second * 1000 + millisecond;
    }
}

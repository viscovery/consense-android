package com.viscovery.player;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.MediaController;

import com.viscovery.ad.AdSdkManager;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener{
    private static final String TAG = "SamplePlayer";
    private static final String KEY_CURRENT_POSITION = "currentPosition";
    private static final String API_KEY = "873cbd49-738d-406c-b9bc-e15588567b39";

    private VideoPlayer mPlayer;
    private MediaController mController;
    private AdSdkManager mAdSdkManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final String path = getString(R.string.video_url);
        final ViewGroup container = (ViewGroup) findViewById(R.id.container);
        final ViewGroup outstream = (ViewGroup) findViewById(R.id.outstream);
        mController = new MediaController(this, false);
        mPlayer = (VideoPlayer) findViewById(R.id.player);
        mController.setAnchorView(mPlayer);
        mPlayer.setMediaController(mController);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnErrorListener(this);
        mAdSdkManager = new AdSdkManager(this, container, mPlayer, API_KEY, true);
        mAdSdkManager.setOutstreamContainer(outstream);
        mAdSdkManager.setVideoPath(path);

        final String raw = String.format("android.resource://%s/raw/video", getPackageName());
        mPlayer.setVideoPath(raw);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mAdSdkManager.pause();
        getIntent().putExtra(KEY_CURRENT_POSITION, mPlayer.getCurrentPosition());
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.seekTo(getIntent().getIntExtra(KEY_CURRENT_POSITION, 0));
        mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                if (mAdSdkManager.isActive()) {
                    mController.hide();
                } else {
                    mController.show(0);
                }
            }
        });
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                mAdSdkManager.start();
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        final String message = String.format(
                Locale.US, "video playback failed: error %d, code %d", what, extra);
        return true;
    }
}

package com.viscovery.player;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

import com.viscovery.ad.AdSdkManager.AdSdkPlayer;

public class VideoPlayer extends VideoView implements AdSdkPlayer {
    public VideoPlayer(Context context) {
        super(context);
    }

    public VideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setVideoPath(String path) {
        super.setVideoPath(path);
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void resume() {
        super.start();
    }

    @Override
    public int getCurrentPosition() {
        return super.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return super.getDuration();
    }
}

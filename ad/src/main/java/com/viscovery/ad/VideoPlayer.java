package com.viscovery.ad;

public interface VideoPlayer {
    public interface PlayerCallback {
        void onPlay();
        void onPause();
        void onResume();
        void onComplete();
        void onError();
    }
}

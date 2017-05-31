package com.viscovery.ad;

public class AdBreak {
    public static final String BREAK_TYPE_LINEAR = "linear";
    public static final String BREAK_TYPE_NONLINEAR = "nonlinear";
    public static final String BREAK_TYPE_DISPLAY = "display";

    private String mTimeOffset;
    private String mBreakType;
    private AdSource mAdSource;

    public AdBreak(String timeOffset, String breakType) {
        mTimeOffset = timeOffset;
        mBreakType = breakType;
    }

    public String getTimeOffset() {
        return mTimeOffset;
    }

    public String getBreakType() {
        return mBreakType;
    }

    public void setAdSource(AdSource adSource) {
        mAdSource = adSource;
    }

    public AdSource getAdSource() {
        return mAdSource;
    }
}

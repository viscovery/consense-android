package com.viscovery.ad;

public class NonLinear {
    public static final String TYPE_INSTREAM = "instream";
    public static final String TYPE_OUTSTREAM = "outstream";

    private int mWidth;
    private int mHeight;
    private String mSuggestedDuration;
    private String mResourceUrl;
    private String mClickThroughUrl;
    private String mClickTrackingUrl;
    private String mAdParameters;
    private String mType = TYPE_INSTREAM;

    public NonLinear(int width, int height, String resourceUrl) {
        mWidth = width;
        mHeight = height;
        mResourceUrl = resourceUrl;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setSuggestedDuration(String suggestedDuration) {
        mSuggestedDuration = suggestedDuration;
    }

    public String getSuggestedDuration() {
        return mSuggestedDuration;
    }

    public String getResourceUrl() {
        return mResourceUrl;
    }

    public void setClickThroughUrl(String clickThroughUrl) {
        mClickThroughUrl = clickThroughUrl;
    }

    public String getClickThroughUrl() {
        return mClickThroughUrl;
    }

    public void setClickTrackingUrl(String clickTrackingUrl) {
        mClickTrackingUrl = clickTrackingUrl;
    }

    public String getClickTrackingUrl() {
        return mClickTrackingUrl;
    }

    public void setAdParameters(String adParameters) {
        mAdParameters = adParameters;
    }

    public String getAdParameters() {
        return mAdParameters;
    }

    public void setType(String type) {
        mType = type;
    }

    public String getType() {
        return mType;
    }
}

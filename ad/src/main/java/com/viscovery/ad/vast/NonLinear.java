package com.viscovery.ad.vast;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "NonLinear", strict = false)
public class NonLinear {
    @Attribute(name = "width")
    private int mWidth;
    @Attribute(name = "height")
    private int mHeight;
    @Attribute(name = "minSuggestedDuration")
    private String mMinSuggestedDuration;
    @Attribute(name = "scalable")
    private boolean mScalable;
    @Attribute(name = "maintainAspectRatio")
    private boolean mMaintainAspectRatio;

    @Element(name = "StaticResource")
    private String mStaticResource;
    @Element(name = "NonLinearClickThrough")
    private String mNonLinearClickThrough;
    @Element(name = "NonLinearClickTracking")
    private String mNonLinearClickTracking;
    @Element(name = "AdParameters")
    private String mAdParameters;

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getMinSuggestedDuration() {
        return mMinSuggestedDuration;
    }

    public boolean isScalable() {
        return mScalable;
    }

    public boolean isMaintainAspectRatio() {
        return mMaintainAspectRatio;
    }

    public String getStaticResource() {
        return mStaticResource;
    }

    public String getNonLinearClickThrough() {
        return mNonLinearClickThrough;
    }

    public String getNonLinearClickTracking() {
        return mNonLinearClickTracking;
    }

    public String getAdParameters() {
        return mAdParameters;
    }
}

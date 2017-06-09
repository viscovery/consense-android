package com.viscovery.ad.vast;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(name = "InLine", strict = false)
public class InLine {
    @Element(name = "AdSystem")
    private String mAdSystem;
    @Element(name = "AdTitle")
    private String mAdTitle;
    @Element(name = "Description")
    private String mDescription;
    @Element(name = "Error")
    private String mError;
    @Element(name = "Impression")
    private String mImpression;
    @Path("Creatives/Creative/NonLinearAds")
    @Element(name = "NonLinear")
    private NonLinear mNonLinear;

    public String getAdSystem() {
        return mAdSystem;
    }

    public String getAdTitle() {
        return mAdTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getError() {
        return mError;
    }

    public String getImpression() {
        return mImpression;
    }

    public NonLinear getNonLinear() {
        return mNonLinear;
    }
}

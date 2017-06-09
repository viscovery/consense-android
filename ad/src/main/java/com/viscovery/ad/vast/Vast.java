package com.viscovery.ad.vast;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "VAST", strict = false)
public class Vast {
    @Element(name = "Ad")
    private Ad mAd;

    public Ad getAd() {
        return mAd;
    }
}

package com.viscovery.ad.vmap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "AdBreak", strict = false)
public class AdBreak {
    @Attribute(name = "timeOffset")
    private String mTimeOffset;
    @Attribute(name = "breakType")
    private String mBreakType;

    @Element(name = "AdSource")
    private AdSource mAdSource;
    @ElementList(name = "Extensions")
    private List<Extension> mExtensions;

    public String getTimeOffset() {
        return mTimeOffset;
    }

    public String getBreakType() {
        return mBreakType;
    }

    public AdSource getAdSource() {
        return mAdSource;
    }

    public List<Extension> getExtensions() {
        return mExtensions;
    }
}

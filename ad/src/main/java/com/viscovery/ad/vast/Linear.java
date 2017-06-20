package com.viscovery.ad.vast;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "Linear", strict = false)
public class Linear {
    @Element(name = "Duration")
    private String mDuration;

    @ElementList(name = "MediaFiles")
    private List<MediaFile> mMediaFiles;

    @Attribute(name = "skipoffset", required = false)
    private String mSkipOffset;

    @Path("VideoClicks")
    @Element(name = "ClickThrough", required = false)
    private String mClickThrough;

    @Path("VideoClicks")
    @Element(name = "ClickTracking", required = false)
    private String mClickTracking;

    public String getDuration() {
        return mDuration;
    }

    public List<MediaFile> getMediaFiles() {
        return mMediaFiles;
    }

    public String getSkipOffset() {
        return mSkipOffset;
    }

    public String getClickThrough() {
        return mClickThrough;
    }

    public String getClickTracking() {
        return mClickTracking;
    }
}

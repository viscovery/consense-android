package com.viscovery.ad.vast;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(name = "MediaFile", strict = false)
public class MediaFile {
    public static final String TYPE_3GPP = "video/3gpp";
    public static final String TYPE_MPEG4 = "video/mp4";
    public static final String TYPE_WEBM = "video/webm";


    @Attribute(name = "delivery")
    private String mDelivery;
    @Attribute(name = "type")
    private String mType;
    @Attribute(name = "width")
    private int mWidth;
    @Attribute(name = "height")
    private int mHeight;

    @Attribute(name = "id", required = false)
    private String mId;
    @Attribute(name = "bitrate", required = false)
    private int mBitrate;
    @Attribute(name = "minBitrate", required = false)
    private int mMinBitrate;
    @Attribute(name = "maxBitrate", required = false)
    private int mMaxBitRate;
    @Attribute(name = "scalable", required = false)
    private boolean mScalable;
    @Attribute(name = "maintainAspectRatio", required = false)
    private boolean mMaintainAspectRatio;

    @Text
    private String mValue;

    public String getDelivery() {
        return mDelivery;
    }

    public String getType() {
        return mType;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getId() {
        return mId;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public int getMinBitrate() {
        return mMinBitrate;
    }

    public int getMaxBitRate() {
        return mMaxBitRate;
    }

    public boolean isScalable() {
        return mScalable;
    }

    public boolean isMaintainAspectRatio() {
        return mMaintainAspectRatio;
    }

    public String getValue() {
        return mValue;
    }
}
